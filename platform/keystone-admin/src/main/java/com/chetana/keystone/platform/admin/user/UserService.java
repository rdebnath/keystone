package com.chetana.keystone.platform.admin.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.PlatformSchema;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.TENANTS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USER_ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USERS;

/**
 * Use-case service for user management.
 */
@Singleton
public final class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final DataAccess data;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;
    private final SupabaseAdminClient supabaseAdmin;

    @Inject
    public UserService(@Platform DataAccess data, IdGenerator idGenerator, DateTimeService dateTimeService, SupabaseAdminClient supabaseAdmin) {
        this.data = data;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
        this.supabaseAdmin = supabaseAdmin;
    }

    /**
     * Lists users, optionally restricted to one tenant. {@code null} returns every user; the reserved
     * platform tenant id returns the platform users ({@code tenant_id IS NULL}); any other id returns
     * that tenant's users.
     */
    public List<UserDto> list(UUID tenantId) {
        var users = data.read().selectFrom(USERS)
                .where(tenantFilter(tenantId))
                .orderBy(USERS.USERNAME)
                .fetch();
        Map<UUID, List<String>> rolesByUser = data.read()
                .select(USER_ROLES.USER_ID, ROLES.CODE)
                .from(USER_ROLES)
                .join(ROLES).on(ROLES.ID.eq(USER_ROLES.ROLE_ID))
                .join(USERS).on(USERS.ID.eq(USER_ROLES.USER_ID))
                .where(tenantFilter(tenantId))
                .fetchGroups(USER_ROLES.USER_ID, ROLES.CODE);
        return users.map(u -> new UserDto(
                u.getId(),
                u.getSub(),
                u.getUsername(),
                u.getEmail(),
                u.getTenantId(),
                u.getMustChangePassword(),
                rolesByUser.getOrDefault(u.getId(), List.of()).stream().sorted().toList(),
                u.getCreatedAt(),
                u.getUpdatedAt()));
    }

    /** No filter returns everyone; the reserved id addresses the platform plane. */
    private static Condition tenantFilter(UUID tenantId) {
        if (tenantId == null) {
            return DSL.noCondition();
        }
        return PlatformSchema.isPlatformTenant(tenantId) ? USERS.TENANT_ID.isNull() : USERS.TENANT_ID.eq(tenantId);
    }

    public UserDto create(UserRequest request) {
        String username = Username.normalize(request.username());
        validateTemporaryPassword(request.temporaryPassword());
        UUID tenantId = tenant(request.tenantId());
        String tenantSlug = resolveTenantSlug(tenantId);
        String email = Emails.derive(username, tenantSlug, request.email());
        checkUsernameUnique(username, tenantId);

        String sub = supabaseAdmin.createOrAdoptUser(email, request.temporaryPassword());
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        List<String> roles = normalized(request.roles());
        data.transaction(tx -> {
            tx.insertInto(USERS, USERS.ID, USERS.SUB, USERS.USERNAME, USERS.EMAIL, USERS.TENANT_ID, USERS.MUST_CHANGE_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
                    .values(id, sub, username, email, tenantId, true, now, now)
                    .execute();
            replaceRoles(tx, id, tenantId, roles);
        });
        return new UserDto(id, sub, username, email, tenantId, true, roles, now, now);
    }

    /**
     * Renames a user and replaces its role set in one transaction. The email is not editable: it is the
     * Supabase Auth identity bound to {@code users.sub} (see {@link UserUpdateRequest}).
     */
    public UserDto update(UUID id, UserUpdateRequest request) {
        var existing = data.read().selectFrom(USERS).where(USERS.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("User not found: " + id);
        }
        String username = Username.normalize(request.username());
        if (!username.equals(existing.getUsername())) {
            checkUsernameUnique(username, existing.getTenantId(), id);
        }
        List<String> roles = normalized(request.roles());
        OffsetDateTime now = now();
        data.transaction(tx -> {
            tx.update(USERS)
                    .set(USERS.USERNAME, username)
                    .set(USERS.UPDATED_AT, now)
                    .where(USERS.ID.eq(id))
                    .execute();
            replaceRoles(tx, id, existing.getTenantId(), roles);
        });
        return new UserDto(
                id,
                existing.getSub(),
                username,
                existing.getEmail(),
                existing.getTenantId(),
                existing.getMustChangePassword(),
                roles,
                existing.getCreatedAt(),
                now);
    }

    public void assignRoles(UUID id, AssignRolesRequest request) {
        var user = data.read().select(USERS.TENANT_ID).from(USERS).where(USERS.ID.eq(id)).fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + id);
        }
        data.transaction(tx -> replaceRoles(tx, id, user.value1(), request.roles()));
    }

    /**
     * Sets a user's temporary password in Supabase Auth and re-arms the forced first-login change.
     *
     * <p>The actor is the authenticated caller: resetting <em>your own</em> password is rejected, because
     * this route cannot prove the current password — a holder of the user-write grant could otherwise use
     * it to take their own account over without knowing the old password (the verified flow is
     * {@code POST /api/v1/me/password}). The temporary password is written to Auth first so a Supabase
     * failure cannot leave the row flagged for a password that never changed; it is never persisted here
     * and never returned.
     */
    public void resetPassword(UUID id, ResetPasswordRequest request, String actorSub) {
        var target = data.read().selectFrom(USERS).where(USERS.ID.eq(id)).fetchOne();
        if (target == null) {
            throw new NotFoundException("User not found: " + id);
        }
        validateTemporaryPassword(request.temporaryPassword());
        if (target.getSub().equals(actorSub)) {
            throw new ValidationException("Use the change-password flow for your own account");
        }
        supabaseAdmin.updatePassword(target.getSub(), request.temporaryPassword());
        data.write().update(USERS)
                .set(USERS.MUST_CHANGE_PASSWORD, true)
                .set(USERS.UPDATED_AT, now())
                .where(USERS.ID.eq(id))
                .execute();
        // A privileged, otherwise silent action at the account-takeover boundary: worth a trail. Only ids
        // (random uuids) are logged — never the email, the username or the password.
        log.info("Password reset for user {} by actor {}", id, actorSub);
    }

    public void delete(UUID id) {
        data.transaction(tx -> {
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(id)).execute();
            tx.deleteFrom(USERS).where(USERS.ID.eq(id)).execute();
        });
    }

    private void replaceRoles(DSLContext tx, UUID userId, UUID tenantId, List<String> roleCodes) {
        tx.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(userId)).execute();
        for (String code : normalized(roleCodes)) {
            var role = tx.select(ROLES.ID, ROLES.SCOPE).from(ROLES).where(ROLES.CODE.eq(code)).fetchOne();
            if (role == null) {
                throw new ValidationException("Unknown role: " + code);
            }
            String scope = role.get(ROLES.SCOPE);
            if (tenantId == null && !"PLATFORM".equals(scope)) {
                throw new ValidationException("Only PLATFORM roles can be assigned at platform level: " + code);
            }
            if (tenantId != null && "PLATFORM".equals(scope)) {
                throw new ValidationException("PLATFORM roles cannot be assigned to tenant users: " + code);
            }
            tx.insertInto(USER_ROLES, USER_ROLES.USER_ID, USER_ROLES.ROLE_ID, USER_ROLES.TENANT_ID)
                    .values(userId, role.get(ROLES.ID), tenantId)
                    .onConflictDoNothing()
                    .execute();
        }
    }

    /**
     * Normalizes the requested tenant: the reserved platform tenant id is accepted as an alias for the
     * platform plane ({@code null}), so a caller can pass back the id it received from the tenant list.
     */
    private static UUID tenant(UUID tenantId) {
        return PlatformSchema.isPlatformTenant(tenantId) ? null : tenantId;
    }

    private String resolveTenantSlug(UUID tenantId) {
        if (tenantId == null) {
            return PlatformSchema.RESERVED_SLUG;
        }
        String slug = data.read().select(TENANTS.SLUG).from(TENANTS).where(TENANTS.ID.eq(tenantId)).fetchOne(TENANTS.SLUG);
        if (slug == null) {
            throw new NotFoundException("Tenant not found: " + tenantId);
        }
        return slug;
    }

    private void checkUsernameUnique(String username, UUID tenantId) {
        checkUsernameUnique(username, tenantId, null);
    }

    private void checkUsernameUnique(String username, UUID tenantId, UUID excludeUserId) {
        Condition condition = USERS.USERNAME.eq(username).and(tenantCondition(tenantId));
        if (excludeUserId != null) {
            condition = condition.and(USERS.ID.ne(excludeUserId));
        }
        if (data.read().fetchExists(USERS, condition)) {
            throw new ConflictException("User already exists: " + username);
        }
    }

    /** Usernames are unique within a plane: platform users all share {@code tenant_id IS NULL}. */
    private static Condition tenantCondition(UUID tenantId) {
        return tenantId == null ? USERS.TENANT_ID.isNull() : USERS.TENANT_ID.eq(tenantId);
    }

    /** A temporary password is required: it is the credential the user's first login uses. */
    private static void validateTemporaryPassword(String temporaryPassword) {
        if (temporaryPassword == null || temporaryPassword.isBlank()) {
            throw new ValidationException("temporaryPassword must not be blank");
        }
    }

    private static List<String> normalized(List<String> codes) {
        return codes == null ? List.of() : codes.stream().distinct().toList();
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

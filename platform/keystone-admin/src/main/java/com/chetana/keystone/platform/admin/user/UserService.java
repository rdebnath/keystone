package com.chetana.keystone.platform.admin.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.query.Page;
import com.chetana.keystone.common.query.PageRequest;
import com.chetana.keystone.common.query.SortOrder;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.data.Search;
import com.chetana.keystone.platform.admin.PlatformSchema;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.identity.CallerScope;
import com.chetana.keystone.platform.admin.jooq.platform.tables.records.UsersRecord;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLE_PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.TENANTS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USER_ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USERS;

/**
 * Use-case service for user management across the two planes.
 *
 * <p>On the tenant plane the tenant of every operation is the <em>caller's own</em>, so a tenant admin
 * cannot create or reach a user outside its tenant, and the target of an update/delete/reset is checked
 * against it first (a foreign user is a plain {@code 404}, never a hint that it exists).
 *
 * <p>Role assignment resolves a role within the user's plane — the global catalog plus, for a tenant
 * user, the roles that tenant owns — and honours "grant only what you hold": a tenant caller may only
 * assign a role whose permissions it already has.
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
     * One page of users. The platform plane may narrow by {@code filter} — {@code null} is every user, the
     * reserved platform id the platform users ({@code tenant_id IS NULL}), any other id that tenant's
     * users — and {@code search} matches username, email or phone number. A tenant caller always sees
     * exactly its own tenant's users.
     *
     * <p>The count and the window run against the same condition (visibility AND search), which is what
     * makes a search cover every user the caller may see rather than the page they are looking at. The
     * roles of the page's users come from one grouped query over that page's ids — never one query per row.
     */
    public Page<UserDto> list(CallerScope caller, UUID filter, String search, PageRequest page) {
        UUID tenantId = visibleTenant(caller, filter);
        Condition where = tenantFilter(tenantId)
                .and(Search.containsIgnoreCase(search, USERS.USERNAME, USERS.EMAIL, USERS.PHONE_NUMBER));
        long total = data.read().fetchCount(USERS, where);
        var users = data.read().selectFrom(USERS)
                .where(where)
                .orderBy(orderOf(page))
                .limit(page.size())
                .offset(page.offset())
                .fetch();
        Map<UUID, List<String>> rolesByUser = rolesOf(users.getValues(USERS.ID));
        return Page.of(users.map(user -> toDto(user, rolesByUser)), page, total);
    }

    /** The roles of {@code userIds}, grouped: one query for the page, not one per row. */
    private Map<UUID, List<String>> rolesOf(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return data.read()
                .select(USER_ROLES.USER_ID, ROLES.CODE)
                .from(USER_ROLES)
                .join(ROLES).on(ROLES.ID.eq(USER_ROLES.ROLE_ID))
                .where(USER_ROLES.USER_ID.in(userIds))
                .fetchGroups(USER_ROLES.USER_ID, ROLES.CODE);
    }

    private static UserDto toDto(UsersRecord user, Map<UUID, List<String>> rolesByUser) {
        return new UserDto(
                user.getId(),
                user.getSub(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getTenantId(),
                user.getMustChangePassword(),
                rolesByUser.getOrDefault(user.getId(), List.of()).stream().sorted().toList(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    /**
     * The requested sort order, always with {@code id} as the tiebreaker so a page window is stable: a
     * non-total order lets PostgreSQL return equal rows in either order per query, which shows up as a row
     * missing from every page — or on two. A key the platform does not publish is a `422` naming the ones
     * it does, so a caller-supplied string never reaches `ORDER BY`.
     */
    private static List<OrderField<?>> orderOf(PageRequest page) {
        if (!page.sorted()) {
            return List.of(USERS.USERNAME.asc(), USERS.ID.asc());
        }
        Field<?> key = switch (page.sort()) {
            case "username" -> USERS.USERNAME;
            case "email" -> USERS.EMAIL;
            case "createdAt" -> USERS.CREATED_AT;
            case "updatedAt" -> USERS.UPDATED_AT;
            default -> throw new ValidationException("Unknown sort key: " + page.sort()
                    + " (allowed: username, email, createdAt, updatedAt)");
        };
        return List.of(direction(key, page.order()), USERS.ID.asc());
    }

    private static OrderField<?> direction(Field<?> field, SortOrder order) {
        return order == SortOrder.DESC ? field.desc() : field.asc();
    }

    public UserDto create(CallerScope caller, UserRequest request) {
        String username = Username.normalize(request.username());
        validateTemporaryPassword(request.temporaryPassword());
        UUID tenantId = tenantOf(caller, request.tenantId());
        String tenantSlug = resolveTenantSlug(tenantId);
        String email = Emails.derive(username, tenantSlug, request.email());
        String phoneNumber = PhoneNumber.normalizeOptional(request.phoneNumber());
        checkUsernameUnique(username, tenantId);
        List<String> roles = normalized(request.roles());

        String sub = supabaseAdmin.createOrAdoptUser(email, request.temporaryPassword());
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        data.transaction(tx -> {
            tx.insertInto(USERS, USERS.ID, USERS.SUB, USERS.USERNAME, USERS.EMAIL, USERS.PHONE_NUMBER,
                            USERS.TENANT_ID, USERS.MUST_CHANGE_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
                    .values(id, sub, username, email, phoneNumber, tenantId, true, now, now)
                    .execute();
            replaceRoles(tx, id, tenantId, roles, caller);
        });
        return new UserDto(id, sub, username, email, phoneNumber, tenantId, true, roles, now, now);
    }

    /**
     * Renames a user, sets its phone number and replaces its role set in one transaction. The email is
     * not editable: it is the Supabase Auth identity bound to {@code users.sub} (see
     * {@link UserUpdateRequest}).
     */
    public UserDto update(CallerScope caller, UUID id, UserUpdateRequest request) {
        var existing = requireVisibleUser(caller, id);
        String username = Username.normalize(request.username());
        if (!username.equals(existing.getUsername())) {
            checkUsernameUnique(username, existing.getTenantId(), id);
        }
        String phoneNumber = PhoneNumber.normalizeOptional(request.phoneNumber());
        List<String> roles = normalized(request.roles());
        OffsetDateTime now = now();
        data.transaction(tx -> {
            tx.update(USERS)
                    .set(USERS.USERNAME, username)
                    .set(USERS.PHONE_NUMBER, phoneNumber)
                    .set(USERS.UPDATED_AT, now)
                    .where(USERS.ID.eq(id))
                    .execute();
            replaceRoles(tx, id, existing.getTenantId(), roles, caller);
        });
        return new UserDto(
                id,
                existing.getSub(),
                username,
                existing.getEmail(),
                phoneNumber,
                existing.getTenantId(),
                existing.getMustChangePassword(),
                roles,
                existing.getCreatedAt(),
                now);
    }

    public void assignRoles(CallerScope caller, UUID id, AssignRolesRequest request) {
        var user = requireVisibleUser(caller, id);
        data.transaction(tx -> replaceRoles(tx, id, user.getTenantId(), request.roles(), caller));
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
    public void resetPassword(CallerScope caller, UUID id, ResetPasswordRequest request, String actorSub) {
        var target = requireVisibleUser(caller, id);
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

    public void delete(CallerScope caller, UUID id) {
        requireVisibleUser(caller, id);
        data.transaction(tx -> {
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(id)).execute();
            tx.deleteFrom(USERS).where(USERS.ID.eq(id)).execute();
        });
    }

    /**
     * Replaces a user's roles. A role code is resolved <em>within the user's plane</em> — the global
     * catalog plus, for a tenant user, the roles that tenant owns — so it can never silently resolve to
     * another tenant's role. The global catalog row wins when a code exists in both, which only the
     * platform plane can create (a tenant cannot shadow the catalog).
     */
    private void replaceRoles(DSLContext tx, UUID userId, UUID tenantId, List<String> roleCodes, CallerScope caller) {
        tx.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(userId)).execute();
        for (String code : normalized(roleCodes)) {
            var role = tx.select(ROLES.ID, ROLES.SCOPE)
                    .from(ROLES)
                    .where(ROLES.CODE.eq(code))
                    .and(roleVisibility(tenantId))
                    .orderBy(ROLES.TENANT_ID.asc().nullsFirst())
                    .fetchAny();
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
            requireAssignable(tx, caller, role.get(ROLES.ID), code);
            tx.insertInto(USER_ROLES, USER_ROLES.USER_ID, USER_ROLES.ROLE_ID, USER_ROLES.TENANT_ID)
                    .values(userId, role.get(ROLES.ID), tenantId)
                    .onConflictDoNothing()
                    .execute();
        }
    }

    /**
     * "Grant only what you hold", for roles: a tenant caller may only assign a role whose permissions are
     * a subset of its own. Without it a holder of {@code tenant:user:read-write} could hand on a more
     * powerful role — including the tenant's own {@code admin}.
     */
    private static void requireAssignable(DSLContext tx, CallerScope caller, UUID roleId, String code) {
        if (caller.platform()) {
            return;
        }
        Set<String> roleGrants = tx.select(PERMISSIONS.CODE)
                .from(ROLE_PERMISSIONS)
                .join(PERMISSIONS).on(PERMISSIONS.ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                .where(ROLE_PERMISSIONS.ROLE_ID.eq(roleId))
                .fetchSet(PERMISSIONS.CODE);
        List<String> unheld = roleGrants.stream()
                .filter(grant -> !caller.holds(grant))
                .sorted()
                .toList();
        if (!unheld.isEmpty()) {
            throw new ValidationException("Cannot assign the role " + code
                    + " with permissions you do not hold: " + String.join(", ", unheld));
        }
    }

    /** The roles assignable to a user of {@code tenantId}: the global catalog plus that tenant's own. */
    private static Condition roleVisibility(UUID tenantId) {
        if (tenantId == null) {
            return ROLES.TENANT_ID.isNull();
        }
        return ROLES.TENANT_ID.isNull().or(ROLES.TENANT_ID.eq(tenantId));
    }

    /** The target user, or {@code 404} when a tenant caller may not see it — never a hint that it exists. */
    private UsersRecord requireVisibleUser(CallerScope caller, UUID id) {
        var user = data.read().selectFrom(USERS).where(USERS.ID.eq(id)).fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + id);
        }
        if (!caller.platform()
                && (user.getTenantId() == null || !user.getTenantId().equals(caller.tenantId()))) {
            throw new NotFoundException("User not found: " + id);
        }
        return user;
    }

    /** The tenant whose users the caller lists: its own on the tenant plane, the filter on the platform plane. */
    private static UUID visibleTenant(CallerScope caller, UUID filter) {
        if (caller.platform()) {
            return filter;
        }
        if (filter != null) {
            throw new ValidationException("A tenant cannot choose the tenant to list");
        }
        return caller.tenantId();
    }

    /** The tenant a write targets: the caller's own on the tenant plane, the request's on the platform plane. */
    private static UUID tenantOf(CallerScope caller, UUID requested) {
        if (caller.platform()) {
            return tenant(requested);
        }
        if (requested != null) {
            throw new ValidationException("A tenant does not choose the tenant: it is the caller's tenant");
        }
        return caller.tenantId();
    }

    /** No filter returns everyone; the reserved id addresses the platform plane. */
    private static Condition tenantFilter(UUID tenantId) {
        if (tenantId == null) {
            return DSL.noCondition();
        }
        return PlatformSchema.isPlatformTenant(tenantId) ? USERS.TENANT_ID.isNull() : USERS.TENANT_ID.eq(tenantId);
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

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

    public List<UserDto> list() {
        var users = data.read().selectFrom(USERS).orderBy(USERS.USERNAME).fetch();
        Map<UUID, List<String>> rolesByUser = data.read()
                .select(USER_ROLES.USER_ID, ROLES.CODE)
                .from(USER_ROLES)
                .join(ROLES).on(ROLES.ID.eq(USER_ROLES.ROLE_ID))
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

    public UserDto create(UserRequest request) {
        String username = Username.normalize(request.username());
        validatePassword(request);
        String tenantSlug = resolveTenantSlug(request.tenantId());
        String email = Emails.derive(username, tenantSlug, request.email());
        checkUsernameUnique(username, request.tenantId());

        String sub = supabaseAdmin.createOrAdoptUser(email, request.temporaryPassword());
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        data.transaction(tx -> {
            tx.insertInto(USERS, USERS.ID, USERS.SUB, USERS.USERNAME, USERS.EMAIL, USERS.TENANT_ID, USERS.MUST_CHANGE_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
                    .values(id, sub, username, email, request.tenantId(), true, now, now)
                    .execute();
            assignRoles(tx, id, request.tenantId(), request.roles());
        });
        return new UserDto(id, sub, username, email, request.tenantId(), true, normalized(request.roles()), now, now);
    }
    public void assignRoles(UUID id, AssignRolesRequest request) {
        var user = data.read().select(USERS.TENANT_ID).from(USERS).where(USERS.ID.eq(id)).fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + id);
        }
        data.transaction(tx -> assignRoles(tx, id, user.value1(), request.roles()));
    }

    public void delete(UUID id) {
        data.transaction(tx -> {
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(id)).execute();
            tx.deleteFrom(USERS).where(USERS.ID.eq(id)).execute();
        });
    }

    private void assignRoles(DSLContext tx, UUID userId, UUID tenantId, List<String> roleCodes) {
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
        Condition tenantMatch = tenantId == null ? USERS.TENANT_ID.isNull() : USERS.TENANT_ID.eq(tenantId);
        if (data.read().fetchExists(USERS, USERS.USERNAME.eq(username).and(tenantMatch))) {
            throw new ConflictException("User already exists: " + username);
        }
    }

    private static void validatePassword(UserRequest request) {
        if (request.temporaryPassword() == null || request.temporaryPassword().isBlank()) {
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

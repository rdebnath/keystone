package com.chetana.keystone.platform.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.platform.supabase.SupabaseAdminClient;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.chetana.keystone.platform.jooq.Tables.ROLES;
import static com.chetana.keystone.platform.jooq.Tables.USER_ROLES;
import static com.chetana.keystone.platform.jooq.Tables.USERS;

/**
 * Use-case service for platform user management.
 */
@Singleton
public final class UserService {

    private final DSLContext dsl;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;
    private final SupabaseAdminClient supabaseAdmin;

    @Inject
    public UserService(DSLContext dsl, IdGenerator idGenerator, DateTimeService dateTimeService, SupabaseAdminClient supabaseAdmin) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
        this.supabaseAdmin = supabaseAdmin;
    }

    public List<UserDto> list() {
        var users = dsl.selectFrom(USERS).orderBy(USERS.EMAIL).fetch();
        Map<UUID, List<String>> rolesByUser = dsl
                .select(USER_ROLES.USER_ID, ROLES.CODE)
                .from(USER_ROLES)
                .join(ROLES).on(ROLES.ID.eq(USER_ROLES.ROLE_ID))
                .fetchGroups(USER_ROLES.USER_ID, ROLES.CODE);
        return users.map(u -> new UserDto(
                u.getId(),
                u.getSub(),
                u.getEmail(),
                u.getTenantId(),
                u.getMustChangePassword(),
                rolesByUser.getOrDefault(u.getId(), List.of()).stream().sorted().toList(),
                u.getCreatedAt(),
                u.getUpdatedAt()));
    }

    public UserDto create(UserRequest request) {
        validate(request);
        if (dsl.fetchExists(USERS, USERS.EMAIL.eq(request.email()))) {
            throw new ConflictException("User already exists: " + request.email());
        }
        String sub = supabaseAdmin.ensureUser(request.email(), request.temporaryPassword());
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        dsl.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);
            tx.insertInto(USERS, USERS.ID, USERS.SUB, USERS.EMAIL, USERS.TENANT_ID, USERS.MUST_CHANGE_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
                    .values(id, sub, request.email(), null, true, now, now)
                    .execute();
            assignRoles(tx, id, request.roles());
        });
        return new UserDto(id, sub, request.email(), null, true, normalized(request.roles()), now, now);
    }

    public void assignRoles(UUID id, AssignRolesRequest request) {
        if (!dsl.fetchExists(USERS, USERS.ID.eq(id))) {
            throw new NotFoundException("User not found: " + id);
        }
        dsl.transaction(configuration -> assignRoles(DSL.using(configuration), id, request.roles()));
    }

    public void delete(UUID id) {
        dsl.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(id)).execute();
            tx.deleteFrom(USERS).where(USERS.ID.eq(id)).execute();
        });
    }

    private void assignRoles(DSLContext tx, UUID userId, List<String> roleCodes) {
        tx.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(userId)).execute();
        for (String code : normalized(roleCodes)) {
            var role = tx.select(ROLES.ID, ROLES.SCOPE).from(ROLES).where(ROLES.CODE.eq(code)).fetchOne();
            if (role == null) {
                throw new ValidationException("Unknown role: " + code);
            }
            if (!"PLATFORM".equals(role.get(ROLES.SCOPE))) {
                throw new ValidationException("Only PLATFORM roles can be assigned at platform level: " + code);
            }
            tx.insertInto(USER_ROLES, USER_ROLES.USER_ID, USER_ROLES.ROLE_ID, USER_ROLES.TENANT_ID)
                    .values(userId, role.get(ROLES.ID), null)
                    .onConflictDoNothing()
                    .execute();
        }
    }

    private static List<String> normalized(List<String> codes) {
        return codes == null ? List.of() : codes.stream().distinct().toList();
    }

    private static void validate(UserRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ValidationException("email must not be blank");
        }
        if (request.temporaryPassword() == null || request.temporaryPassword().isBlank()) {
            throw new ValidationException("temporaryPassword must not be blank");
        }
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

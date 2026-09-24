package com.chetana.keystone.platform.admin.role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.identity.Scope;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.Tables.ROLE_PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.Tables.ROLES;
import static com.chetana.keystone.platform.admin.jooq.Tables.USER_ROLES;

/**
 * Use-case service for role management (platform- and tenant-scoped catalog).
 */
@Singleton
public final class RoleService {

    private final DSLContext dsl;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public RoleService(@Platform DSLContext dsl, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    public List<RoleDto> list() {
        var roles = dsl.selectFrom(ROLES).orderBy(ROLES.CODE).fetch();
        Map<UUID, List<String>> permissionsByRole = dsl
                .select(ROLE_PERMISSIONS.ROLE_ID, PERMISSIONS.CODE)
                .from(ROLE_PERMISSIONS)
                .join(PERMISSIONS).on(PERMISSIONS.ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                .fetchGroups(ROLE_PERMISSIONS.ROLE_ID, PERMISSIONS.CODE);
        return roles.map(r -> new RoleDto(
                r.getId(),
                r.getCode(),
                Scope.from(r.getScope()),
                permissionsByRole.getOrDefault(r.getId(), List.of()).stream().sorted().toList(),
                r.getCreatedAt(),
                r.getUpdatedAt()));
    }

    public RoleDto create(RoleRequest request) {
        validate(request);
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        return dsl.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            int inserted = tx.insertInto(ROLES, ROLES.ID, ROLES.CODE, ROLES.SCOPE, ROLES.CREATED_AT, ROLES.UPDATED_AT)
                    .values(id, request.code(), request.scope().name(), now, now)
                    .onConflictDoNothing()
                    .execute();
            if (inserted == 0) {
                throw new ConflictException("Role already exists: " + request.code());
            }
            grantPermissions(tx, id, request.scope(), request.permissions());
            return new RoleDto(id, request.code(), request.scope(), normalized(request.permissions()), now, now);
        });
    }
    public RoleDto update(UUID id, RoleRequest request) {
        validate(request);
        var existing = dsl.selectFrom(ROLES).where(ROLES.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("Role not found: " + id);
        }
        OffsetDateTime now = now();
        dsl.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);
            tx.update(ROLES)
                    .set(ROLES.CODE, request.code())
                    .set(ROLES.SCOPE, request.scope().name())
                    .set(ROLES.UPDATED_AT, now)
                    .where(ROLES.ID.eq(id))
                    .execute();
            tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.ROLE_ID.eq(id)).execute();
            grantPermissions(tx, id, request.scope(), request.permissions());
        });
        return new RoleDto(id, request.code(), request.scope(), normalized(request.permissions()), existing.getCreatedAt(), now);
    }

    public void delete(UUID id) {
        dsl.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);
            tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.ROLE_ID.eq(id)).execute();
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.ROLE_ID.eq(id)).execute();
            tx.deleteFrom(ROLES).where(ROLES.ID.eq(id)).execute();
        });
    }

    private void grantPermissions(DSLContext ctx, UUID roleId, Scope scope, List<String> codes) {
        for (String code : normalized(codes)) {
            var permission = ctx.select(PERMISSIONS.ID, PERMISSIONS.SCOPE)
                    .from(PERMISSIONS)
                    .where(PERMISSIONS.CODE.eq(code))
                    .fetchOne();
            if (permission == null) {
                throw new ValidationException("Unknown permission: " + code);
            }
            if (!scope.name().equals(permission.get(PERMISSIONS.SCOPE))) {
                throw new ValidationException("Permission scope mismatch for role " + scope + ": " + code);
            }
            ctx.insertInto(ROLE_PERMISSIONS, ROLE_PERMISSIONS.ROLE_ID, ROLE_PERMISSIONS.PERMISSION_ID)
                    .values(roleId, permission.get(PERMISSIONS.ID))
                    .onConflictDoNothing()
                    .execute();
        }
    }

    private static List<String> normalized(List<String> codes) {
        return codes == null ? List.of() : codes.stream().distinct().toList();
    }

    private static void validate(RoleRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new ValidationException("code must not be blank");
        }
        if (request.scope() == null) {
            throw new ValidationException("scope must not be null");
        }
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

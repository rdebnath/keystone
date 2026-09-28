package com.chetana.keystone.platform.admin.role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.platform.admin.PermissionCatalog;
import com.chetana.keystone.platform.admin.identity.Scope;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLE_PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLES;

/**
 * Seeds the platform's administrative roles, idempotently and <em>completely</em>: both the role row and
 * its grants are ensured, so a role that already exists is brought up to date instead of being left
 * half-provisioned. Nothing is ever re-created, renamed or re-pointed — {@code ensure} adds only what is
 * missing — so it is safe to run at startup, on tenant creation, and after a manual repair.
 *
 * <p>The two roles mirror each other: the global {@code platform-admin} (the wildcard, platform plane)
 * and one tenant-owned {@code admin} per tenant (the read/write {@code TENANT}-scope codes, so a tenant
 * administers its own users, roles and permissions).
 */
@Singleton
public final class RoleSeeder {

    private final IdGenerator idGenerator;

    @Inject
    public RoleSeeder(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /** Ensures the global {@code platform-admin} role exists and holds the wildcard. */
    public UUID ensurePlatformAdminRole(DSLContext tx, OffsetDateTime now) {
        UUID roleId = ensureRole(tx, PermissionCatalog.PLATFORM_ADMIN_ROLE, Scope.PLATFORM.name(), null, now);
        grant(tx, roleId, PermissionCatalog.WILDCARD);
        return roleId;
    }

    /**
     * Ensures {@code tenantId}'s {@code admin} role exists and holds the read/write {@code TENANT}-scope
     * grants. The row is tenant-owned, so {@code tenant_id} scopes it and no other tenant ever sees it.
     */
    public UUID ensureTenantAdminRole(DSLContext tx, UUID tenantId, OffsetDateTime now) {
        UUID roleId = ensureRole(tx, PermissionCatalog.TENANT_ADMIN_ROLE, Scope.TENANT.name(), tenantId, now);
        for (String code : PermissionCatalog.tenantAdminGrants()) {
            grant(tx, roleId, code);
        }
        return roleId;
    }

    /**
     * The role's id, inserting it only when {@code (code, owner)} is absent. The lookup is owner-scoped
     * on purpose: {@code code} is unique per owner, so a bare {@code code} match could pick up a
     * different tenant's role (or a tenant's {@code admin} when the global one was meant).
     */
    private UUID ensureRole(DSLContext tx, String code, String scope, UUID tenantId, OffsetDateTime now) {
        tx.insertInto(ROLES, ROLES.ID, ROLES.CODE, ROLES.SCOPE, ROLES.TENANT_ID, ROLES.CREATED_AT, ROLES.UPDATED_AT)
                .values(idGenerator.nextId(), code, scope, tenantId, now, now)
                .onConflictDoNothing()
                .execute();
        return tx.select(ROLES.ID)
                .from(ROLES)
                .where(ROLES.CODE.eq(code))
                .and(tenantId == null ? ROLES.TENANT_ID.isNull() : ROLES.TENANT_ID.eq(tenantId))
                .fetchOne(ROLES.ID);
    }

    /** Grants a code from the <b>global</b> catalog: a seeded role never holds a tenant-owned permission. */
    private void grant(DSLContext tx, UUID roleId, String code) {
        UUID permissionId = tx.select(PERMISSIONS.ID)
                .from(PERMISSIONS)
                .where(PERMISSIONS.CODE.eq(code))
                .and(PERMISSIONS.TENANT_ID.isNull())
                .fetchOne(PERMISSIONS.ID);
        if (permissionId == null) {
            throw new IllegalStateException("Missing permission in the global catalog: " + code);
        }
        tx.insertInto(ROLE_PERMISSIONS, ROLE_PERMISSIONS.ROLE_ID, ROLE_PERMISSIONS.PERMISSION_ID)
                .values(roleId, permissionId)
                .onConflictDoNothing()
                .execute();
    }
}

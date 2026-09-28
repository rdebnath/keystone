package com.chetana.keystone.platform.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.config.AdminConfig;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.role.RoleSeeder;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.TENANTS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USER_ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USERS;

/**
 * Idempotent first-user bootstrap, run once at startup: seeds the permission catalog and the
 * {@code platform-admin} role (granted the wildcard {@code *} permission), provisions the platform
 * admin identity in Supabase Auth (service-role key), and assigns that role to the admin user.
 *
 * <p>Optional: when {@code bootstrap.enabled} is false (or {@code BOOTSTRAP_ON_START=false} in the
 * environment) the call is a no-op and only logs, so a deployment can keep the platform's schema
 * migrations automatic while seeding the catalog/roles out of band.
 */
@Singleton
public final class BootstrapRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final SupabaseAdminClient supabaseAdmin;
    private final AdminConfig.Bootstrap bootstrap;
    private final DataAccess data;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;
    private final RoleSeeder roleSeeder;

    @Inject
    public BootstrapRunner(
            SupabaseAdminClient supabaseAdmin,
            AdminConfig.Bootstrap bootstrap,
            @Platform DataAccess data,
            IdGenerator idGenerator,
            DateTimeService dateTimeService,
            RoleSeeder roleSeeder) {
        this.supabaseAdmin = supabaseAdmin;
        this.bootstrap = bootstrap;
        this.data = data;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
        this.roleSeeder = roleSeeder;
    }

    public void bootstrap() {
        if (!bootstrap.enabled()) {
            log.info("Platform bootstrap is disabled (bootstrap.enabled=false / BOOTSTRAP_ON_START=false);"
                    + " skipping the permission catalog, platform-admin role and admin user seed.");
            return;
        }
        String sub = resolveAdminSub();
        UUID userId = data.transactionResult(tx -> {
            OffsetDateTime now = now();
            seedPermissions(tx, now);
            UUID roleId = roleSeeder.ensurePlatformAdminRole(tx, now);
            UUID id = upsertAdminUser(tx, sub, now);
            grantRole(tx, id, roleId);
            ensureTenantAdminRoles(tx, now);
            return id;
        });
        log.info("Bootstrapped platform admin user {} (sub {})", userId, sub);
    }

    /**
     * Returns the admin's Auth {@code sub}, preferring the value already persisted in
     * {@code users.sub} so a restart does not re-list (or re-create) the Supabase user. Falls back
     * to provisioning — and recovering from a partially-completed bootstrap — only when no
     * application row exists yet.
     */
    private String resolveAdminSub() {
        String persisted = data.read()
                .select(USERS.SUB).from(USERS)
                .where(USERS.USERNAME.eq(bootstrap.adminUsername()))
                .and(USERS.TENANT_ID.isNull())
                .fetchOne(USERS.SUB);
        if (persisted != null) {
            return persisted;
        }
        return supabaseAdmin.createOrAdoptUser(bootstrap.adminEmail(), bootstrap.adminPassword());
    }

    private void seedPermissions(DSLContext tx, OffsetDateTime now) {
        for (PermissionCatalog.Permission seed : PermissionCatalog.PERMISSIONS) {
            tx.insertInto(PERMISSIONS, PERMISSIONS.ID, PERMISSIONS.CODE, PERMISSIONS.SCOPE, PERMISSIONS.CREATED_AT, PERMISSIONS.UPDATED_AT)
                    .values(idGenerator.nextId(), seed.code(), seed.scope(), now, now)
                    .onConflictDoNothing()
                    .execute();
        }
    }

    /**
     * Re-assures every existing tenant of its own admin role. A tenant's role belongs to its lifecycle, but
     * one created before the role existed — or while the bootstrap was switched off — would otherwise never
     * get one; {@code ensure} adds only what is missing.
     */
    private void ensureTenantAdminRoles(DSLContext tx, OffsetDateTime now) {
        List<UUID> tenantIds = tx.select(TENANTS.ID).from(TENANTS).fetch(TENANTS.ID);
        for (UUID tenantId : tenantIds) {
            roleSeeder.ensureTenantAdminRole(tx, tenantId, now);
        }
    }

    private UUID upsertAdminUser(DSLContext tx, String sub, OffsetDateTime now) {
        UUID existing = tx.select(USERS.ID).from(USERS).where(USERS.SUB.eq(sub)).fetchOne(USERS.ID);
        if (existing != null) {
            return existing;
        }
        UUID id = idGenerator.nextId();
        tx.insertInto(USERS, USERS.ID, USERS.SUB, USERS.USERNAME, USERS.EMAIL, USERS.TENANT_ID, USERS.MUST_CHANGE_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
                .values(id, sub, bootstrap.adminUsername(), bootstrap.adminEmail(), null, true, now, now)
                .execute();
        return id;
    }

    private void grantRole(DSLContext tx, UUID userId, UUID roleId) {
        tx.insertInto(USER_ROLES, USER_ROLES.USER_ID, USER_ROLES.ROLE_ID, USER_ROLES.TENANT_ID)
                .values(userId, roleId, null)
                .onConflictDoNothing()
                .execute();
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

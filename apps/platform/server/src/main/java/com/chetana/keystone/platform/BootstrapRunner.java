package com.chetana.keystone.platform;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.platform.config.AppConfig;
import com.chetana.keystone.platform.supabase.SupabaseAdminClient;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.chetana.keystone.platform.PermissionCatalog.PLATFORM_ADMIN_ROLE;
import static com.chetana.keystone.platform.PermissionCatalog.WILDCARD;
import static com.chetana.keystone.platform.jooq.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.jooq.Tables.ROLE_PERMISSIONS;
import static com.chetana.keystone.platform.jooq.Tables.ROLES;
import static com.chetana.keystone.platform.jooq.Tables.USER_ROLES;
import static com.chetana.keystone.platform.jooq.Tables.USERS;

/**
 * Idempotent first-user bootstrap, run once at startup: seeds the permission catalog and the
 * {@code platform-admin} role (granted the wildcard {@code *} permission), provisions the platform
 * admin identity in Supabase Auth (service-role key), and assigns that role to the admin user.
 */
@Singleton
public final class BootstrapRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final SupabaseAdminClient supabaseAdmin;
    private final AppConfig.Bootstrap bootstrap;
    private final DSLContext dsl;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public BootstrapRunner(
            SupabaseAdminClient supabaseAdmin,
            AppConfig.Bootstrap bootstrap,
            DSLContext dsl,
            IdGenerator idGenerator,
            DateTimeService dateTimeService) {
        this.supabaseAdmin = supabaseAdmin;
        this.bootstrap = bootstrap;
        this.dsl = dsl;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    public void bootstrap() {
        String sub = supabaseAdmin.ensureUser(bootstrap.adminEmail(), bootstrap.adminPassword());
        UUID userId = dsl.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            OffsetDateTime now = now();
            seedPermissions(tx, now);
            UUID roleId = seedPlatformAdminRole(tx, now);
            UUID id = upsertAdminUser(tx, sub, now);
            grantRole(tx, id, roleId);
            return id;
        });
        log.info("Bootstrapped platform admin user {} (sub {})", userId, sub);
    }

    private void seedPermissions(DSLContext tx, OffsetDateTime now) {
        for (PermissionCatalog.Permission seed : PermissionCatalog.PERMISSIONS) {
            tx.insertInto(PERMISSIONS, PERMISSIONS.ID, PERMISSIONS.CODE, PERMISSIONS.SCOPE, PERMISSIONS.CREATED_AT, PERMISSIONS.UPDATED_AT)
                    .values(idGenerator.nextId(), seed.code(), seed.scope(), now, now)
                    .onConflictDoNothing()
                    .execute();
        }
    }

    private UUID seedPlatformAdminRole(DSLContext tx, OffsetDateTime now) {
        UUID existing = tx.select(ROLES.ID).from(ROLES).where(ROLES.CODE.eq(PLATFORM_ADMIN_ROLE)).fetchOne(ROLES.ID);
        if (existing != null) {
            return existing;
        }
        UUID roleId = idGenerator.nextId();
        tx.insertInto(ROLES, ROLES.ID, ROLES.CODE, ROLES.SCOPE, ROLES.CREATED_AT, ROLES.UPDATED_AT)
                .values(roleId, PLATFORM_ADMIN_ROLE, "PLATFORM", now, now)
                .execute();
        UUID wildcardId = tx.select(PERMISSIONS.ID).from(PERMISSIONS).where(PERMISSIONS.CODE.eq(WILDCARD)).fetchOne(PERMISSIONS.ID);
        if (wildcardId == null) {
            throw new IllegalStateException("Missing wildcard permission: " + WILDCARD);
        }
        tx.insertInto(ROLE_PERMISSIONS, ROLE_PERMISSIONS.ROLE_ID, ROLE_PERMISSIONS.PERMISSION_ID)
                .values(roleId, wildcardId)
                .onConflictDoNothing()
                .execute();
        return roleId;
    }

    private UUID upsertAdminUser(DSLContext tx, String sub, OffsetDateTime now) {
        UUID existing = tx.select(USERS.ID).from(USERS).where(USERS.SUB.eq(sub)).fetchOne(USERS.ID);
        if (existing != null) {
            return existing;
        }
        UUID id = idGenerator.nextId();
        tx.insertInto(USERS, USERS.ID, USERS.SUB, USERS.EMAIL, USERS.TENANT_ID, USERS.MUST_CHANGE_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
                .values(id, sub, bootstrap.adminEmail(), null, true, now, now)
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

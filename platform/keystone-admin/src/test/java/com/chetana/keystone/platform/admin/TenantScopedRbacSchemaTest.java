package com.chetana.keystone.platform.admin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.data.PlatformDataModule;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.TENANTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The invariants the {@code 0003} changeset creates, asserted against a real PostgreSQL rather than
 * assumed: {@code code} is unique **per owner** (not globally), a tenant-owned row must be
 * {@code TENANT} scope, and the indexes each query relies on exist.
 */
@Testcontainers(disabledWithoutDocker = true)
class TenantScopedRbacSchemaTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private DSLContext db;
    private UUID acme;
    private UUID globex;

    @BeforeEach
    void migrateAndSeedTwoTenants() {
        DatabaseConfig config = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");
        Injector injector = Guice.createInjector(new PlatformDataModule(config));
        injector.getInstance(AdminMigrationRunner.class).migrate();
        db = injector.getInstance(Key.get(DSLContext.class, Platform.class));
        // The container is shared by every test in the class, and tenant name/slug are unique, so
        // each seed run gets its own pair.
        String token = UUID.randomUUID().toString().substring(0, 8);
        acme = insertTenant("Acme " + token, "acme-" + token);
        globex = insertTenant("Globex " + token, "globex-" + token);
    }

    @Test
    void should_keep_a_global_role_code_globally_unique() {
        // 0001's global UNIQUE (code) is gone, so the partial index is what keeps the global pool
        // unique: without it a unique index would treat two NULL owners as distinct.
        insertRole("manager", "TENANT", null);

        assertThatThrownBy(() -> insertRole("manager", "TENANT", null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_roles_code_global");
    }

    @Test
    void should_allow_the_same_role_code_in_two_tenants_but_not_twice_in_one() {
        insertRole("manager", "TENANT", acme);
        insertRole("manager", "TENANT", globex);

        assertThatThrownBy(() -> insertRole("manager", "TENANT", acme))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_roles_code_tenant");
    }

    @Test
    void should_reject_a_tenant_owned_row_that_is_platform_scope() {
        assertThatThrownBy(() -> insertRole("cross-tenant", "PLATFORM", acme))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_roles_tenant_scope");

        assertThatThrownBy(() -> insertPermission("platform:thing:read-write", "PLATFORM", acme))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_permissions_tenant_scope");
    }

    @Test
    void should_accept_a_global_permission_and_a_tenant_owned_one_with_the_same_code() {
        insertPermission("inventory:item:read-write", "TENANT", null);
        insertPermission("inventory:item:read-write", "TENANT", acme);
    }

    @Test
    void should_carry_every_query_serving_index() {
        List<String> indexes = db.fetch("select indexname from pg_indexes where schemaname = 'platform'")
                .into(String.class);

        assertThat(indexes).contains(
                "uq_roles_code_global",
                "uq_permissions_code_global",
                "idx_roles_tenant_code",
                "idx_permissions_tenant_code",
                "idx_role_permissions_permission",
                "idx_user_roles_tenant",
                "idx_users_tenant");
    }

    private UUID insertTenant(String name, String slug) {
        UUID id = UUID.randomUUID();
        db.insertInto(TENANTS, TENANTS.ID, TENANTS.NAME, TENANTS.SLUG, TENANTS.CREATED_AT, TENANTS.UPDATED_AT)
                .values(id, name, slug, NOW, NOW)
                .execute();
        return id;
    }

    private void insertRole(String code, String scope, UUID tenantId) {
        db.insertInto(ROLES, ROLES.ID, ROLES.CODE, ROLES.SCOPE, ROLES.TENANT_ID, ROLES.CREATED_AT, ROLES.UPDATED_AT)
                .values(UUID.randomUUID(), code, scope, tenantId, NOW, NOW)
                .execute();
    }

    private void insertPermission(String code, String scope, UUID tenantId) {
        db.insertInto(PERMISSIONS, PERMISSIONS.ID, PERMISSIONS.CODE, PERMISSIONS.SCOPE, PERMISSIONS.TENANT_ID,
                        PERMISSIONS.CREATED_AT, PERMISSIONS.UPDATED_AT)
                .values(UUID.randomUUID(), code, scope, tenantId, NOW, NOW)
                .execute();
    }
}

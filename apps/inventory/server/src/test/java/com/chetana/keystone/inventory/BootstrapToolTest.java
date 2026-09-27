package com.chetana.keystone.inventory;

import com.google.inject.AbstractModule;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.inventory.config.AppConfig;
import com.chetana.keystone.platform.admin.PermissionCatalog;
import com.chetana.keystone.platform.admin.config.AdminConfig;
import com.chetana.keystone.platform.admin.supabase.Session;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class BootstrapToolTest {

    /** The schema the platform changelog — and therefore its generated jOOQ types — is pinned to. */
    private static final String PLATFORM_SCHEMA = "platform";
    private static final String ADMIN_SUB = "00000000-0000-0000-0000-00000000bbbb";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void should_seed_the_catalog_role_and_first_admin_user_idempotently() throws Exception {
        // The schemas must exist first — the separate step a MIGRATE_ON_START=false deployment runs.
        new SchemaTool().run(SchemaTool.Command.MIGRATE, databaseConfig("inventory"), databaseConfig(PLATFORM_SCHEMA));

        BootstrapTool tool = new BootstrapTool();
        tool.run(appConfig(), bootstrapConfig(true), fakeSupabase());

        assertThat(count("SELECT count(*) FROM platform.permissions")).isEqualTo(PermissionCatalog.PERMISSIONS.size());
        assertThat(count("SELECT count(*) FROM platform.roles")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM platform.role_permissions")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM platform.users")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM platform.user_roles")).isEqualTo(1);
        assertThat(string("SELECT code FROM platform.roles")).isEqualTo(PermissionCatalog.PLATFORM_ADMIN_ROLE);
        assertThat(string("SELECT username FROM platform.users WHERE tenant_id IS NULL")).isEqualTo("admin");
        assertThat(string("SELECT sub FROM platform.users")).isEqualTo(ADMIN_SUB);
        assertThat(string("SELECT must_change_password::text FROM platform.users")).isEqualTo("true");

        // Running it again changes nothing: the same catalog, role, user and single grant.
        tool.run(appConfig(), bootstrapConfig(true), fakeSupabase());

        assertThat(count("SELECT count(*) FROM platform.permissions")).isEqualTo(PermissionCatalog.PERMISSIONS.size());
        assertThat(count("SELECT count(*) FROM platform.roles")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM platform.users")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM platform.user_roles")).isEqualTo(1);
    }

    @Test
    void should_refuse_to_run_when_the_bootstrap_is_disabled() {
        assertThatThrownBy(() -> new BootstrapTool().run(appConfig(), bootstrapConfig(false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_ON_START=true");
    }

    private static AppConfig appConfig() {
        DatabaseConfig platform = databaseConfig(PLATFORM_SCHEMA);
        AppConfig.Database database = new AppConfig.Database(
                platform.url(), platform.username(), platform.password(), platform.maxPoolSize(), "inventory",
                new AppConfig.Read(platform.url(), platform.username(), platform.password(), platform.maxPoolSize()));
        return new AppConfig(
                "test",
                database,
                new AppConfig.Platform(PLATFORM_SCHEMA),
                new AppConfig.Startup(true),
                new AppConfig.Server(8080, "/inventory"),
                new AppConfig.Cors(List.of()),
                new AppConfig.Realtime("", ""),
                new AppConfig.Security("", ""));
    }

    private static AdminConfig bootstrapConfig(boolean enabled) {
        return new AdminConfig(
                "test",
                new AdminConfig.Supabase("https://test.supabase.co", "service-role-key"),
                new AdminConfig.Security(
                        "https://test.supabase.co/auth/v1",
                        "authenticated",
                        "https://test.supabase.co/auth/v1/.well-known/jwks.json"),
                new AdminConfig.Bootstrap(enabled, "admin", "admin@keystone.com", "changeit"));
    }

    private static DatabaseConfig databaseConfig(String schema) {
        return DatabaseConfig.of(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, schema);
    }

    /** Replaces the Supabase Auth admin client: the test never calls the real API. */
    private static AbstractModule fakeSupabase() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(SupabaseAdminClient.class).toInstance(new FakeSupabaseAdminClient());
            }
        };
    }

    /** The bootstrap only provisions an Auth identity — login and password updates are not used. */
    private static final class FakeSupabaseAdminClient implements SupabaseAdminClient {

        @Override
        public String createUser(String email, String password) {
            return ADMIN_SUB;
        }

        @Override
        public Optional<String> findSubByEmail(String email) {
            return Optional.of(ADMIN_SUB);
        }

        @Override
        public Session login(String email, String password) {
            throw new UnsupportedOperationException("login is not part of the bootstrap");
        }

        @Override
        public void updatePassword(String sub, String password) {
            throw new UnsupportedOperationException("updatePassword is not part of the bootstrap");
        }
    }

    /** Rows read straight from the container, so the assertions do not depend on the code under test. */
    private static long count(String sql) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String string(String sql) throws Exception {
        try (Connection connection = POSTGRES.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}

package com.chetana.keystone.platform.admin;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.id.UuidIdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.common.time.SystemDateTimeService;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.platform.admin.auth.TokenAuthenticator;
import com.chetana.keystone.platform.admin.config.AdminConfig;
import com.chetana.keystone.platform.admin.data.PlatformDataModule;
import com.chetana.keystone.platform.admin.supabase.Session;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import com.chetana.keystone.security.Principal;
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class AdminIntegrationTest {

    private static final String ADMIN_SUB = "00000000-0000-0000-0000-00000000aaaa";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    @Test
    void should_bootstrap_admin_login_and_manage_tenants_and_users() {
        Injector injector = buildInjector();
        injector.getInstance(AdminMigrationRunner.class).migrate();
        injector.getInstance(BootstrapRunner.class).bootstrap();

        Javalin app = injector.getInstance(Javalin.class);

        JavalinTest.test(app, (javalin, client) -> {
            assertThat(client.get("/healthz").code()).isEqualTo(200);

            var me = client.get("/api/v1/me", req -> req.header("Authorization", "Bearer test-token"));
            assertThat(me.code()).isEqualTo(200);
            assertThat(me.body().string())
                    .contains("\"sub\":\"" + ADMIN_SUB + "\"")
                    .contains("\"mustChangePassword\":true")
                    .contains("\"*\"");

            // Backend-proxied login with the reserved platform slug.
            var login = client.post("/api/v1/auth/login",
                    Map.of("identifier", "admin@keystone", "password", "changeit"));
            assertThat(login.code()).isEqualTo(200);
            assertThat(login.body().string()).contains("\"accessToken\":\"access-token\"");

            // Uniform error for unknown tenant — no account enumeration.
            assertThat(client.post("/api/v1/auth/login",
                    Map.of("identifier", "nobody@ghost", "password", "x")).code()).isEqualTo(403);

            // Change the admin's password through the backend.
            var changed = client.post("/api/v1/me/password", Map.of("password", "new-password"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(changed.code()).isEqualTo(204);

            // Create a tenant (with slug) and a tenant user (username -> derived email).
            var tenant = client.post("/api/v1/tenants", Map.of("name", "Acme", "slug", "acme"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(tenant.code()).isEqualTo(201);
            String tenantId = parseId(tenant.body().string());

            var user = client.post("/api/v1/users", Map.of(
                            "username", "alice",
                            "tenantId", tenantId,
                            "temporaryPassword", "temp-pass",
                            "roles", java.util.List.of()),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(user.code()).isEqualTo(201);
            assertThat(user.body().string()).contains("\"username\":\"alice\"", "\"email\":\"alice@acme.com\"");

            // The tenant user can log in with username@tenantid.
            var tenantLogin = client.post("/api/v1/auth/login",
                    Map.of("identifier", "alice@acme", "password", "temp-pass"));
            assertThat(tenantLogin.code()).isEqualTo(200);
        });
    }

    private Injector buildInjector() {
        DatabaseConfig db = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");
        SupabaseAdminClient fakeSupabase = new SupabaseAdminClient() {
            @Override
            public String createUser(String email, String password) {
                // Real Supabase Auth returns a unique sub per user; mimic that so `users.sub`
                // (unique) is not violated. The admin keeps ADMIN_SUB for the /me assertions.
                return email.equals("admin@keystone.com") ? ADMIN_SUB : "sub:" + email;
            }

            @Override
            public Optional<String> findSubByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public Session login(String email, String password) {
                return new Session("access-token", "refresh-token", "bearer", 3600);
            }

            @Override
            public void updatePassword(String sub, String password) {
            }
        };
        return Guice.createInjector(Stage.PRODUCTION,
                new PlatformDataModule(db),
                new WebModule(),
                Modules.override(new AdminModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(AdminConfig.Bootstrap.class).toInstance(
                                new AdminConfig.Bootstrap("admin", "admin@keystone.com", "changeit"));
                        bind(IdGenerator.class).to(UuidIdGenerator.class);
                        bind(DateTimeService.class).to(SystemDateTimeService.class);
                        bind(TokenAuthenticator.class).toInstance(token -> new Principal(ADMIN_SUB, Map.of()));
                        bind(SupabaseAdminClient.class).toInstance(fakeSupabase);
                    }
                }));
    }

    private static String parseId(String body) {
        int idx = body.indexOf("\"id\":\"");
        return body.substring(idx + 6, body.indexOf('"', idx + 6));
    }
}

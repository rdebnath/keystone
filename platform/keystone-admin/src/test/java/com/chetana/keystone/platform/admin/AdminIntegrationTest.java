package com.chetana.keystone.platform.admin;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import com.chetana.keystone.common.error.AccessDeniedException;
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
import com.chetana.keystone.security.Claims;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

            // The catalog is two access levels per resource.
            var permissions = client.get("/api/v1/permissions",
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(permissions.code()).isEqualTo(200);
            assertThat(permissions.body().string())
                    .contains("platform:tenant:read-only", "platform:tenant:read-write")
                    .contains("tenant:user:read-only", "tenant:user:read-write");

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

            // That change flipped must_change_password, so from now on the caller must PROVE the current
            // password: a bearer token alone is not enough to set a new one.
            assertThat(client.post("/api/v1/me/password", Map.of("password", "another-password"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.post("/api/v1/me/password",
                    Map.of("currentPassword", "wrong", "password", "another-password"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.post("/api/v1/me/password",
                    Map.of("currentPassword", "new-password", "password", " "),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);

            var voluntary = client.post("/api/v1/me/password",
                    Map.of("currentPassword", "new-password", "password", "another-password"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(voluntary.code()).isEqualTo(204);

            // Auth now holds the new password (the proof was a real password grant, and so is the next login).
            assertThat(client.post("/api/v1/auth/login",
                    Map.of("identifier", "admin@keystone", "password", "another-password")).code()).isEqualTo(200);
            assertThat(client.post("/api/v1/auth/login",
                    Map.of("identifier", "admin@keystone", "password", "new-password")).code()).isEqualTo(403);

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
            // Read the body once: the test client streams it.
            String userBody = user.body().string();
            assertThat(userBody).contains("\"username\":\"alice\"", "\"email\":\"alice@acme.com\"");

            // The tenant user can log in with username@tenantid.
            var tenantLogin = client.post("/api/v1/auth/login",
                    Map.of("identifier", "alice@acme", "password", "temp-pass"));
            assertThat(tenantLogin.code()).isEqualTo(200);

            // The tenant list carries the synthetic platform tenant first, and the real one is flagged.
            var tenants = client.get("/api/v1/tenants",
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(tenants.code()).isEqualTo(200);
            String tenantList = tenants.body().string();
            assertThat(tenantList)
                    .contains("\"id\":\"" + PlatformSchema.PLATFORM_TENANT_ID + "\"")
                    .contains("\"name\":\"Keystone\"")
                    .contains("\"slug\":\"keystone\"")
                    .contains("\"platform\":true");
            assertThat(tenantList.indexOf("Keystone")).isLessThan(tenantList.indexOf("Acme"));
            assertThat(tenantList).contains("\"platform\":false");

            // Users can be listed per plane: platform users, one tenant's users, or everyone.
            String platformUsers = client.get("/api/v1/users?tenantId=" + PlatformSchema.PLATFORM_TENANT_ID,
                    req -> req.header("Authorization", "Bearer test-token")).body().string();
            assertThat(platformUsers).contains("\"username\":\"admin\"").doesNotContain("\"username\":\"alice\"");
            String adminId = parseId(platformUsers);

            String acmeUsers = client.get("/api/v1/users?tenantId=" + tenantId,
                    req -> req.header("Authorization", "Bearer test-token")).body().string();
            assertThat(acmeUsers).contains("\"username\":\"alice\"").doesNotContain("\"username\":\"admin\"");

            String everyone = client.get("/api/v1/users",
                    req -> req.header("Authorization", "Bearer test-token")).body().string();
            assertThat(everyone).contains("\"username\":\"admin\"", "\"username\":\"alice\"");

            // A tenant role can be granted to a tenant user; a platform role cannot.
            var tenantRole = client.post("/api/v1/roles", Map.of(
                            "code", "tenant-admin",
                            "scope", "TENANT",
                            "permissions", java.util.List.of("tenant:user:read-write")),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(tenantRole.code()).isEqualTo(201);

            String aliceId = parseId(userBody);
            var renamed = client.patch("/api/v1/users/" + aliceId,
                    Map.of("username", "alice-b", "roles", java.util.List.of("tenant-admin")),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(renamed.code()).isEqualTo(200);
            assertThat(renamed.body().string())
                    .contains("\"username\":\"alice-b\"")
                    .contains("\"roles\":[\"tenant-admin\"]");

            var crossPlane = client.patch("/api/v1/users/" + aliceId,
                    Map.of("username", "alice-b", "roles", java.util.List.of("platform-admin")),
                    req -> req.header("Authorization", "Bearer test-token"));
            // A rejected value is a ValidationException, which the RFC 9457 mapper answers as 422.
            assertThat(crossPlane.code()).isEqualTo(422);

            // An administrator resets ANOTHER user's password: the target gets a temporary password and is
            // forced to change it on the next login.
            var reset = client.put("/api/v1/users/" + aliceId + "/password",
                    Map.of("temporaryPassword", "reset-pass"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(reset.code()).isEqualTo(204);
            assertThat(client.post("/api/v1/auth/login",
                    Map.of("identifier", "alice-b@acme", "password", "reset-pass")).code()).isEqualTo(200);
            assertThat(client.post("/api/v1/auth/login",
                    Map.of("identifier", "alice-b@acme", "password", "temp-pass")).code()).isEqualTo(403);
            assertThat(client.get("/api/v1/users?tenantId=" + tenantId,
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"mustChangePassword\":true");

            // Rejected resets: a blank temporary password, an unknown user, the caller's own account (which
            // must prove the current password through /me/password) and an unauthenticated caller.
            assertThat(client.put("/api/v1/users/" + aliceId + "/password",
                    Map.of("temporaryPassword", " "),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.put("/api/v1/users/" + UUID.randomUUID() + "/password",
                    Map.of("temporaryPassword", "reset-pass"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(404);
            assertThat(client.put("/api/v1/users/" + adminId + "/password",
                    Map.of("temporaryPassword", "reset-pass"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.put("/api/v1/users/" + aliceId + "/password",
                    Map.of("temporaryPassword", "reset-pass")).code()).isEqualTo(403);

            // The reserved platform slug and id are not real tenants: neither can be created or edited.
            assertThat(client.post("/api/v1/tenants", Map.of("name", "Impostor", "slug", "keystone"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.patch("/api/v1/tenants/" + PlatformSchema.PLATFORM_TENANT_ID,
                    Map.of("name", "Keystone 2", "slug", "keystone-2"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.delete("/api/v1/tenants/" + PlatformSchema.PLATFORM_TENANT_ID, null,
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);

            // A tenant with users cannot be deleted; removing the user unblocks it.
            assertThat(client.delete("/api/v1/tenants/" + tenantId, null,
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(409);
            assertThat(client.delete("/api/v1/users/" + aliceId, null,
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(204);
            assertThat(client.delete("/api/v1/tenants/" + tenantId, null,
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(204);
            assertThat(client.get("/api/v1/tenants",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .doesNotContain("\"name\":\"Acme\"");
        });
    }

    private Injector buildInjector() {
        DatabaseConfig db = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");
        SupabaseAdminClient fakeSupabase = new FakeSupabaseAdminClient();
        return Guice.createInjector(Stage.PRODUCTION,
                new PlatformDataModule(db),
                new WebModule(),
                Modules.override(new AdminModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(AdminConfig.Bootstrap.class).toInstance(
                                new AdminConfig.Bootstrap(true, "admin", "admin@keystone.com", "changeit"));
                        bind(IdGenerator.class).to(UuidIdGenerator.class);
                        bind(DateTimeService.class).to(SystemDateTimeService.class);
                        bind(TokenAuthenticator.class).toInstance(token -> new Principal(ADMIN_SUB, Claims.empty()));
                        bind(SupabaseAdminClient.class).toInstance(fakeSupabase);
                    }
                }));
    }

    private static String parseId(String body) {
        int idx = body.indexOf("\"id\":\"");
        return body.substring(idx + 6, body.indexOf('"', idx + 6));
    }

    /**
     * A stand-in for Supabase Auth that actually remembers each account's password, so the two password
     * flows can be exercised honestly: login is a real (here: local) password grant, and the
     * current-password proof fails unless the caller sends the password Auth holds. Supabase Auth itself
     * returns a unique sub per user; the admin keeps {@link #ADMIN_SUB} for the {@code /me} assertions.
     */
    private static final class FakeSupabaseAdminClient implements SupabaseAdminClient {

        private final Map<String, String> emailBySub = new ConcurrentHashMap<>();
        private final Map<String, String> passwordBySub = new ConcurrentHashMap<>();

        FakeSupabaseAdminClient() {
            remember("admin@keystone.com", ADMIN_SUB, "changeit");
        }

        @Override
        public String createUser(String email, String password) {
            String sub = email.equals("admin@keystone.com") ? ADMIN_SUB : "sub:" + email;
            remember(email, sub, password);
            return sub;
        }

        @Override
        public Optional<String> findSubByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public Session login(String email, String password) {
            boolean known = emailBySub.entrySet().stream()
                    .anyMatch(entry -> entry.getValue().equals(email)
                            && password.equals(passwordBySub.get(entry.getKey())));
            if (!known) {
                throw new AccessDeniedException("Invalid username, tenant, or password.");
            }
            return new Session("access-token", "refresh-token", "bearer", 3600);
        }

        @Override
        public void updatePassword(String sub, String password) {
            emailBySub.computeIfAbsent(sub, key -> key.startsWith("sub:") ? key.substring(4) : key);
            passwordBySub.put(sub, password);
        }

        private void remember(String email, String sub, String password) {
            emailBySub.put(sub, email);
            passwordBySub.put(sub, password);
        }
    }
}

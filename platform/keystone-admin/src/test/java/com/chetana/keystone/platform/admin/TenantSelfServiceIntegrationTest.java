package com.chetana.keystone.platform.admin;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.common.error.ConflictException;
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
import io.javalin.testtools.HttpClient;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tenant self-service plane over HTTP, end to end: the {@code admin} role a tenant is seeded with, what
 * a tenant admin may do inside its own tenant, and — the security-critical part — that the tenant it acts
 * in is derived from its own user row and can never be named by the request.
 *
 * <p>The bearer token <em>is</em> the subject here, so one test can act as the platform admin, a tenant
 * admin, or another tenant's admin without a real identity provider.
 */
@Testcontainers(disabledWithoutDocker = true)
class TenantSelfServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private static final String ADMIN_SUB = "sub:admin@keystone.com";

    private Javalin app;

    @BeforeEach
    void boot() {
        DatabaseConfig db = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "platform");
        SupabaseAdminClient fakeSupabase = new FakeSupabaseAdminClient();
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new PlatformDataModule(db),
                new WebModule(),
                Modules.override(new AdminModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(AdminConfig.Bootstrap.class).toInstance(
                                new AdminConfig.Bootstrap(true, "admin", "admin@keystone.com", "changeit"));
                        bind(IdGenerator.class).to(UuidIdGenerator.class);
                        bind(DateTimeService.class).to(SystemDateTimeService.class);
                        // The bearer token is the subject: a test can act as any user.
                        bind(TokenAuthenticator.class).toInstance(token -> new Principal(token, Claims.empty()));
                        bind(SupabaseAdminClient.class).toInstance(fakeSupabase);
                    }
                }));
        injector.getInstance(AdminMigrationRunner.class).migrate();
        injector.getInstance(BootstrapRunner.class).bootstrap();
        app = injector.getInstance(Javalin.class);
    }

    @Test
    void should_seed_the_tenants_admin_role_with_the_read_write_level_only() {
        JavalinTest.test(app, (javalin, client) -> {
            String tenantId = createTenant(client, unique("Acme"), unique("acme"));

            String roles = client.get("/api/v1/roles?tenantId=" + tenantId,
                    req -> req.header("Authorization", bearer(ADMIN_SUB))).body().string();

            assertThat(roles)
                    .contains("\"code\":\"admin\"", tenantId)
                    .contains("tenant:user:read-write", "tenant:role:read-write", "tenant:permission:read-write")
                    // A read/write grant already satisfies every read check, so the read-only codes are absent.
                    .doesNotContain("tenant:user:read-only", "tenant:role:read-only", "tenant:permission:read-only");
        });
    }

    @Test
    void should_refuse_the_platform_plane_on_the_tenant_routes() {
        JavalinTest.test(app, (javalin, client) -> {
            // The platform plane has its own routes for the same resources; these exist for a tenant to
            // administer itself, and the caller must actually be one.
            assertThat(client.get("/api/v1/tenant/users",
                    req -> req.header("Authorization", bearer(ADMIN_SUB))).code()).isEqualTo(403);
            assertThat(client.get("/api/v1/tenant/roles",
                    req -> req.header("Authorization", bearer(ADMIN_SUB))).code()).isEqualTo(403);
            assertThat(client.get("/api/v1/tenant/permissions",
                    req -> req.header("Authorization", bearer(ADMIN_SUB))).code()).isEqualTo(403);
        });
    }

    @Test
    void should_let_a_tenant_admin_run_its_own_tenant_and_reach_nothing_else() {
        JavalinTest.test(app, (javalin, client) -> {
            String acmeSlug = unique("acme");
            String acmeId = createTenant(client, unique("Acme"), acmeSlug);
            String aliceSub = createTenantAdmin(client, acmeId, "alice");

            // She sees her own tenant, and the global catalog alongside its own roles.
            assertThat(client.get("/api/v1/tenant/users",
                    req -> req.header("Authorization", bearer(aliceSub))).body().string())
                    .contains("\"username\":\"alice\"");
            assertThat(client.get("/api/v1/tenant/roles",
                    req -> req.header("Authorization", bearer(aliceSub))).body().string())
                    .contains("\"code\":\"admin\"", "\"code\":\"platform-admin\"");

            // She may create a role granting the read-only level of a resource she holds read/write on —
            // the same implication the guard uses (write implies read).
            assertThat(client.post("/api/v1/tenant/roles",
                    Map.of("code", "manager", "permissions", List.of("tenant:user:read-only")),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(201);

            // ... but cannot escalate: the wildcard is refused outright, and a cross-tenant PLATFORM code
            // is one she does not hold.
            assertThat(client.post("/api/v1/tenant/roles",
                    Map.of("code", "boss", "permissions", List.of("*")),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(422);
            assertThat(client.post("/api/v1/tenant/roles",
                    Map.of("code", "boss", "permissions", List.of("platform:tenant:read-write")),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(422);

            // She creates a user in her own tenant, assigning the role she just made and recording a phone
            // number; naming a tenant in the body is refused.
            var bob = client.post("/api/v1/tenant/users",
                    Map.of("username", "bob", "temporaryPassword", "temp-pass",
                            "phoneNumber", "+91 98765 43210", "roles", List.of("manager")),
                    req -> req.header("Authorization", bearer(aliceSub)));
            assertThat(bob.code()).isEqualTo(201);
            assertThat(bob.body().string()).contains("\"phoneNumber\":\"+919876543210\"");
            // The tenant plane validates the same values the platform plane does.
            assertThat(client.post("/api/v1/tenant/users",
                    Map.of("username", "mallory", "temporaryPassword", "temp-pass", "phoneNumber", "nope"),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(422);
            assertThat(client.post("/api/v1/tenant/users",
                    Map.of("username", "eve", "tenantId", acmeId, "temporaryPassword", "temp-pass"),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(422);

            // Another tenant, with its own admin, user and role.
            String globexId = createTenant(client, unique("Globex"), unique("globex"));
            String carolSub = createTenantAdmin(client, globexId, "carol");
            String carolRoleId = parseId(client.post("/api/v1/tenant/roles",
                    Map.of("code", "reports", "permissions", List.of()),
                    req -> req.header("Authorization", bearer(carolSub))).body().string());
            String carolId = idNear(client.get("/api/v1/users?tenantId=" + globexId,
                    req -> req.header("Authorization", bearer(ADMIN_SUB))).body().string(),
                    "\"username\":\"carol\"");

            // Another tenant's user is simply not there for her...
            assertThat(client.patch("/api/v1/tenant/users/" + carolId, Map.of("username", "carol-2"),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(404);
            assertThat(client.delete("/api/v1/tenant/users/" + carolId, null,
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(404);
            // ... and neither is its role.
            assertThat(client.delete("/api/v1/tenant/roles/" + carolRoleId, null,
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(404);

            // Her own admin role is protected, and a global role is read-only to her.
            String herRoles = client.get("/api/v1/tenant/roles",
                    req -> req.header("Authorization", bearer(aliceSub))).body().string();
            assertThat(client.delete("/api/v1/tenant/roles/" + idNear(herRoles, "\"code\":\"admin\""), null,
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(403);
            assertThat(client.delete("/api/v1/tenant/roles/"
                            + idNear(herRoles, "\"code\":\"platform-admin\""), null,
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(403);
        });
    }

    /** A tenant's first administrator: the platform assigns the tenant's own seeded {@code admin} role. */
    private static String createTenantAdmin(HttpClient client, String tenantId, String username) {
        var response = client.post("/api/v1/users", Map.of(
                        "username", username,
                        "tenantId", tenantId,
                        "temporaryPassword", "temp-pass",
                        "roles", List.of("admin")),
                req -> req.header("Authorization", bearer(ADMIN_SUB)));
        assertThat(response.code()).isEqualTo(201);
        return field(response.body().string(), "sub");
    }

    private static String createTenant(HttpClient client, String name, String slug) {
        var response = client.post("/api/v1/tenants", Map.of("name", name, "slug", slug),
                req -> req.header("Authorization", bearer(ADMIN_SUB)));
        assertThat(response.code()).isEqualTo(201);
        return parseId(response.body().string());
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String bearer(String subject) {
        return "Bearer " + subject;
    }

    private static String parseId(String json) {
        return field(json, "id");
    }

    /** The value of the first {@code "name":"…"} string field in a JSON payload. */
    private static String field(String json, String name) {
        int at = json.indexOf("\"" + name + "\":\"");
        assertThat(at).as(name + " present in " + json).isGreaterThan(-1);
        int from = at + name.length() + 4;
        return json.substring(from, json.indexOf('"', from));
    }

    /** The {@code id} of the object whose text contains {@code marker} (a DTO serializes its id first). */
    private static String idNear(String json, String marker) {
        int at = json.indexOf(marker);
        assertThat(at).as("marker present: " + marker).isGreaterThan(-1);
        int idAt = json.lastIndexOf("\"id\":\"", at);
        assertThat(idAt).as("id before " + marker).isGreaterThan(-1);
        return json.substring(idAt + 6, json.indexOf('"', idAt + 6));
    }

    private static String sub(String email) {
        return "sub:" + email;
    }

    /** A Supabase Auth stand-in with real adoption semantics: an existing email conflicts, and a password
     *  grant succeeds only with the password Auth holds. */
    private static final class FakeSupabaseAdminClient implements SupabaseAdminClient {

        private final Map<String, String> passwordBySub = new ConcurrentHashMap<>();

        @Override
        public String createUser(String email, String password) {
            String sub = sub(email);
            if (passwordBySub.containsKey(sub)) {
                throw new ConflictException("Supabase user already exists: " + email);
            }
            passwordBySub.put(sub, password);
            return sub;
        }

        @Override
        public Optional<String> findSubByEmail(String email) {
            String sub = sub(email);
            return passwordBySub.containsKey(sub) ? Optional.of(sub) : Optional.empty();
        }

        @Override
        public Session login(String email, String password) {
            if (!password.equals(passwordBySub.get(sub(email)))) {
                throw new AccessDeniedException("Invalid username, tenant, or password.");
            }
            return new Session("access-token", "refresh-token", "bearer", 3600);
        }

        @Override
        public void updatePassword(String sub, String password) {
            passwordBySub.put(sub, password);
        }
    }

    @Test
    void should_refuse_a_tenant_role_that_shadows_a_global_catalog_role() {
        JavalinTest.test(app, (javalin, client) -> {
            String acmeSlug = unique("acme");
            String acmeId = createTenant(client, unique("Acme"), acmeSlug);
            String aliceSub = createTenantAdmin(client, acmeId, "alice");

            // The platform adds a global catalog role...
            assertThat(client.post("/api/v1/roles",
                    Map.of("code", "catalog-auditor", "scope", "TENANT", "permissions", List.of()),
                    req -> req.header("Authorization", bearer(ADMIN_SUB))).code()).isEqualTo(201);

            // ... which a tenant may then use, but not re-define: a code must never mean two things.
            assertThat(client.post("/api/v1/tenant/roles",
                    Map.of("code", "catalog-auditor", "permissions", List.of()),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(409);
        });
    }

    @Test
    void should_filter_its_own_catalog_by_access_level() {
        JavalinTest.test(app, (javalin, client) -> {
            String acmeSlug = unique("acme");
            String acmeId = createTenant(client, unique("Acme"), acmeSlug);
            String aliceSub = createTenantAdmin(client, acmeId, "alice");

            // The tenant defines a permission of its own, at the read-only level.
            assertThat(client.post("/api/v1/tenant/permissions",
                    Map.of("code", "tenant:" + acmeSlug + ":report:read-only", "scope", "TENANT"),
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(201);

            // The level filter is the last segment of the code, on the tenant plane exactly as on the
            // platform one — so a console's picker can offer "the read-only grants" here too.
            assertThat(client.get("/api/v1/tenant/permissions?access=read-only",
                    req -> req.header("Authorization", bearer(aliceSub))).body().string())
                    .contains(":report:read-only")
                    .doesNotContain(":read-write");
            assertThat(client.get("/api/v1/tenant/permissions?access=read-write",
                    req -> req.header("Authorization", bearer(aliceSub))).body().string())
                    .contains("tenant:user:read-write")
                    .doesNotContain(":read-only");
            // A level that is neither is a rejected value, not an ignored filter.
            assertThat(client.get("/api/v1/tenant/permissions?access=sideways",
                    req -> req.header("Authorization", bearer(aliceSub))).code()).isEqualTo(422);
        });
    }
}

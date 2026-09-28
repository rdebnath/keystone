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

            // Create a tenant (with slug and country) and a tenant user (username -> derived email).
            var tenant = client.post("/api/v1/tenants",
                    Map.of("name", "Acme", "slug", "acme", "country", "in"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(tenant.code()).isEqualTo(201);
            // Read each body once: the test client streams it.
            String tenantBody = tenant.body().string();
            // The country is normalized to its canonical ISO 3166-1 alpha-2 form.
            assertThat(tenantBody).contains("\"country\":\"IN\"");
            String tenantId = parseId(tenantBody);

            var user = client.post("/api/v1/users", Map.of(
                            "username", "alice",
                            "tenantId", tenantId,
                            "phoneNumber", "+91 98765 43210",
                            "temporaryPassword", "temp-pass",
                            "roles", java.util.List.of()),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(user.code()).isEqualTo(201);
            String userBody = user.body().string();
            assertThat(userBody)
                    .contains("\"username\":\"alice\"", "\"email\":\"alice@acme.com\"")
                    // The phone number is normalized to E.164, so what is stored is what a dialler needs.
                    .contains("\"phoneNumber\":\"+919876543210\"");

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
            // The country travels with the row; the platform tenant records none, so it reads as null.
            assertThat(tenantList).contains("\"country\":\"IN\"", "\"country\":null");

            // A patch replaces the country too — the update takes the same full body as create.
            var reCountryed = client.patch("/api/v1/tenants/" + tenantId,
                    Map.of("name", "Acme", "slug", "acme", "country", "gb"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(reCountryed.code()).isEqualTo(200);
            assertThat(reCountryed.body().string()).contains("\"country\":\"GB\"");

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

            // The access-level filter narrows the catalog to one level of the *code* — the segment every
            // permission ends in — so the console's picker can offer "read-only grants only".
            assertThat(client.get("/api/v1/permissions?access=read-only",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("platform:tenant:read-only")
                    .doesNotContain(":read-write");
            assertThat(client.get("/api/v1/permissions?access=read-write",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("platform:tenant:read-write")
                    .doesNotContain(":read-only");
            // A level that is neither is a rejected value, never a silently ignored filter.
            assertThat(client.get("/api/v1/permissions?access=sideways",
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);

            // A permission a tenant defines may be granted to that tenant's own role — and NOT to a global
            // one: a global role's grants are held by every tenant, so a tenant-owned code there would leak
            // one tenant's permission into all the others.
            assertThat(client.post("/api/v1/permissions", Map.of(
                            "code", "tenant:acme:audit:read-only",
                            "scope", "TENANT",
                            "tenantId", tenantId),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(201);
            assertThat(client.post("/api/v1/roles", Map.of(
                            "code", "acme-auditor",
                            "scope", "TENANT",
                            "tenantId", tenantId,
                            "permissions", java.util.List.of("tenant:acme:audit:read-only")),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(201);
            var globalRoleWithTenantPermission = client.post("/api/v1/roles", Map.of(
                            "code", "everyone-auditor",
                            "scope", "TENANT",
                            "permissions", java.util.List.of("tenant:acme:audit:read-only")),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(globalRoleWithTenantPermission.code()).isEqualTo(422);
            assertThat(globalRoleWithTenantPermission.body().string()).contains("global catalog only");

            // The phone number is editable (unlike the email): a patch sets it, and a value that is not
            // E.164 is a rejected value.
            var rePhoned = client.patch("/api/v1/users/" + aliceId,
                    Map.of("username", "alice-b", "phoneNumber", "+14155552671"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(rePhoned.code()).isEqualTo(200);
            assertThat(rePhoned.body().string()).contains("\"phoneNumber\":\"+14155552671\"");
            assertThat(client.patch("/api/v1/users/" + aliceId,
                    Map.of("username", "alice-b", "phoneNumber", "12345"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);

            // A country that is not an ISO 3166-1 alpha-2 code is rejected on create and on update.
            assertThat(client.post("/api/v1/tenants", Map.of("name", "Nope", "slug", "nope", "country", "IND"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.patch("/api/v1/tenants/" + tenantId,
                    Map.of("name", "Acme", "slug", "acme", "country", "ZZ"),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);

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

    /**
     * The point of this delivery: search and paging happen **on the server**, so a search finds a row that
     * is not on the page the client happens to hold — and every window is consistent with the totals.
     */
    @Test
    void should_page_search_and_filter_the_lists_on_the_server() {
        Injector injector = buildInjector();
        injector.getInstance(AdminMigrationRunner.class).migrate();
        injector.getInstance(BootstrapRunner.class).bootstrap();

        Javalin app = injector.getInstance(Javalin.class);

        JavalinTest.test(app, (javalin, client) -> {
            for (int index = 1; index <= 5; index++) {
                assertThat(client.post("/api/v1/tenants",
                        Map.of("name", "Tenant " + index, "slug", "tenant-" + index),
                        req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(201);
            }

            // A page is a window with totals, not the whole list. Six rows = the pinned platform row + five
            // tenants, so two rows per page is three pages.
            String firstPage = client.get("/api/v1/tenants?size=2",
                    req -> req.header("Authorization", "Bearer test-token")).body().string();
            assertThat(firstPage)
                    .contains("\"page\":0", "\"size\":2", "\"totalElements\":6", "\"totalPages\":3")
                    .contains("\"hasNext\":true", "\"hasPrevious\":false")
                    .contains("\"name\":\"Keystone\"", "Tenant 1")
                    .doesNotContain("Tenant 2");

            String secondPage = client.get("/api/v1/tenants?size=2&page=1",
                    req -> req.header("Authorization", "Bearer test-token")).body().string();
            assertThat(secondPage)
                    .contains("\"page\":1", "\"hasPrevious\":true")
                    .contains("Tenant 2", "Tenant 3")
                    // No row appears on two pages, and none is skipped between them.
                    .doesNotContain("Tenant 1", "Tenant 4", "\"name\":\"Keystone\"");

            // The search is applied by the server, so it finds a tenant that sorts past the first page.
            String found = client.get("/api/v1/tenants?q=tenant-5&size=2",
                    req -> req.header("Authorization", "Bearer test-token")).body().string();
            assertThat(found)
                    .contains("\"totalElements\":1", "Tenant 5")
                    .doesNotContain("Tenant 1")
                    // The pinned row is searched like any other: a result may not contain a row that does
                    // not match the term.
                    .doesNotContain("\"name\":\"Keystone\"");

            assertThat(client.get("/api/v1/tenants?q=key",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"name\":\"Keystone\"");

            // LIKE wildcards are literal: `%` is a character, not "everything".
            assertThat(client.get("/api/v1/tenants?q=%25",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"totalElements\":0");

            // A published sort key works; an unpublished one, a bad direction, an oversized page and a
            // negative page are all refused rather than ignored.
            assertThat(client.get("/api/v1/tenants?sort=slug&order=desc",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"items\"");
            assertThat(client.get("/api/v1/tenants?sort=nope",
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.get("/api/v1/tenants?order=sideways",
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.get("/api/v1/tenants?size=101",
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.get("/api/v1/tenants?page=-1",
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);
            assertThat(client.get("/api/v1/tenants?q=" + "a".repeat(101),
                    req -> req.header("Authorization", "Bearer test-token")).code()).isEqualTo(422);

            // A stale page is not an error: an empty window with the real totals.
            assertThat(client.get("/api/v1/tenants?page=99",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"items\":[]", "\"totalElements\":6");

            // The pickers read the unpaged options route — a complete set, not a page of one.
            assertThat(client.get("/api/v1/tenants/options",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"truncated\":false", "\"name\":\"Keystone\"", "Tenant 5");

            // The other three lists answer in the same shape, and their own filters still apply.
            assertThat(client.get("/api/v1/users?q=nobody",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"items\":[]", "\"totalElements\":0");
            assertThat(client.get("/api/v1/roles?q=admin&scope=TENANT",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("tenant-admin")
                    .doesNotContain("platform-admin");
            assertThat(client.get("/api/v1/roles/options",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("\"items\":[", "tenant-admin");
            assertThat(client.get("/api/v1/permissions?q=platform%3Atenant",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .contains("platform:tenant:read-only");
            assertThat(client.get("/api/v1/permissions?scope=TENANT",
                    req -> req.header("Authorization", "Bearer test-token")).body().string())
                    .doesNotContain("platform:tenant:read-only");
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

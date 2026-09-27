package com.chetana.keystone.platform.admin.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import com.chetana.keystone.platform.admin.PermissionCatalog;
import com.chetana.keystone.security.Claims;
import com.chetana.keystone.security.Principal;
import com.chetana.keystone.web.RouteConfigurer;
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two-level rule over HTTP: a read-only grant reads but cannot mutate, a read/write grant does
 * both, and the wildcard still grants everything.
 *
 * <p>The caller's principal and permission set are preset on the request context — the same
 * attribute the guard caches — so the guard's resolver (and its database) is never reached.
 */
class PermissionGuardTest {

    private static final String RESOURCE = PermissionCatalog.PLATFORM_TENANT;

    @Test
    void should_allow_read_and_deny_write_for_a_read_only_grant() {
        JavalinTest.test(app(Set.of("platform:tenant:read-only")), (javalin, client) -> {
            assertThat(client.get("/api/v1/tenants").code()).isEqualTo(200);
            assertThat(client.post("/api/v1/tenants", Map.of()).code()).isEqualTo(403);
        });
    }

    @Test
    void should_allow_read_and_write_for_a_read_write_grant() {
        JavalinTest.test(app(Set.of("platform:tenant:read-write")), (javalin, client) -> {
            assertThat(client.get("/api/v1/tenants").code()).isEqualTo(200);
            assertThat(client.post("/api/v1/tenants", Map.of()).code()).isEqualTo(200);
        });
    }

    @Test
    void should_allow_read_and_write_for_the_wildcard() {
        JavalinTest.test(app(Set.of(PermissionCatalog.WILDCARD)), (javalin, client) -> {
            assertThat(client.get("/api/v1/tenants").code()).isEqualTo(200);
            assertThat(client.post("/api/v1/tenants", Map.of()).code()).isEqualTo(200);
        });
    }

    @Test
    void should_deny_read_and_write_without_a_grant() {
        JavalinTest.test(app(Set.of()), (javalin, client) -> {
            assertThat(client.get("/api/v1/tenants").code()).isEqualTo(403);
            assertThat(client.post("/api/v1/tenants", Map.of()).code()).isEqualTo(403);
        });
    }

    @Test
    void should_not_accept_a_grant_for_another_resource() {
        JavalinTest.test(app(Set.of("tenant:user:read-write")), (javalin, client) -> {
            assertThat(client.get("/api/v1/tenants").code()).isEqualTo(403);
            assertThat(client.post("/api/v1/tenants", Map.of()).code()).isEqualTo(403);
        });
    }

    private static Javalin app(Set<String> permissions) {
        // The resolver is never consulted: the permission set is already on the context.
        PermissionGuard guard = new PermissionGuard(null);

        return Guice.createInjector(
                new WebModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        Multibinder<RouteConfigurer> routes =
                                Multibinder.newSetBinder(binder(), RouteConfigurer.class);
                        routes.addBinding().toInstance(r -> {
                            r.before("/api/v1/tenants", ctx -> {
                                ctx.attribute(AuthFilter.PRINCIPAL_ATTRIBUTE,
                                        new Principal("sub", Claims.empty()));
                                ctx.attribute(PermissionGuard.PERMISSIONS_ATTRIBUTE, permissions);
                            });
                            r.get("/api/v1/tenants", ctx -> {
                                guard.requireRead(ctx, RESOURCE);
                                ctx.result("read");
                            });
                            r.post("/api/v1/tenants", ctx -> {
                                guard.requireWrite(ctx, RESOURCE);
                                ctx.result("write");
                            });
                        });
                    }
                }).getInstance(Javalin.class);
    }
}

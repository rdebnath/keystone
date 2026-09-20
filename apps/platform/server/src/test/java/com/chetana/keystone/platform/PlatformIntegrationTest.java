package com.chetana.keystone.platform;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import com.chetana.keystone.data.DataModule;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.platform.auth.TokenAuthenticator;
import com.chetana.keystone.platform.config.AppConfig;
import com.chetana.keystone.platform.supabase.SupabaseAdminClient;
import com.chetana.keystone.security.Principal;
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PlatformIntegrationTest {

    private static final String ADMIN_SUB = "00000000-0000-0000-0000-00000000aaaa";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void should_bootstrap_admin_and_manage_tenants() {
        Injector injector = buildInjector();
        injector.getInstance(MigrationRunner.class).migrate();
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

            assertThat(client.get("/api/v1/tenants").code()).isEqualTo(403);

            var changed = client.post("/api/v1/me/password-changed", null,
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(changed.code()).isEqualTo(204);

            var meAfter = client.get("/api/v1/me", req -> req.header("Authorization", "Bearer test-token"));
            assertThat(meAfter.body().string()).contains("\"mustChangePassword\":false");

            var created = client.post("/api/v1/tenants", Map.of("name", "Acme"),
                    req -> req.header("Authorization", "Bearer test-token"));
            assertThat(created.code()).isEqualTo(201);

            var list = client.get("/api/v1/tenants", req -> req.header("Authorization", "Bearer test-token"));
            assertThat(list.code()).isEqualTo(200);
            assertThat(list.body().string()).contains("\"name\":\"Acme\"");
        });
    }

    private Injector buildInjector() {
        DatabaseConfig db = DatabaseConfig.of(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5);
        return Guice.createInjector(Stage.PRODUCTION,
                new DataModule(db),
                new WebModule(),
                Modules.override(new PlatformModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(AppConfig.Bootstrap.class).toInstance(new AppConfig.Bootstrap("admin@keystone.local", "changeit"));
                        bind(TokenAuthenticator.class).toInstance(token -> new Principal(ADMIN_SUB, Map.of()));
                        bind(SupabaseAdminClient.class).toInstance((email, password) -> ADMIN_SUB);
                    }
                }));
    }
}

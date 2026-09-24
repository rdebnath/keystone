package com.chetana.keystone.inventory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.chetana.keystone.data.DataModule;
import com.chetana.keystone.data.DatabaseConfig;
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
class InventoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void should_migrate_create_and_list_items() {
        DatabaseConfig db = DatabaseConfig.of(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 5, "inventory");

        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new DataModule(db), new WebModule(), new InventoryModule());

        injector.getInstance(MigrationRunner.class).migrate();

        Javalin app = injector.getInstance(Javalin.class);

        JavalinTest.test(app, (javalin, client) -> {
            var create = client.post("/api/v1/items", Map.of("name", "Widget", "quantity", 5));
            assertThat(create.getCode()).isEqualTo(201);

            var list = client.get("/api/v1/items");
            assertThat(list.getCode()).isEqualTo(200);
            assertThat(list.getBody().string()).contains("\"name\":\"Widget\"");
        });
    }
}

package com.chetana.keystone.inventory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.chetana.keystone.data.DataModule;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.inventory.config.AppConfig;
import com.chetana.keystone.inventory.config.ConfigLoader;
import com.chetana.keystone.inventory.config.ConfigModule;
import com.chetana.keystone.observability.MetricsModule;
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;

/**
 * Inventory application entry point. Resolves configuration, builds the Guice {@link Injector},
 * applies the database migration, then starts the HTTP server.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = ConfigLoader.load();
        DatabaseConfig db = DatabaseConfig.of(
                config.database().url(),
                config.database().username(),
                config.database().password(),
                config.database().maxPoolSize());

        Injector injector = Guice.createInjector(
                Stage.PRODUCTION,
                new ConfigModule(config),
                new DataModule(db),
                new WebModule(),
                new MetricsModule(),
                new InventoryModule());

        injector.getInstance(MigrationRunner.class).migrate();

        injector.getInstance(Javalin.class).start(config.server().port());
    }
}

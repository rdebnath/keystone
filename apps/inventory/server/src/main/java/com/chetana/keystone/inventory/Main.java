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
import com.chetana.keystone.platform.admin.AdminMigrationRunner;
import com.chetana.keystone.platform.admin.AdminModule;
import com.chetana.keystone.platform.admin.BootstrapRunner;
import com.chetana.keystone.platform.admin.config.AdminConfig;
import com.chetana.keystone.platform.admin.config.AdminConfigLoader;
import com.chetana.keystone.platform.admin.config.AdminConfigModule;
import com.chetana.keystone.platform.admin.config.SecurityConfigFactory;
import com.chetana.keystone.platform.admin.data.PlatformDataModule;
import com.chetana.keystone.security.SecurityConfig;
import com.chetana.keystone.security.SecurityModule;
import com.chetana.keystone.web.CorsConfig;
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inventory application entry point. Hosts the platform admin console library alongside the
 * inventory service: both schemas are migrated (unless {@code startup.migrateOnStart=false}), the
 * first platform user is bootstrapped (unless the platform bootstrap is disabled), and the shared
 * HTTP server serves both the inventory API and the platform admin API.
 */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = ConfigLoader.load();
        AdminConfig adminConfig = AdminConfigLoader.load();
        SecurityConfig security = SecurityConfigFactory.load(adminConfig.security());

        DatabaseConfig inventoryDb = DatabaseConfig.of(
                config.database().url(),
                config.database().username(),
                config.database().password(),
                config.database().maxPoolSize(),
                config.database().schema());
        DatabaseConfig inventoryRead = readDatabase(config.database(), config.database().schema());
        DatabaseConfig platformDb = DatabaseConfig.of(
                config.database().url(),
                config.database().username(),
                config.database().password(),
                config.database().maxPoolSize(),
                config.platform().schema());
        DatabaseConfig platformRead = readDatabase(config.database(), config.platform().schema());

        Injector injector = Guice.createInjector(
                Stage.PRODUCTION,
                new ConfigModule(config),
                new DataModule(inventoryDb, inventoryRead),
                new AdminConfigModule(adminConfig),
                new PlatformDataModule(platformDb, platformRead),
                new WebModule(new CorsConfig(config.cors().allowedOrigins()), config.server().contextPath()),
                new MetricsModule(),
                new SecurityModule(security),
                new AdminModule(),
                new InventoryModule());

        if (config.startup().migrateOnStart()) {
            injector.getInstance(MigrationRunner.class).migrate();
            injector.getInstance(AdminMigrationRunner.class).migrate();
        } else {
            log.info("Automatic Liquibase migration is disabled (startup.migrateOnStart=false /"
                            + " MIGRATE_ON_START=false); schemas '{}' and '{}' are expected to be migrated"
                            + " out of band (SchemaTool migrate) before serving traffic.",
                    inventoryDb.schema(), platformDb.schema());
        }
        injector.getInstance(BootstrapRunner.class).bootstrap();

        injector.getInstance(Javalin.class).start(config.server().port());
    }

    /** Maps a resolved read target to a {@link DatabaseConfig}; the same URL as the primary aliases it. */
    private static DatabaseConfig readDatabase(AppConfig.Database database, String schema) {
        AppConfig.Read read = database.read();
        return DatabaseConfig.of(read.url(), read.username(), read.password(), read.maxPoolSize(), schema);
    }
}

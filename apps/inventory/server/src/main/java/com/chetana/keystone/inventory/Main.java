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
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;

/**
 * Inventory application entry point. Hosts the platform admin console library alongside the
 * inventory service: both schemas are migrated, the first platform user is bootstrapped, and the
 * shared HTTP server serves both the inventory API and the platform admin API.
 */
public final class Main {

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
        DatabaseConfig platformDb = DatabaseConfig.of(
                adminConfig.database().url(),
                adminConfig.database().username(),
                adminConfig.database().password(),
                adminConfig.database().maxPoolSize(),
                adminConfig.database().schema());

        Injector injector = Guice.createInjector(
                Stage.PRODUCTION,
                new ConfigModule(config),
                new DataModule(inventoryDb),
                new AdminConfigModule(adminConfig),
                new PlatformDataModule(platformDb),
                new WebModule(),
                new MetricsModule(),
                new SecurityModule(security),
                new AdminModule(),
                new InventoryModule());

        injector.getInstance(MigrationRunner.class).migrate();
        injector.getInstance(AdminMigrationRunner.class).migrate();
        injector.getInstance(BootstrapRunner.class).bootstrap();

        injector.getInstance(Javalin.class).start(config.server().port());
    }
}

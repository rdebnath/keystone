package com.chetana.keystone.platform;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.chetana.keystone.data.DataModule;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.observability.MetricsModule;
import com.chetana.keystone.platform.config.AppConfig;
import com.chetana.keystone.platform.config.ConfigLoader;
import com.chetana.keystone.platform.config.ConfigModule;
import com.chetana.keystone.platform.config.SecurityConfigFactory;
import com.chetana.keystone.security.SecurityConfig;
import com.chetana.keystone.security.SecurityModule;
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;

/**
 * Platform admin console entry point. Resolves configuration, builds the Guice {@link Injector},
 * applies the database migration and idempotent bootstrap, then starts the HTTP server.
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
        SecurityConfig security = SecurityConfigFactory.load(config.security());

        Injector injector = Guice.createInjector(
                Stage.PRODUCTION,
                new ConfigModule(config),
                new DataModule(db),
                new WebModule(),
                new MetricsModule(),
                new SecurityModule(security),
                new PlatformModule());

        injector.getInstance(MigrationRunner.class).migrate();
        injector.getInstance(BootstrapRunner.class).bootstrap();

        injector.getInstance(Javalin.class).start(config.server().port());
    }
}

package com.chetana.keystone.inventory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.id.UuidIdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.common.time.SystemDateTimeService;
import com.chetana.keystone.data.DatabaseConfig;
import com.chetana.keystone.inventory.config.AppConfig;
import com.chetana.keystone.inventory.config.ConfigLoader;
import com.chetana.keystone.platform.admin.BootstrapRunner;
import com.chetana.keystone.platform.admin.config.AdminConfig;
import com.chetana.keystone.platform.admin.config.AdminConfigLoader;
import com.chetana.keystone.platform.admin.config.AdminConfigModule;
import com.chetana.keystone.platform.admin.data.PlatformDataModule;
import com.chetana.keystone.platform.admin.supabase.SupabaseAdminClient;
import com.chetana.keystone.platform.admin.supabase.SupabaseHttpAdminClient;
import com.chetana.keystone.web.WebModule;

/**
 * Command-line first-user bootstrap — the explicit counterpart of the server's automatic startup
 * seed ({@code bootstrap.enabled} / {@code BOOTSTRAP_ON_START}).
 *
 * <p>Run it to seed the platform when the server starts with the automatic bootstrap switched off, or
 * when the seed belongs in its own step of a release. It is idempotent and does everything
 * {@link BootstrapRunner} does: seed the permission catalog, create the {@code platform-admin} role
 * (granted the wildcard {@code *} permission) if it is missing, provision the first platform admin in
 * Supabase Auth (service-role key) and grant it that role. The schemas must already exist — run
 * {@link SchemaTool migrate} (or {@code scripts/migrate-schema.sh}) first when
 * {@code MIGRATE_ON_START=false}.
 *
 * <p>Environment selection works like the server: {@code APP_ENV} (default {@code dev}) picks the
 * per-environment config files, and {@code DB_PASSWORD}, {@code SUPABASE_SERVICE_ROLE_KEY} and
 * {@code BOOTSTRAP_ADMIN_PASSWORD} come from the environment. The bootstrap must be enabled
 * ({@code BOOTSTRAP_ON_START=true}, the default): a deployment that switched it off cannot seed by
 * accident, and gets a clear error instead of a silent no-op.
 *
 * <pre>
 * APP_ENV=demo DB_PASSWORD=... SUPABASE_SERVICE_ROLE_KEY=... \
 *   java -cp "apps/inventory/server/target/classes:$(cat apps/inventory/server/target/classpath.txt)" \
 *   com.chetana.keystone.inventory.BootstrapTool
 * </pre>
 */
public final class BootstrapTool {

    public static void main(String[] args) {
        try {
            new BootstrapTool().run(ConfigLoader.load(), AdminConfigLoader.load());
        } catch (Exception e) {
            System.err.println("BootstrapTool failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Bootstraps against the given configuration. Package-private for tests, which supply a
     * container-backed {@link AppConfig} and can override a binding (e.g. the Supabase client).
     */
    void run(AppConfig app, AdminConfig admin, Module... overrides) {
        if (!admin.bootstrap().enabled()) {
            throw new IllegalStateException("Platform bootstrap is disabled (bootstrap.enabled=false /"
                    + " BOOTSTRAP_ON_START=false); run it with BOOTSTRAP_ON_START=true to seed the permission"
                    + " catalog, the platform-admin role and the first platform admin user.");
        }
        DatabaseConfig platformDb = DatabaseConfig.of(
                app.database().url(),
                app.database().username(),
                app.database().password(),
                app.database().maxPoolSize(),
                app.platform().schema());

        Module wiring = Modules.override(
                        new AdminConfigModule(admin),
                        new PlatformDataModule(platformDb),
                        new WebModule())
                .with(new BootstrapCollaborators());
        if (overrides.length > 0) {
            wiring = Modules.override(wiring).with(overrides);
        }

        Injector injector = Guice.createInjector(Stage.PRODUCTION, wiring);
        injector.getInstance(BootstrapRunner.class).bootstrap();
    }

    /**
     * The collaborators the bootstrap needs, which the hosted server gets from its own bundle: the
     * common infrastructure every application binds for the platform library ({@code IdGenerator},
     * {@code DateTimeService} — see {@code AdminModule}), the Supabase Auth admin client, and the
     * runner itself. {@link WebModule} is included for the application's shared {@code ObjectMapper};
     * its {@code Javalin} instance is created but never started.
     */
    private static final class BootstrapCollaborators extends AbstractModule {

        @Override
        protected void configure() {
            bind(IdGenerator.class).to(UuidIdGenerator.class);
            bind(DateTimeService.class).to(SystemDateTimeService.class);
            bind(SupabaseAdminClient.class).to(SupabaseHttpAdminClient.class);
            bind(BootstrapRunner.class);
        }
    }
}

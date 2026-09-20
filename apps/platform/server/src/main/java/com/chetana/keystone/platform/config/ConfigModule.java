package com.chetana.keystone.platform.config;

import com.google.inject.AbstractModule;

/**
 * Binds the resolved {@link AppConfig} — and its per-concern slices — into Guice so services and
 * handlers can {@code @Inject} exactly the configuration they need.
 */
public final class ConfigModule extends AbstractModule {

    private final AppConfig config;

    public ConfigModule(AppConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(AppConfig.class).toInstance(config);
        bind(AppConfig.Database.class).toInstance(config.database());
        bind(AppConfig.Server.class).toInstance(config.server());
        bind(AppConfig.Security.class).toInstance(config.security());
        bind(AppConfig.Supabase.class).toInstance(config.supabase());
        bind(AppConfig.Bootstrap.class).toInstance(config.bootstrap());
    }
}

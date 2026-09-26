package com.chetana.keystone.inventory.config;

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
        bind(AppConfig.Platform.class).toInstance(config.platform());
        bind(AppConfig.Server.class).toInstance(config.server());
        bind(AppConfig.Realtime.class).toInstance(config.realtime());
        bind(AppConfig.Security.class).toInstance(config.security());
    }
}

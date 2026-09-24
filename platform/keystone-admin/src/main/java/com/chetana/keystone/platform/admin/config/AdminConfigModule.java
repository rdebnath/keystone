package com.chetana.keystone.platform.admin.config;

import com.google.inject.AbstractModule;

/**
 * Binds the resolved {@link AdminConfig} — and its per-concern slices — into Guice so services and
 * handlers can {@code @Inject} exactly the configuration they need.
 */
public final class AdminConfigModule extends AbstractModule {

    private final AdminConfig config;

    public AdminConfigModule(AdminConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(AdminConfig.class).toInstance(config);
        bind(AdminConfig.Database.class).toInstance(config.database());
        bind(AdminConfig.Supabase.class).toInstance(config.supabase());
        bind(AdminConfig.Security.class).toInstance(config.security());
        bind(AdminConfig.Bootstrap.class).toInstance(config.bootstrap());
    }
}

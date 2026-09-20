package com.chetana.keystone.security;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Binds the {@link JwtAuthenticator} from the application's {@link SecurityConfig}.
 */
public final class SecurityModule extends AbstractModule {

    private final SecurityConfig config;

    public SecurityModule(SecurityConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    JwtAuthenticator jwtAuthenticator() {
        return new JwtAuthenticator(config.jwkSet(), config.issuer(), config.audience());
    }
}

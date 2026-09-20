package com.chetana.keystone.platform.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.security.JwtAuthenticator;
import com.chetana.keystone.security.Principal;

/**
 * Production {@link TokenAuthenticator} that delegates to the platform OIDC resource-server
 * {@link JwtAuthenticator}.
 */
@Singleton
public final class JwtTokenAuthenticator implements TokenAuthenticator {

    private final JwtAuthenticator delegate;

    @Inject
    public JwtTokenAuthenticator(JwtAuthenticator delegate) {
        this.delegate = delegate;
    }

    @Override
    public Principal authenticate(String token) {
        return delegate.authenticate(token);
    }
}

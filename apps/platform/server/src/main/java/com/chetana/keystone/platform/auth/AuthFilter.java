package com.chetana.keystone.platform.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.security.Principal;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

/**
 * Authenticates every request under {@code /api/*} by validating the bearer token and storing the
 * resolved {@link Principal} in the request context.
 */
@Singleton
public final class AuthFilter implements RouteConfigurer {

    public static final String PRINCIPAL_ATTRIBUTE = "principal";

    private final TokenAuthenticator authenticator;

    @Inject
    public AuthFilter(TokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.before("/api/*", this::authenticate);
    }

    private void authenticate(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing or invalid Authorization header");
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new AccessDeniedException("Missing bearer token");
        }
        ctx.attribute(PRINCIPAL_ATTRIBUTE, authenticator.authenticate(token));
    }
}

package com.chetana.keystone.platform.admin.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.security.Principal;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

/**
 * Authenticates every request under {@code /api/*} by validating the bearer token and storing the
 * resolved {@link Principal} in the request context. The login endpoint is exempt (it has no token
 * yet), and CORS preflight ({@code OPTIONS}) requests are never authenticated (they carry no token).
 */
@Singleton
public final class AuthFilter implements RouteConfigurer {

    public static final String PRINCIPAL_ATTRIBUTE = "principal";
    private static final String LOGIN_PATH = "/api/v1/auth/login";

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
        if (ctx.method() == HandlerType.OPTIONS) {
            return; // CORS preflight — never carries a bearer token
        }
        if (LOGIN_PATH.equals(pathWithinContext(ctx))) {
            return; // the login endpoint has no token yet
        }
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

    /**
     * {@link Context#path()} returns the full request URI including the context path (e.g.
     * {@code /inventory/api/v1/auth/login}). Strip it so route comparisons are context-path agnostic.
     */
    private static String pathWithinContext(Context ctx) {
        String path = ctx.path();
        String contextPath = ctx.contextPath();
        if (!contextPath.isEmpty() && !contextPath.equals("/") && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }
}

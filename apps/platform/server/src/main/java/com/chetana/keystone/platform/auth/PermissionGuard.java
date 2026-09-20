package com.chetana.keystone.platform.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.platform.identity.PermissionResolver;
import com.chetana.keystone.security.Principal;
import io.javalin.http.Context;

import java.util.Set;

/**
 * Enforces permission checks at the handler boundary. Code checks permissions, never roles; the
 * wildcard permission {@code *} grants everything.
 */
@Singleton
public final class PermissionGuard {

    public static final String PERMISSIONS_ATTRIBUTE = "permissions";

    private final PermissionResolver resolver;

    @Inject
    public PermissionGuard(PermissionResolver resolver) {
        this.resolver = resolver;
    }

    public Principal principal(Context ctx) {
        Principal principal = ctx.attribute(AuthFilter.PRINCIPAL_ATTRIBUTE);
        if (principal == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return principal;
    }

    public void require(Context ctx, String permission) {
        Principal principal = principal(ctx);
        Set<String> permissions = ctx.attribute(PERMISSIONS_ATTRIBUTE);
        if (permissions == null) {
            permissions = resolver.resolve(principal.subject(), null);
            ctx.attribute(PERMISSIONS_ATTRIBUTE, permissions);
        }
        if (!permissions.contains("*") && !permissions.contains(permission)) {
            throw new AccessDeniedException("Missing permission: " + permission);
        }
    }
}

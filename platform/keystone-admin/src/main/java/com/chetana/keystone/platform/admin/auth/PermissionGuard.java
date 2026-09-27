package com.chetana.keystone.platform.admin.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.platform.admin.PermissionCatalog;
import com.chetana.keystone.platform.admin.identity.Access;
import com.chetana.keystone.platform.admin.identity.PermissionResolver;
import com.chetana.keystone.security.Principal;
import io.javalin.http.Context;

import java.util.List;
import java.util.Set;

import static com.chetana.keystone.platform.admin.PermissionCatalog.WILDCARD;

/**
 * Enforces permission checks at the handler boundary. Code checks permissions, never roles; every
 * resource is checked at one of two access levels and the wildcard {@code *} grants everything.
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

    /** Requires read access to {@code resource} — satisfied by its read-only or read/write code. */
    public void requireRead(Context ctx, String resource) {
        requireAny(ctx, PermissionCatalog.acceptedCodes(resource, Access.READ_ONLY));
    }

    /** Requires write access to {@code resource} — satisfied only by its read/write code. */
    public void requireWrite(Context ctx, String resource) {
        requireAny(ctx, PermissionCatalog.acceptedCodes(resource, Access.READ_WRITE));
    }

    /** Pure decision: does this permission set grant one of {@code accepted}, or the wildcard? */
    static boolean grants(Set<String> permissions, List<String> accepted) {
        if (permissions.contains(WILDCARD)) {
            return true;
        }
        return accepted.stream().anyMatch(permissions::contains);
    }

    private void requireAny(Context ctx, List<String> accepted) {
        Set<String> permissions = ctx.attribute(PERMISSIONS_ATTRIBUTE);
        if (permissions == null) {
            permissions = resolver.resolve(principal(ctx).subject(), null);
            ctx.attribute(PERMISSIONS_ATTRIBUTE, permissions);
        }
        if (!grants(permissions, accepted)) {
            throw new AccessDeniedException("Missing permission: " + String.join(" or ", accepted));
        }
    }
}

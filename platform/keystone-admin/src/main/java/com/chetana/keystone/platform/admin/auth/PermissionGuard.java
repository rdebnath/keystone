package com.chetana.keystone.platform.admin.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.PermissionCatalog;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.identity.Access;
import com.chetana.keystone.platform.admin.identity.CallerScope;
import com.chetana.keystone.platform.admin.identity.PermissionResolver;
import com.chetana.keystone.security.Principal;
import io.javalin.http.Context;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.WILDCARD;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USERS;

/**
 * Enforces permission checks at the handler boundary, and resolves the caller's {@link CallerScope}
 * (their plane, their tenant, and their effective permissions) for the services that need it.
 *
 * <p>Code checks permissions, never roles; every resource is checked at one of two access levels and
 * the wildcard {@code *} grants everything.
 *
 * <p>Permissions are resolved <strong>in the caller's own tenant context</strong>: a platform user
 * (no tenant) gets the global grants, a tenant user the global grants plus its own tenant's. Resolving
 * everything as "platform" would silently deny every legitimate tenant admin.
 */
@Singleton
public final class PermissionGuard {

    public static final String PERMISSIONS_ATTRIBUTE = "permissions";
    private static final String CALLER_SCOPE_ATTRIBUTE = "callerScope";

    private final PermissionResolver resolver;
    private final DataAccess data;

    @Inject
    public PermissionGuard(PermissionResolver resolver, @Platform DataAccess data) {
        this.resolver = resolver;
        this.data = data;
    }

    public Principal principal(Context ctx) {
        Principal principal = ctx.attribute(AuthFilter.PRINCIPAL_ATTRIBUTE);
        if (principal == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return principal;
    }

    /**
     * The caller's plane, tenant and effective permissions, resolved once per request. The tenant comes
     * from the caller's own {@code users} row — never from the request — which is what makes
     * cross-tenant access structurally impossible rather than merely filtered.
     */
    public CallerScope callerScope(Context ctx) {
        CallerScope cached = ctx.attribute(CALLER_SCOPE_ATTRIBUTE);
        if (cached != null) {
            return cached;
        }
        Principal principal = principal(ctx);
        var user = data.read()
                .select(USERS.TENANT_ID)
                .from(USERS)
                .where(USERS.SUB.eq(principal.subject()))
                .fetchOne();
        if (user == null) {
            throw new NotFoundException("User not found: " + principal.subject());
        }
        UUID tenantId = user.value1();
        Set<String> permissions = resolver.resolve(principal.subject(), tenantId);
        ctx.attribute(PERMISSIONS_ATTRIBUTE, permissions);
        CallerScope scope = tenantId == null
                ? CallerScope.platformPlane(permissions)
                : CallerScope.tenant(tenantId, permissions);
        ctx.attribute(CALLER_SCOPE_ATTRIBUTE, scope);
        return scope;
    }

    /**
     * The caller's <strong>tenant</strong> scope, for the tenant self-service plane. A platform caller is
     * refused: those routes exist so a tenant can administer itself, and the platform plane already has its
     * own routes for the same resources.
     */
    public CallerScope callerTenantScope(Context ctx) {
        CallerScope scope = callerScope(ctx);
        if (scope.platform()) {
            throw new AccessDeniedException("This route is for tenant administrators");
        }
        return scope;
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
            permissions = callerScope(ctx).permissions();
        }
        if (!grants(permissions, accepted)) {
            throw new AccessDeniedException("Missing permission: " + String.join(" or ", accepted));
        }
    }
}

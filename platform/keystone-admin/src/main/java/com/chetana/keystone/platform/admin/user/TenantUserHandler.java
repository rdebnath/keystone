package com.chetana.keystone.platform.admin.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.platform.admin.identity.CallerScope;
import com.chetana.keystone.web.QueryParams;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.TENANT_USER;

/**
 * Tenant self-service routes for the {@code users} resource — what makes a tenant admin able to create
 * and run its own tenant.
 *
 * <p>The tenant is <strong>never</strong> taken from the request: the create body's {@code tenantId} is
 * refused and every target is checked against the caller's own tenant first, so a tenant admin can only
 * ever reach its own users. A platform caller is refused outright.
 */
@Singleton
public final class TenantUserHandler implements RouteConfigurer {

    private final UserService service;
    private final PermissionGuard guard;

    @Inject
    public TenantUserHandler(UserService service, PermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/tenant/users", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireRead(ctx, TENANT_USER);
            ctx.json(service.list(caller, null, QueryParams.search(ctx), QueryParams.page(ctx)));
        });

        routes.post("/api/v1/tenant/users", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_USER);
            UserRequest request = ctx.bodyAsClass(UserRequest.class);
            ctx.status(201).json(service.create(caller, request));
        });

        routes.patch("/api/v1/tenant/users/{id}", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            UserUpdateRequest request = ctx.bodyAsClass(UserUpdateRequest.class);
            ctx.json(service.update(caller, id, request));
        });

        routes.put("/api/v1/tenant/users/{id}/roles", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            AssignRolesRequest request = ctx.bodyAsClass(AssignRolesRequest.class);
            service.assignRoles(caller, id, request);
            ctx.status(204);
        });

        // Same rule as the platform plane: a temporary password for someone else, never your own account
        // (which goes through the verified POST /api/v1/me/password).
        routes.put("/api/v1/tenant/users/{id}/password", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            ResetPasswordRequest request = ctx.bodyAsClass(ResetPasswordRequest.class);
            service.resetPassword(caller, id, request, guard.principal(ctx).subject());
            ctx.status(204);
        });

        routes.delete("/api/v1/tenant/users/{id}", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(caller, id);
            ctx.status(204);
        });
    }
}

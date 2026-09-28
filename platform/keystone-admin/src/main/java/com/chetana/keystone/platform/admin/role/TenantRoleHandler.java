package com.chetana.keystone.platform.admin.role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.platform.admin.identity.CallerScope;
import com.chetana.keystone.platform.admin.identity.Scope;
import com.chetana.keystone.web.QueryParams;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.TENANT_ROLE;

/**
 * Tenant self-service routes for the {@code roles} resource.
 *
 * <p>The tenant is <strong>never</strong> taken from the request: there is no path segment, query
 * parameter or body field that could name one, because {@link PermissionGuard#callerTenantScope} derives
 * it from the caller's own user row. That is what makes cross-tenant access structurally impossible
 * rather than merely filtered — and a platform caller is refused outright, since it has the platform
 * plane for the same resources.
 */
@Singleton
public final class TenantRoleHandler implements RouteConfigurer {

    private final RoleService service;
    private final PermissionGuard guard;

    @Inject
    public TenantRoleHandler(RoleService service, PermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/tenant/roles", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireRead(ctx, TENANT_ROLE);
            Scope scope = Scope.optional(ctx.queryParam("scope"));
            ctx.json(service.list(caller, null, scope, QueryParams.search(ctx), QueryParams.page(ctx)));
        });

        // The tenant console's role checklist (assigning roles to one of its users) must offer every role
        // it may assign, so it reads the unpaged options route rather than a page.
        routes.get("/api/v1/tenant/roles/options", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireRead(ctx, TENANT_ROLE);
            ctx.json(service.options(caller, null));
        });

        routes.post("/api/v1/tenant/roles", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_ROLE);
            RoleRequest request = ctx.bodyAsClass(RoleRequest.class);
            ctx.status(201).json(service.create(caller, request));
        });

        routes.patch("/api/v1/tenant/roles/{id}", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_ROLE);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            RoleRequest request = ctx.bodyAsClass(RoleRequest.class);
            ctx.json(service.update(caller, id, request));
        });

        routes.delete("/api/v1/tenant/roles/{id}", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_ROLE);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(caller, id);
            ctx.status(204);
        });
    }
}

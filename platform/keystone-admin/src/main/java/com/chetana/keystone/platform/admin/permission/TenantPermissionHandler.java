package com.chetana.keystone.platform.admin.permission;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.platform.admin.identity.Access;
import com.chetana.keystone.platform.admin.identity.CallerScope;
import com.chetana.keystone.platform.admin.identity.Scope;
import com.chetana.keystone.web.QueryParams;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.TENANT_PERMISSION;

/**
 * Tenant self-service routes for the {@code permissions} resource. Like the role routes, the tenant comes
 * from the caller's own user row and never from the request, and a platform caller is refused.
 */
@Singleton
public final class TenantPermissionHandler implements RouteConfigurer {

    private final PermissionService service;
    private final PermissionGuard guard;

    @Inject
    public TenantPermissionHandler(PermissionService service, PermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/tenant/permissions", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireRead(ctx, TENANT_PERMISSION);
            Scope scope = Scope.optional(ctx.queryParam("scope"));
            Access access = Access.optional(ctx.queryParam("access"));
            ctx.json(service.list(caller, null, scope, access, QueryParams.search(ctx),
                    QueryParams.page(ctx)));
        });

        routes.post("/api/v1/tenant/permissions", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_PERMISSION);
            PermissionRequest request = ctx.bodyAsClass(PermissionRequest.class);
            ctx.status(201).json(service.create(caller, request));
        });

        routes.delete("/api/v1/tenant/permissions/{id}", ctx -> {
            CallerScope caller = guard.callerTenantScope(ctx);
            guard.requireWrite(ctx, TENANT_PERMISSION);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(caller, id);
            ctx.status(204);
        });
    }
}

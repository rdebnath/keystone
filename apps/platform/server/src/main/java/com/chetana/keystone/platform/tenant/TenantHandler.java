package com.chetana.keystone.platform.tenant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.Ids;
import com.chetana.keystone.platform.auth.PermissionGuard;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

/**
 * REST routes for the {@code tenants} resource.
 */
@Singleton
public final class TenantHandler implements RouteConfigurer {

    private final TenantService service;
    private final PermissionGuard guard;

    @Inject
    public TenantHandler(TenantService service, PermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/tenants", ctx -> {
            guard.require(ctx, "platform:tenant:read");
            ctx.json(service.list());
        });

        routes.post("/api/v1/tenants", ctx -> {
            guard.require(ctx, "platform:tenant:create");
            TenantRequest request = ctx.bodyAsClass(TenantRequest.class);
            ctx.status(201).json(service.create(request));
        });

        routes.patch("/api/v1/tenants/{id}", ctx -> {
            guard.require(ctx, "platform:tenant:update");
            UUID id = Ids.uuid(ctx.pathParam("id"));
            TenantRequest request = ctx.bodyAsClass(TenantRequest.class);
            ctx.json(service.update(id, request));
        });

        routes.delete("/api/v1/tenants/{id}", ctx -> {
            guard.require(ctx, "platform:tenant:delete");
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(id);
            ctx.status(204);
        });
    }
}

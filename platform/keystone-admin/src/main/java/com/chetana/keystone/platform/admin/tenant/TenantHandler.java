package com.chetana.keystone.platform.admin.tenant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.PLATFORM_TENANT;

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
            guard.requireRead(ctx, PLATFORM_TENANT);
            ctx.json(service.list());
        });

        routes.post("/api/v1/tenants", ctx -> {
            guard.requireWrite(ctx, PLATFORM_TENANT);
            TenantRequest request = ctx.bodyAsClass(TenantRequest.class);
            ctx.status(201).json(service.create(request));
        });

        routes.patch("/api/v1/tenants/{id}", ctx -> {
            guard.requireWrite(ctx, PLATFORM_TENANT);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            TenantRequest request = ctx.bodyAsClass(TenantRequest.class);
            ctx.json(service.update(id, request));
        });

        routes.delete("/api/v1/tenants/{id}", ctx -> {
            guard.requireWrite(ctx, PLATFORM_TENANT);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(id);
            ctx.status(204);
        });
    }
}

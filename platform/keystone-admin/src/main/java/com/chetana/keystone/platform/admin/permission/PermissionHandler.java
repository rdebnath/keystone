package com.chetana.keystone.platform.admin.permission;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

/**
 * REST routes for the {@code permissions} resource.
 */
@Singleton
public final class PermissionHandler implements RouteConfigurer {

    private final PermissionService service;
    private final PermissionGuard guard;

    @Inject
    public PermissionHandler(PermissionService service, PermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/permissions", ctx -> {
            guard.require(ctx, "platform:permission:read");
            ctx.json(service.list());
        });

        routes.post("/api/v1/permissions", ctx -> {
            guard.require(ctx, "platform:permission:create");
            PermissionRequest request = ctx.bodyAsClass(PermissionRequest.class);
            ctx.status(201).json(service.create(request));
        });

        routes.delete("/api/v1/permissions/{id}", ctx -> {
            guard.require(ctx, "platform:permission:delete");
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(id);
            ctx.status(204);
        });
    }
}

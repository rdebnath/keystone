package com.chetana.keystone.platform.admin.role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

/**
 * REST routes for the {@code roles} resource.
 */
@Singleton
public final class RoleHandler implements RouteConfigurer {

    private final RoleService service;
    private final PermissionGuard guard;

    @Inject
    public RoleHandler(RoleService service, PermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/roles", ctx -> {
            guard.require(ctx, "platform:role:read");
            ctx.json(service.list());
        });

        routes.post("/api/v1/roles", ctx -> {
            guard.require(ctx, "platform:role:create");
            RoleRequest request = ctx.bodyAsClass(RoleRequest.class);
            ctx.status(201).json(service.create(request));
        });

        routes.patch("/api/v1/roles/{id}", ctx -> {
            guard.require(ctx, "platform:role:update");
            UUID id = Ids.uuid(ctx.pathParam("id"));
            RoleRequest request = ctx.bodyAsClass(RoleRequest.class);
            ctx.json(service.update(id, request));
        });

        routes.delete("/api/v1/roles/{id}", ctx -> {
            guard.require(ctx, "platform:role:delete");
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(id);
            ctx.status(204);
        });
    }
}

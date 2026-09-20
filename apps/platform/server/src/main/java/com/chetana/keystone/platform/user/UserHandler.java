package com.chetana.keystone.platform.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.Ids;
import com.chetana.keystone.platform.auth.PermissionGuard;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

/**
 * REST routes for the {@code users} resource.
 */
@Singleton
public final class UserHandler implements RouteConfigurer {

    private final UserService service;
    private final PermissionGuard guard;

    @Inject
    public UserHandler(UserService service, PermissionGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/users", ctx -> {
            guard.require(ctx, "platform:user:read");
            ctx.json(service.list());
        });

        routes.post("/api/v1/users", ctx -> {
            guard.require(ctx, "platform:user:create");
            UserRequest request = ctx.bodyAsClass(UserRequest.class);
            ctx.status(201).json(service.create(request));
        });

        routes.put("/api/v1/users/{id}/roles", ctx -> {
            guard.require(ctx, "platform:user:assign-role");
            UUID id = Ids.uuid(ctx.pathParam("id"));
            AssignRolesRequest request = ctx.bodyAsClass(AssignRolesRequest.class);
            service.assignRoles(id, request);
            ctx.status(204);
        });

        routes.delete("/api/v1/users/{id}", ctx -> {
            guard.require(ctx, "platform:user:delete");
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(id);
            ctx.status(204);
        });
    }
}

package com.chetana.keystone.platform.admin.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.PLATFORM_USER;

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
            guard.requireRead(ctx, PLATFORM_USER);
            String tenantId = ctx.queryParam("tenantId");
            ctx.json(service.list(tenantId == null ? null : Ids.uuid(tenantId)));
        });

        routes.post("/api/v1/users", ctx -> {
            guard.requireWrite(ctx, PLATFORM_USER);
            UserRequest request = ctx.bodyAsClass(UserRequest.class);
            ctx.status(201).json(service.create(request));
        });

        routes.patch("/api/v1/users/{id}", ctx -> {
            guard.requireWrite(ctx, PLATFORM_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            UserUpdateRequest request = ctx.bodyAsClass(UserUpdateRequest.class);
            ctx.json(service.update(id, request));
        });

        routes.put("/api/v1/users/{id}/roles", ctx -> {
            guard.requireWrite(ctx, PLATFORM_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            AssignRolesRequest request = ctx.bodyAsClass(AssignRolesRequest.class);
            service.assignRoles(id, request);
            ctx.status(204);
        });

        // A user's own password goes through POST /api/v1/me/password (which proves the current
        // password); this route sets someone else's temporary password and forces a change on their
        // next login. The caller comes from the authenticated principal, never from the body.
        routes.put("/api/v1/users/{id}/password", ctx -> {
            guard.requireWrite(ctx, PLATFORM_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            ResetPasswordRequest request = ctx.bodyAsClass(ResetPasswordRequest.class);
            service.resetPassword(id, request, guard.principal(ctx).subject());
            ctx.status(204);
        });

        routes.delete("/api/v1/users/{id}", ctx -> {
            guard.requireWrite(ctx, PLATFORM_USER);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(id);
            ctx.status(204);
        });
    }
}

package com.chetana.keystone.platform.admin.role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.platform.admin.identity.Scope;
import com.chetana.keystone.web.QueryParams;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.PLATFORM_ROLE;

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
            guard.requireRead(ctx, PLATFORM_ROLE);
            UUID tenantId = Ids.optionalUuid(ctx.queryParam("tenantId"));
            Scope scope = Scope.optional(ctx.queryParam("scope"));
            ctx.json(service.list(guard.callerScope(ctx), tenantId, scope, QueryParams.search(ctx),
                    QueryParams.page(ctx)));
        });

        // The user editor's role checklist must offer *every* assignable role, so it reads this unpaged
        // route instead of a page — a checklist fed by page 1 would hide the rest of the catalogue.
        routes.get("/api/v1/roles/options", ctx -> {
            guard.requireRead(ctx, PLATFORM_ROLE);
            UUID tenantId = Ids.optionalUuid(ctx.queryParam("tenantId"));
            ctx.json(service.options(guard.callerScope(ctx), tenantId));
        });

        routes.post("/api/v1/roles", ctx -> {
            guard.requireWrite(ctx, PLATFORM_ROLE);
            RoleRequest request = ctx.bodyAsClass(RoleRequest.class);
            ctx.status(201).json(service.create(guard.callerScope(ctx), request));
        });

        routes.patch("/api/v1/roles/{id}", ctx -> {
            guard.requireWrite(ctx, PLATFORM_ROLE);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            RoleRequest request = ctx.bodyAsClass(RoleRequest.class);
            ctx.json(service.update(guard.callerScope(ctx), id, request));
        });

        routes.delete("/api/v1/roles/{id}", ctx -> {
            guard.requireWrite(ctx, PLATFORM_ROLE);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(guard.callerScope(ctx), id);
            ctx.status(204);
        });
    }
}

package com.chetana.keystone.platform.admin.permission;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.Ids;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.platform.admin.identity.Access;
import com.chetana.keystone.platform.admin.identity.Scope;
import com.chetana.keystone.web.QueryParams;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.PermissionCatalog.PLATFORM_PERMISSION;

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
            guard.requireRead(ctx, PLATFORM_PERMISSION);
            UUID tenantId = Ids.optionalUuid(ctx.queryParam("tenantId"));
            Scope scope = Scope.optional(ctx.queryParam("scope"));
            Access access = Access.optional(ctx.queryParam("access"));
            ctx.json(service.list(guard.callerScope(ctx), tenantId, scope, access, QueryParams.search(ctx),
                    QueryParams.page(ctx)));
        });

        routes.post("/api/v1/permissions", ctx -> {
            guard.requireWrite(ctx, PLATFORM_PERMISSION);
            PermissionRequest request = ctx.bodyAsClass(PermissionRequest.class);
            ctx.status(201).json(service.create(guard.callerScope(ctx), request));
        });

        routes.delete("/api/v1/permissions/{id}", ctx -> {
            guard.requireWrite(ctx, PLATFORM_PERMISSION);
            UUID id = Ids.uuid(ctx.pathParam("id"));
            service.delete(guard.callerScope(ctx), id);
            ctx.status(204);
        });
    }
}

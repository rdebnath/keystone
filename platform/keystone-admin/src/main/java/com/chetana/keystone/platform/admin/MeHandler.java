package com.chetana.keystone.platform.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.platform.admin.auth.PermissionGuard;
import com.chetana.keystone.platform.admin.identity.ChangePasswordRequest;
import com.chetana.keystone.platform.admin.identity.MeService;
import com.chetana.keystone.security.Principal;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

/**
 * REST routes for the authenticated caller's own profile.
 */
@Singleton
public final class MeHandler implements RouteConfigurer {

    private final MeService meService;
    private final PermissionGuard guard;

    @Inject
    public MeHandler(MeService meService, PermissionGuard guard) {
        this.meService = meService;
        this.guard = guard;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/me", ctx -> {
            Principal principal = guard.principal(ctx);
            ctx.json(meService.me(principal.subject()));
        });

        routes.post("/api/v1/me/password", ctx -> {
            Principal principal = guard.principal(ctx);
            var request = ctx.bodyAsClass(ChangePasswordRequest.class);
            meService.changePassword(principal.subject(), request);
            ctx.status(204);
        });

        routes.post("/api/v1/me/password-changed", ctx -> {
            Principal principal = guard.principal(ctx);
            meService.markPasswordChanged(principal.subject());
            ctx.status(204);
        });
    }
}

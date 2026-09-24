package com.chetana.keystone.platform.admin.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

/**
 * REST route for backend-proxied login.
 */
@Singleton
public final class AuthHandler implements RouteConfigurer {

    private final LoginService loginService;

    @Inject
    public AuthHandler(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.post("/api/v1/auth/login", ctx -> {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            ctx.json(loginService.login(request));
        });
    }
}

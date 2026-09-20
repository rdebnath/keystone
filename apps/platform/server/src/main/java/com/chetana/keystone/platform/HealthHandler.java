package com.chetana.keystone.platform;

import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

/**
 * Liveness probe, kept outside {@code /api/*} so it is not behind authentication.
 */
public final class HealthHandler implements RouteConfigurer {

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/healthz", ctx -> ctx.result("ok"));
    }
}

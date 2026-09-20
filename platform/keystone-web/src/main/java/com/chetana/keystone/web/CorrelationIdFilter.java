package com.chetana.keystone.web;

import io.javalin.config.RoutesConfig;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Propagates an {@code X-Correlation-Id} header across a request and into the log context so a
 * request can be traced end-to-end.
 */
public final class CorrelationIdFilter {

    public static final String HEADER = "X-Correlation-Id";

    private CorrelationIdFilter() {
    }

    public static void register(RoutesConfig routes) {
        routes.before(ctx -> {
            String id = ctx.header(HEADER);
            if (id == null || id.isBlank()) {
                id = UUID.randomUUID().toString();
            }
            ctx.attribute("correlationId", id);
            MDC.put("correlationId", id);
        });
        routes.after(ctx -> {
            ctx.header(HEADER, ctx.attribute("correlationId"));
            MDC.remove("correlationId");
        });
    }
}

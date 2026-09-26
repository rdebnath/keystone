package com.chetana.keystone.web;

import java.util.List;

/**
 * CORS policy applied to the embedded HTTP server via Javalin's bundled {@code CorsPlugin}.
 *
 * <p>An empty {@code allowedOrigins} list disables CORS (no {@code Access-Control-Allow-Origin}
 * header is emitted). The wildcard {@code "*"} allows any origin; any other entry is matched
 * exactly and reflected back to the requesting origin.
 */
public record CorsConfig(List<String> allowedOrigins) {

    public CorsConfig {
        allowedOrigins = List.copyOf(allowedOrigins);
    }

    /** CORS disabled — no CORS headers are emitted. */
    public static CorsConfig none() {
        return new CorsConfig(List.of());
    }

    public boolean enabled() {
        return !allowedOrigins.isEmpty();
    }
}

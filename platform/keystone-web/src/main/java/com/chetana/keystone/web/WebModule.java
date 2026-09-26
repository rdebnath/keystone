package com.chetana.keystone.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.chetana.keystone.common.error.KeystoneException;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;

import java.util.List;
import java.util.Set;

/**
 * Wires the web tier: a Jackson {@link ObjectMapper} and a {@link Javalin} instance with the
 * global RFC 9457 exception handler, correlation-id filter, and CORS policy installed, plus
 * every {@link RouteConfigurer} contributed by the application.
 */
public final class WebModule extends AbstractModule {

    private final CorsConfig corsConfig;
    private final String contextPath;

    public WebModule() {
        this(CorsConfig.none(), "/");
    }

    public WebModule(CorsConfig corsConfig) {
        this(corsConfig, "/");
    }

    public WebModule(CorsConfig corsConfig, String contextPath) {
        this.corsConfig = corsConfig;
        this.contextPath = contextPath;
    }

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), RouteConfigurer.class);
    }

    @Provides
    @Singleton
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Provides
    @Singleton
    Javalin javalin(ObjectMapper objectMapper, Set<RouteConfigurer> routeConfigurers, ProblemDetailMapper problemDetailMapper) {
        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
            config.routes.exception(KeystoneException.class, problemDetailMapper::handleKeystoneException);
            config.routes.exception(Exception.class, problemDetailMapper::handleUnexpectedException);
            applyContextPath(config);
            CorrelationIdFilter.register(config.routes);
            registerCors(config);
            routeConfigurers.forEach(rc -> rc.configure(config.routes));
        });
    }

    private void applyContextPath(JavalinConfig config) {
        if (contextPath != null && !contextPath.isBlank() && !contextPath.equals("/")) {
            config.router.contextPath = contextPath;
        }
    }

    private void registerCors(JavalinConfig config) {
        if (!corsConfig.enabled()) {
            return;
        }
        config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
            List<String> origins = corsConfig.allowedOrigins();
            if (origins.contains("*")) {
                rule.anyHost();
            } else {
                rule.allowHost(origins.getFirst(), origins.subList(1, origins.size()).toArray(String[]::new));
            }
        }));
    }
}

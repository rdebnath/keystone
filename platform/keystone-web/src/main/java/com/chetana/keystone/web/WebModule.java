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
import io.javalin.json.JavalinJackson;

import java.util.Set;

/**
 * Wires the web tier: a Jackson {@link ObjectMapper} and a {@link Javalin} instance with the
 * global RFC 9457 exception handler and correlation-id filter installed, plus every
 * {@link RouteConfigurer} contributed by the application.
 */
public final class WebModule extends AbstractModule {

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
            CorrelationIdFilter.register(config.routes);
            routeConfigurers.forEach(rc -> rc.configure(config.routes));
        });
    }
}

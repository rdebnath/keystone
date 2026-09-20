package com.chetana.keystone.web;

import io.javalin.config.RoutesConfig;

/**
 * A plugin-style hook for registering routes. Applications contribute one via a Guice
 * {@code Multibinder}; {@link WebModule} invokes all of them when the {@code Javalin} app is
 * built.
 */
@FunctionalInterface
public interface RouteConfigurer {

    void configure(RoutesConfig routes);
}

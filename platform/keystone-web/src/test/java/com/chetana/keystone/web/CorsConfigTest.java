package com.chetana.keystone.web;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void should_allow_any_origin_when_wildcard_is_configured() {
        Javalin app = app(new CorsConfig(List.of("*")));

        JavalinTest.test(app, (javalin, client) -> {
            var response = client.get("/ping", req -> req.header("Origin", "http://localhost:54321"));

            assertThat(response.code()).isEqualTo(200);
            assertThat(response.headers().get("Access-Control-Allow-Origin")).containsExactly("*");
        });
    }

    @Test
    void should_reflect_a_matching_allowed_origin() {
        Javalin app = app(new CorsConfig(List.of("http://localhost:3000")));

        JavalinTest.test(app, (javalin, client) -> {
            var response = client.get("/ping", req -> req.header("Origin", "http://localhost:3000"));

            assertThat(response.code()).isEqualTo(200);
            assertThat(response.headers().get("Access-Control-Allow-Origin")).containsExactly("http://localhost:3000");
        });
    }

    @Test
    void should_reject_a_non_matching_origin() {
        Javalin app = app(new CorsConfig(List.of("http://localhost:3000")));

        JavalinTest.test(app, (javalin, client) -> {
            var response = client.get("/ping", req -> req.header("Origin", "http://evil.example"));

            assertThat(response.code()).isEqualTo(400);
            assertThat(response.headers().get("Access-Control-Allow-Origin")).isNullOrEmpty();
        });
    }

    @Test
    void should_not_add_cors_headers_when_disabled() {
        Javalin app = app(CorsConfig.none());

        JavalinTest.test(app, (javalin, client) -> {
            var response = client.get("/ping", req -> req.header("Origin", "http://localhost:54321"));

            assertThat(response.code()).isEqualTo(200);
            assertThat(response.headers().get("Access-Control-Allow-Origin")).isNullOrEmpty();
        });
    }

    private static Javalin app(CorsConfig corsConfig) {
        return Guice.createInjector(new WebModule(corsConfig), new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder.newSetBinder(binder(), RouteConfigurer.class)
                        .addBinding()
                        .toInstance(routes -> routes.get("/ping", ctx -> ctx.result("pong")));
            }
        }).getInstance(Javalin.class);
    }
}

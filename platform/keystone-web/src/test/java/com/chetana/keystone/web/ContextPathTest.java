package com.chetana.keystone.web;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextPathTest {

    @Test
    void should_serve_routes_under_the_context_path() {
        Javalin app = app("/inventory");

        JavalinTest.test(app, (javalin, client) -> {
            var ok = client.get("/inventory/ping");

            assertThat(ok.code()).isEqualTo(200);
            assertThat(ok.body().string()).isEqualTo("pong");

            var outside = client.get("/ping");
            assertThat(outside.code()).isEqualTo(404);
        });
    }

    @Test
    void should_serve_at_root_when_no_context_path_is_set() {
        Javalin app = app("/");

        JavalinTest.test(app, (javalin, client) -> {
            var ok = client.get("/ping");

            assertThat(ok.code()).isEqualTo(200);
            assertThat(ok.body().string()).isEqualTo("pong");
        });
    }

    private static Javalin app(String contextPath) {
        return Guice.createInjector(new WebModule(CorsConfig.none(), contextPath), new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder.newSetBinder(binder(), RouteConfigurer.class)
                        .addBinding()
                        .toInstance(routes -> routes.get("/ping", ctx -> ctx.result("pong")));
            }
        }).getInstance(Javalin.class);
    }
}

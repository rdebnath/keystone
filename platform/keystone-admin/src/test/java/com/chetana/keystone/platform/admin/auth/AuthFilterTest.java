package com.chetana.keystone.platform.admin.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import com.chetana.keystone.security.Principal;
import com.chetana.keystone.web.CorsConfig;
import com.chetana.keystone.web.RouteConfigurer;
import com.chetana.keystone.web.WebModule;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The auth filter must stay out of the way of the login endpoint and of browser CORS preflight,
 * even when the app is served under a non-root context path (e.g. {@code /inventory}).
 */
class AuthFilterTest {

    @Test
    void should_exempt_login_path_when_running_under_a_context_path() {
        Javalin app = app();

        JavalinTest.test(app, (javalin, client) -> {
            var login = client.post("/inventory/api/v1/auth/login",
                    Map.of("identifier", "admin@keystone", "password", "changeit"));

            assertThat(login.code()).isEqualTo(200);
        });
    }

    @Test
    void should_allow_cors_preflight_options_to_protected_routes() throws Exception {
        Javalin app = app();

        JavalinTest.test(app, (javalin, client) -> {
            var preflight = options(javalin, "/inventory/api/v1/me");

            assertThat(preflight.statusCode()).isEqualTo(200);
            assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).contains("*");
        });
    }

    @Test
    void should_still_reject_protected_routes_without_a_token() {
        Javalin app = app();

        JavalinTest.test(app, (javalin, client) -> {
            var me = client.get("/inventory/api/v1/me");

            assertThat(me.code()).isEqualTo(403);
        });
    }

    /** Sends a raw CORS preflight request (the testtools client exposes no OPTIONS helper). */
    private static HttpResponse<String> options(Javalin app, String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + app.port() + path))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization")
                .build();
        return java.net.http.HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Javalin app() {
        return Guice.createInjector(
                new WebModule(new CorsConfig(List.of("*")), "/inventory"),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TokenAuthenticator.class)
                                .toInstance(token -> new Principal("sub", Map.of()));
                        Multibinder<RouteConfigurer> routes =
                                Multibinder.newSetBinder(binder(), RouteConfigurer.class);
                        routes.addBinding().to(AuthFilter.class);
                        routes.addBinding().toInstance(r -> {
                            r.post("/api/v1/auth/login", ctx -> ctx.result("ok"));
                            r.get("/api/v1/me", ctx -> ctx.result("me"));
                        });
                    }
                }).getInstance(Javalin.class);
    }
}

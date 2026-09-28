package com.chetana.keystone.web;

import com.chetana.keystone.common.query.PageRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The HTTP half of the list contract: the shared query parameters are parsed in one place, and an
 * unparsable one is a `422` rather than a silently ignored parameter.
 */
class QueryParamsTest {

    @Test
    void should_read_the_shared_list_parameters() {
        JavalinTest.test(app(), (javalin, client) -> {
            var response = client.get("/page?page=2&size=50&sort=createdAt&order=desc&q=%20acme%20");

            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("2|50|createdAt|DESC|acme");
        });
    }

    @Test
    void should_apply_the_defaults_when_nothing_is_asked_for() {
        JavalinTest.test(app(), (javalin, client) -> {
            assertThat(client.get("/page").body().string())
                    .isEqualTo("0|" + PageRequest.DEFAULT_SIZE + "|null|ASC|null");
        });
    }

    @Test
    void should_reject_an_unparsable_or_out_of_range_parameter() {
        JavalinTest.test(app(), (javalin, client) -> {
            assertThat(client.get("/page?page=two").code()).isEqualTo(422);
            assertThat(client.get("/page?page=-1").code()).isEqualTo(422);
            assertThat(client.get("/page?size=0").code()).isEqualTo(422);
            assertThat(client.get("/page?size=101").code()).isEqualTo(422);
            assertThat(client.get("/page?order=sideways").code()).isEqualTo(422);
            assertThat(client.get("/page?sort=1name").code()).isEqualTo(422);
            assertThat(client.get("/page?q=" + "a".repeat(101)).code()).isEqualTo(422);
        });
    }

    /** A route that renders what the helpers read, so the parsing is asserted without a service. */
    private static Javalin app() {
        return Guice.createInjector(new WebModule(CorsConfig.none(), "/"), new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder.newSetBinder(binder(), RouteConfigurer.class)
                        .addBinding()
                        .toInstance(routes -> routes.get("/page", ctx -> {
                            PageRequest page = QueryParams.page(ctx);
                            ctx.result(String.join("|",
                                    String.valueOf(page.page()),
                                    String.valueOf(page.size()),
                                    String.valueOf(page.sort()),
                                    page.order().name(),
                                    String.valueOf(QueryParams.search(ctx))));
                        }));
            }
        }).getInstance(Javalin.class);
    }
}

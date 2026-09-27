package com.chetana.keystone.web;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import com.chetana.keystone.common.error.NotFoundException;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailMapperTest {

    @Test
    void should_map_keystone_exception_to_problem_json() {
        Javalin app = Guice.createInjector(new WebModule(), new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder.newSetBinder(binder(), RouteConfigurer.class)
                        .addBinding()
                        .toInstance(routes -> routes.get("/boom", _ -> {
                            throw new NotFoundException("gone");
                        }));
            }
        }).getInstance(Javalin.class);

        JavalinTest.test(app, (_, client) -> {
            var response = client.get("/boom");

            assertThat(response.getCode()).isEqualTo(404);
            assertThat(response.getBody().string())
                    .contains("\"title\":\"NOT_FOUND\"")
                    .contains("\"detail\":\"gone\"");
        });
    }
}

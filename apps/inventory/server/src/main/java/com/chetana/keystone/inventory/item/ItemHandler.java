package com.chetana.keystone.inventory.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.web.RouteConfigurer;
import io.javalin.config.RoutesConfig;

/**
 * REST routes for the {@code items} resource, registered through the platform's
 * {@link RouteConfigurer} mechanism.
 */
@Singleton
public final class ItemHandler implements RouteConfigurer {

    private final ItemService service;

    @Inject
    public ItemHandler(ItemService service) {
        this.service = service;
    }

    @Override
    public void configure(RoutesConfig routes) {
        routes.get("/api/v1/items", ctx -> ctx.json(service.list()));
        routes.post("/api/v1/items", ctx -> {
            CreateItemRequest request = ctx.bodyAsClass(CreateItemRequest.class);
            ctx.status(201).json(service.create(request));
        });
    }
}

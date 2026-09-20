package com.chetana.keystone.inventory;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.id.UuidIdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.common.time.SystemDateTimeService;
import com.chetana.keystone.inventory.item.ItemHandler;
import com.chetana.keystone.inventory.item.ItemService;
import com.chetana.keystone.web.RouteConfigurer;

/**
 * Wires the inventory application: its services and its routes.
 */
public final class InventoryModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IdGenerator.class).to(UuidIdGenerator.class);
        bind(DateTimeService.class).to(SystemDateTimeService.class);
        bind(ItemService.class);

        Multibinder.newSetBinder(binder(), RouteConfigurer.class)
                .addBinding().to(ItemHandler.class);
    }
}

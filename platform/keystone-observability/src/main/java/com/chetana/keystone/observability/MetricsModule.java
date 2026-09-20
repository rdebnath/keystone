package com.chetana.keystone.observability;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Provides a Micrometer {@link MeterRegistry}. Applications can substitute a real registry
 * (Prometheus/OTLP) via {@code Modules.override}.
 */
public final class MetricsModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}

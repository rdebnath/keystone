package com.chetana.keystone.common.time;

import java.time.Clock;
import java.time.Instant;

/**
 * Production {@link DateTimeService} backed by a configurable {@link Clock}.
 */
public final class SystemDateTimeService implements DateTimeService {

    private final Clock clock;

    public SystemDateTimeService() {
        this(Clock.systemUTC());
    }

    public SystemDateTimeService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return Instant.now(clock);
    }
}

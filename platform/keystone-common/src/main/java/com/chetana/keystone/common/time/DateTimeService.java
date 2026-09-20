package com.chetana.keystone.common.time;

import java.time.Instant;

/**
 * Port for obtaining the current time.
 *
 * <p>All timestamps in the platform are produced through this service so that
 * time can be controlled (mocked) in tests instead of calling
 * {@code Instant.now()} directly.
 */
@FunctionalInterface
public interface DateTimeService {

    Instant now();
}

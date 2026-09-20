package com.chetana.keystone.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SystemDateTimeServiceTest {

    @Test
    void should_return_fixed_instant_when_constructed_with_fixed_clock() {
        var fixed = Instant.parse("2026-01-02T03:04:05Z");
        var service = new SystemDateTimeService(Clock.fixed(fixed, ZoneOffset.UTC));

        assertThat(service.now()).isEqualTo(fixed);
    }
}

package com.chetana.keystone.platform.admin;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformSchemaTest {

    @Test
    void should_reserve_the_all_zero_id_for_the_platform_tenant() {
        assertThat(PlatformSchema.PLATFORM_TENANT_ID).isEqualTo(new UUID(0, 0));
    }

    @Test
    void should_name_the_platform_tenant_keystone() {
        assertThat(PlatformSchema.PLATFORM_TENANT_NAME).isEqualTo("Keystone");
        assertThat(PlatformSchema.RESERVED_SLUG).isEqualTo("keystone");
    }

    @Test
    void should_recognize_only_the_reserved_id_as_the_platform_tenant() {
        // Real tenant ids are generated as random v4 UUIDs, so they cannot reach the reserved value.
        assertThat(PlatformSchema.isPlatformTenant(PlatformSchema.PLATFORM_TENANT_ID)).isTrue();
        assertThat(PlatformSchema.isPlatformTenant(UUID.randomUUID())).isFalse();
    }

    @Test
    void should_treat_a_missing_tenant_as_a_customer_tenant() {
        assertThat(PlatformSchema.isPlatformTenant(null)).isFalse();
    }
}

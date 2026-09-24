package com.chetana.keystone.platform.admin.tenant;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantSlugTest {

    @Test
    void should_lowercase_and_trim() {
        assertThat(TenantSlug.normalize("  Acme  ")).isEqualTo("acme");
    }

    @Test
    void should_accept_hyphenated_slug() {
        assertThat(TenantSlug.normalize("my-tenant")).isEqualTo("my-tenant");
    }

    @Test
    void should_reject_blank() {
        assertThatThrownBy(() -> TenantSlug.normalize("  "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_reject_invalid_characters() {
        assertThatThrownBy(() -> TenantSlug.normalize("my_tenant")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TenantSlug.normalize("my tenant")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TenantSlug.normalize("my.tenant")).isInstanceOf(ValidationException.class);
    }
}

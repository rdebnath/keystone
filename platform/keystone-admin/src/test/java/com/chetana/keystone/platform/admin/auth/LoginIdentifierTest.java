package com.chetana.keystone.platform.admin.auth;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginIdentifierTest {

    @Test
    void should_parse_username_and_tenant_slug() {
        LoginIdentifier identifier = LoginIdentifier.parse("alice@acme");

        assertThat(identifier.username()).isEqualTo("alice");
        assertThat(identifier.tenantSlug()).isEqualTo("acme");
    }

    @Test
    void should_lowercase_both_parts() {
        LoginIdentifier identifier = LoginIdentifier.parse("Alice@ACME");

        assertThat(identifier.username()).isEqualTo("alice");
        assertThat(identifier.tenantSlug()).isEqualTo("acme");
    }

    @Test
    void should_reject_blank_identifier() {
        assertThatThrownBy(() -> LoginIdentifier.parse("   "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_reject_identifier_without_at() {
        assertThatThrownBy(() -> LoginIdentifier.parse("alice"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_reject_at_at_start_or_end() {
        assertThatThrownBy(() -> LoginIdentifier.parse("@acme"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> LoginIdentifier.parse("alice@"))
                .isInstanceOf(ValidationException.class);
    }
}

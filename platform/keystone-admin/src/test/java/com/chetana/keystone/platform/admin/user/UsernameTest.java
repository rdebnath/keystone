package com.chetana.keystone.platform.admin.user;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameTest {

    @Test
    void should_lowercase_and_trim() {
        assertThat(Username.normalize("  Alice  ")).isEqualTo("alice");
    }

    @Test
    void should_reject_blank() {
        assertThatThrownBy(() -> Username.normalize("  "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_reject_at_sign() {
        assertThatThrownBy(() -> Username.normalize("alice@acme"))
                .isInstanceOf(ValidationException.class);
    }
}

package com.chetana.keystone.platform.admin.user;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void should_keep_a_canonical_e164_number_unchanged() {
        assertThat(PhoneNumber.normalize("+919876543210")).isEqualTo("+919876543210");
        assertThat(PhoneNumber.normalize("+14155552671")).isEqualTo("+14155552671");
    }

    @Test
    void should_strip_the_separators_a_human_types() {
        assertThat(PhoneNumber.normalize("+91 98765 43210")).isEqualTo("+919876543210");
        assertThat(PhoneNumber.normalize(" +91-98765-43210 ")).isEqualTo("+919876543210");
        assertThat(PhoneNumber.normalize("(+91) 98765.43210")).isEqualTo("+919876543210");
    }

    @Test
    void should_accept_the_length_boundaries_of_e164() {
        // E.164 allows at most 15 digits, and a dialable international number needs more than a few.
        assertThat(PhoneNumber.normalize("+1234567")).isEqualTo("+1234567");
        assertThat(PhoneNumber.normalize("+123456789012345")).isEqualTo("+123456789012345");
    }

    @Test
    void should_reject_a_value_that_is_not_e164() {
        assertThatThrownBy(() -> PhoneNumber.normalize("919876543210"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PhoneNumber.normalize("+0123456789"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PhoneNumber.normalize("+123456")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PhoneNumber.normalize("+1234567890123456"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PhoneNumber.normalize("phone")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PhoneNumber.normalize("+91 98765 4321a"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_reject_blank_from_the_strict_normalizer() {
        assertThatThrownBy(() -> PhoneNumber.normalize("   ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PhoneNumber.normalize(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void should_treat_an_absent_number_as_none_recorded() {
        // Optional: absent and blank both clear the number rather than rejecting the request.
        assertThat(PhoneNumber.normalizeOptional(null)).isNull();
        assertThat(PhoneNumber.normalizeOptional("")).isNull();
        assertThat(PhoneNumber.normalizeOptional("  ")).isNull();
        assertThat(PhoneNumber.normalizeOptional("+91 98765 43210")).isEqualTo("+919876543210");
    }
}

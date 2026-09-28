package com.chetana.keystone.platform.admin.tenant;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryTest {

    @Test
    void should_uppercase_and_trim_a_country_code() {
        assertThat(Country.normalize("  in ")).isEqualTo("IN");
        assertThat(Country.normalize("de")).isEqualTo("DE");
        assertThat(Country.normalize("US")).isEqualTo("US");
    }

    @Test
    void should_read_the_jdks_officially_assigned_iso_codes() {
        // The rule is the JDK's ISO 3166-1 alpha-2 list (PART1_ALPHA2 = officially assigned), not a list of
        // our own that could drift; pin a few members so removing the source of truth is a test failure.
        assertThat(Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2))
                .contains("IN", "DE", "US", "GB");
        assertThat(Country.isValid("IN")).isTrue();
    }

    @Test
    void should_reject_a_value_that_is_not_an_iso_3166_1_alpha_2_code() {
        assertThatThrownBy(() -> Country.normalize("IND")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Country.normalize("ZZ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Country.normalize("I1")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Country.normalize("India")).isInstanceOf(ValidationException.class);

        assertThat(Country.isValid("IND")).isFalse();
        assertThat(Country.isValid("ZZ")).isFalse();
        assertThat(Country.isValid(null)).isFalse();
    }

    @Test
    void should_reject_blank_from_the_strict_normalizer() {
        assertThatThrownBy(() -> Country.normalize("  ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Country.normalize(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void should_treat_an_absent_country_as_none_recorded() {
        // Optional: absent and blank both clear the value rather than rejecting the request.
        assertThat(Country.normalizeOptional(null)).isNull();
        assertThat(Country.normalizeOptional("")).isNull();
        assertThat(Country.normalizeOptional("   ")).isNull();
        assertThat(Country.normalizeOptional("in")).isEqualTo("IN");
    }
}

package com.chetana.keystone.platform.admin.identity;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The two access levels of a permission and the code suffixes that carry them.
 */
class AccessTest {

    @Test
    void should_expose_the_read_only_and_read_write_suffixes() {
        assertThat(Access.READ_ONLY.suffix()).isEqualTo("read-only");
        assertThat(Access.READ_WRITE.suffix()).isEqualTo("read-write");
    }

    @Test
    void should_list_exactly_the_two_level_suffixes() {
        assertThat(Access.suffixes()).containsExactly("read-only", "read-write");
    }

    @Test
    void should_parse_the_level_from_its_code_suffix() {
        // The suffix is what a caller filtering the catalog has: it is the last segment of every code.
        assertThat(Access.from("read-only")).isEqualTo(Access.READ_ONLY);
        assertThat(Access.from("READ-WRITE")).isEqualTo(Access.READ_WRITE);
        assertThat(Access.from("  read-write  ")).isEqualTo(Access.READ_WRITE);
    }

    @Test
    void should_refuse_a_level_that_is_not_one_of_the_two() {
        assertThatThrownBy(() -> Access.from("readwrite"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("access must be read-only or read-write");
        assertThatThrownBy(() -> Access.from("READ_ONLY"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Access.from(" "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void should_treat_an_absent_filter_as_no_filter() {
        assertThat(Access.optional(null)).isNull();
        assertThat(Access.optional("   ")).isNull();
        assertThat(Access.optional("read-only")).isEqualTo(Access.READ_ONLY);
    }
}

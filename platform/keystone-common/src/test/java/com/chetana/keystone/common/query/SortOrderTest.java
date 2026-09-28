package com.chetana.keystone.common.query;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The sort direction: parsed case-insensitively, defaulting to ascending, rejecting anything else. */
class SortOrderTest {

    @Test
    void should_parse_the_two_directions_case_insensitively() {
        assertThat(SortOrder.parse("asc")).isEqualTo(SortOrder.ASC);
        assertThat(SortOrder.parse("ASC")).isEqualTo(SortOrder.ASC);
        assertThat(SortOrder.parse(" desc ")).isEqualTo(SortOrder.DESC);
        assertThat(SortOrder.parse("Desc")).isEqualTo(SortOrder.DESC);
    }

    @Test
    void should_default_to_ascending_when_absent() {
        assertThat(SortOrder.parse(null)).isEqualTo(SortOrder.ASC);
        assertThat(SortOrder.parse("  ")).isEqualTo(SortOrder.ASC);
    }

    @Test
    void should_reject_anything_else_rather_than_ignore_it() {
        // A silently ignored direction returns rows in an order the caller did not ask for.
        assertThatThrownBy(() -> SortOrder.parse("sideways"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("order must be asc or desc");
    }

    @Test
    void should_report_the_wire_form() {
        assertThat(SortOrder.ASC.wire()).isEqualTo("asc");
        assertThat(SortOrder.DESC.wire()).isEqualTo("desc");
    }
}

package com.chetana.keystone.common.query;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The request half of the list contract (`docs/CODING_GUIDELINES_BACKEND.md` §8): the window a client asks
 * for is validated at the boundary, so a service can trust it and a malformed request is a `422` rather
 * than a database error.
 */
class PageRequestTest {

    @Test
    void should_default_to_the_first_page_without_a_sort_key() {
        PageRequest request = PageRequest.defaults();

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(PageRequest.DEFAULT_SIZE);
        assertThat(request.offset()).isZero();
        assertThat(request.sorted()).isFalse();
        assertThat(request.sort()).isNull();
        assertThat(request.order()).isEqualTo(SortOrder.ASC);
    }

    @Test
    void should_compute_the_offset_from_the_page_and_the_size() {
        assertThat(new PageRequest(3, 25, null, null).offset()).isEqualTo(75);
        assertThat(new PageRequest(0, 100, null, null).offset()).isZero();
    }

    @Test
    void should_treat_a_blank_sort_key_as_no_sort_key() {
        PageRequest request = new PageRequest(0, 25, "  ", null);

        assertThat(request.sort()).isNull();
        assertThat(request.sorted()).isFalse();
    }

    @Test
    void should_trim_a_sort_key_and_default_the_direction() {
        PageRequest request = new PageRequest(0, 25, "  createdAt ", null);

        assertThat(request.sort()).isEqualTo("createdAt");
        assertThat(request.sorted()).isTrue();
        assertThat(request.order()).isEqualTo(SortOrder.ASC);
    }

    @Test
    void should_reject_a_page_outside_the_allowed_range() {
        assertThatThrownBy(() -> new PageRequest(-1, 25, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("page must be between 0 and 10000");

        // The cap bounds the work one request can ask for: an unbounded offset is a cheap way to request an
        // expensive scan.
        assertThatThrownBy(() -> new PageRequest(PageRequest.MAX_PAGE + 1, 25, null, null))
                .isInstanceOf(ValidationException.class);
        assertThat(new PageRequest(PageRequest.MAX_PAGE, 25, null, null).page())
                .isEqualTo(PageRequest.MAX_PAGE);
    }

    @Test
    void should_reject_a_page_size_outside_the_allowed_range() {
        assertThatThrownBy(() -> new PageRequest(0, 0, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("size must be between 1 and 100");

        assertThatThrownBy(() -> new PageRequest(0, PageRequest.MAX_SIZE + 1, null, null))
                .isInstanceOf(ValidationException.class);
        assertThat(new PageRequest(0, PageRequest.MAX_SIZE, null, null).size())
                .isEqualTo(PageRequest.MAX_SIZE);
    }

    @Test
    void should_reject_a_sort_key_that_could_not_be_a_field_name() {
        // The whitelist of real keys belongs to the resource; this only keeps SQL syntax out of the query.
        assertThatThrownBy(() -> new PageRequest(0, 25, "name; drop table users", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sort must be a field name");

        assertThatThrownBy(() -> new PageRequest(0, 25, "1name", null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new PageRequest(0, 25, "a".repeat(33), null))
                .isInstanceOf(ValidationException.class);
    }
}

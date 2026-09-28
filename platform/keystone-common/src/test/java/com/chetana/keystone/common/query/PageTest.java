package com.chetana.keystone.common.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The envelope half of the list contract: the totals a client renders a pager from, computed in exactly
 * one place so the client never has to derive them.
 */
class PageTest {

    @Test
    void should_report_no_pages_when_nothing_matched() {
        Page<String> page = Page.of(List.of(), new PageRequest(0, 25, null, null), 0);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(Page.empty(new PageRequest(0, 25, null, null)).totalElements()).isZero();
    }

    @Test
    void should_report_a_single_page_when_the_total_fits() {
        Page<String> page = Page.of(List.of("a", "b"), new PageRequest(0, 25, null, null), 2);

        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void should_report_an_exact_multiple_without_an_extra_empty_page() {
        Page<String> first = Page.of(List.of("a"), new PageRequest(0, 10, null, null), 20);
        Page<String> last = Page.of(List.of("a"), new PageRequest(1, 10, null, null), 20);

        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.hasNext()).isTrue();
        assertThat(last.hasNext()).isFalse();
        assertThat(last.hasPrevious()).isTrue();
    }

    @Test
    void should_report_both_directions_in_the_middle_of_a_result_set() {
        Page<String> page = Page.of(List.of("a"), new PageRequest(2, 10, null, null), 100);

        assertThat(page.totalPages()).isEqualTo(10);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(10);
    }

    @Test
    void should_describe_a_page_past_the_end_without_treating_it_as_an_error() {
        // A stale page (rows deleted while the client was on it) is not a failure: it is an empty window
        // with the real totals, so the client can correct itself.
        Page<String> page = Page.of(List.of(), new PageRequest(9, 25, null, null), 3);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void should_keep_the_items_immutable() {
        List<String> items = new java.util.ArrayList<>(List.of("a"));
        Page<String> page = Page.of(items, new PageRequest(0, 25, null, null), 1);

        items.add("b");

        assertThat(page.items()).containsExactly("a");
    }
}

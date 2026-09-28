package com.chetana.keystone.platform.admin.tenant;

import com.chetana.keystone.common.query.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How a requested page maps onto the {@code tenants} table, given that the synthetic platform row is always
 * the first row of the list. Pure arithmetic, so every combination can be checked without a database.
 */
class PlatformRowWindowTest {

    @Test
    void should_window_a_plain_page_when_the_platform_row_is_not_in_the_list() {
        PlatformRowWindow window = PlatformRowWindow.of(page(2, 25), false, 100);

        assertThat(window.realOffset()).isEqualTo(50);
        assertThat(window.realLimit()).isEqualTo(25);
        assertThat(window.includesPlatformRow()).isFalse();
    }

    @Test
    void should_give_the_first_page_one_fewer_real_row_when_the_platform_row_is_pinned() {
        PlatformRowWindow window = PlatformRowWindow.of(page(0, 25), true, 100);

        assertThat(window.realOffset()).isZero();
        assertThat(window.realLimit()).isEqualTo(24);
        assertThat(window.includesPlatformRow()).isTrue();
    }

    @Test
    void should_shift_every_later_page_by_the_one_pinned_row() {
        // The logical list is [platform] + 100 rows, so page 2 starts at logical index 50 = real index 49 —
        // one earlier in the table.
        PlatformRowWindow window = PlatformRowWindow.of(page(2, 25), true, 100);

        assertThat(window.realOffset()).isEqualTo(49);
        assertThat(window.realLimit()).isEqualTo(25);
        assertThat(window.includesPlatformRow()).isFalse();
    }

    @Test
    void should_hold_only_the_platform_row_on_the_first_page_of_size_one() {
        PlatformRowWindow window = PlatformRowWindow.of(page(0, 1), true, 100);

        assertThat(window.realOffset()).isZero();
        assertThat(window.realLimit()).isZero();
        assertThat(window.includesPlatformRow()).isTrue();
    }

    @Test
    void should_fetch_nothing_from_a_page_past_the_end() {
        // 3 real rows + the pinned row = 4 logical rows, so page 1 (size 25) is past the end.
        PlatformRowWindow window = PlatformRowWindow.of(page(1, 25), true, 3);

        assertThat(window.realOffset()).isZero();
        assertThat(window.realLimit()).isZero();
        assertThat(window.includesPlatformRow()).isFalse();
    }

    @Test
    void should_keep_the_platform_row_on_the_only_page_of_a_short_list() {
        PlatformRowWindow window = PlatformRowWindow.of(page(0, 25), true, 3);

        assertThat(window.realOffset()).isZero();
        assertThat(window.realLimit()).isEqualTo(24);
        assertThat(window.includesPlatformRow()).isTrue();
    }

    @Test
    void should_cope_with_an_empty_table() {
        PlatformRowWindow window = PlatformRowWindow.of(page(0, 25), true, 0);

        assertThat(window.realOffset()).isZero();
        assertThat(window.realLimit()).isEqualTo(24);
        assertThat(window.includesPlatformRow()).isTrue();

        PlatformRowWindow past = PlatformRowWindow.of(page(1, 25), true, 0);
        assertThat(past.realLimit()).isZero();
        assertThat(past.includesPlatformRow()).isFalse();
    }

    private static PageRequest page(int page, int size) {
        return new PageRequest(page, size, null, null);
    }
}

package com.chetana.keystone.common.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** The picker envelope: the complete set, capped, with the cap reported instead of hidden. */
class OptionListTest {

    @Test
    void should_return_a_short_list_unchanged() {
        OptionList<String> options = OptionList.of(List.of("a", "b"));

        assertThat(options.items()).containsExactly("a", "b");
        assertThat(options.truncated()).isFalse();
    }

    @Test
    void should_report_truncation_when_the_probe_row_is_present() {
        // The query fetches MAX_OPTIONS + 1 rows: the extra row is the signal, and it is never returned.
        List<String> rows = IntStream.rangeClosed(0, OptionList.MAX_OPTIONS)
                .mapToObj(index -> "row-" + index)
                .toList();

        OptionList<String> options = OptionList.of(rows);

        assertThat(options.items()).hasSize(OptionList.MAX_OPTIONS);
        assertThat(options.truncated()).isTrue();
        assertThat(options.items()).doesNotContain("row-" + OptionList.MAX_OPTIONS);
    }

    @Test
    void should_not_report_truncation_at_the_cap() {
        List<String> rows = IntStream.range(0, OptionList.MAX_OPTIONS)
                .mapToObj(index -> "row-" + index)
                .toList();

        OptionList<String> options = OptionList.of(rows);

        assertThat(options.items()).hasSize(OptionList.MAX_OPTIONS);
        assertThat(options.truncated()).isFalse();
    }

    @Test
    void should_keep_the_items_immutable() {
        List<String> rows = new java.util.ArrayList<>(List.of("a"));
        OptionList<String> options = OptionList.of(rows);

        rows.add("b");

        assertThat(options.items()).containsExactly("a");
    }
}

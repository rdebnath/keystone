package com.chetana.keystone.common.query;

import com.chetana.keystone.common.error.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** What a search term is: the one place that decides, so every resource treats `null`/`""`/`"  x  "` alike. */
class SearchTermTest {

    @Test
    void should_treat_absent_and_blank_terms_as_no_search() {
        assertThat(SearchTerm.normalize(null)).isNull();
        assertThat(SearchTerm.normalize("")).isNull();
        assertThat(SearchTerm.normalize("   ")).isNull();
    }

    @Test
    void should_trim_a_real_term() {
        assertThat(SearchTerm.normalize("  acme  ")).isEqualTo("acme");
    }

    @Test
    void should_accept_a_term_at_the_length_limit() {
        String longest = "a".repeat(SearchTerm.MAX_LENGTH);

        assertThat(SearchTerm.normalize(longest)).isEqualTo(longest);
    }

    @Test
    void should_reject_a_term_beyond_the_length_limit() {
        // An unbounded pattern is an unbounded amount of work for the database; this bounds it.
        assertThatThrownBy(() -> SearchTerm.normalize("a".repeat(SearchTerm.MAX_LENGTH + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("q must be at most 100 characters");
    }
}

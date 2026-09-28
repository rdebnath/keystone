package com.chetana.keystone.data;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Server-side search, asserted on the SQL it produces — which is the point of doing it in the database:
 * with a paged list the client holds one page, so a search applied to it would only ever search that page.
 */
class SearchTest {

    // Not named `DSL`: a field with that name would shadow the `org.jooq.impl.DSL` class it is built from.
    private static final DSLContext CTX = DSL.using(SQLDialect.POSTGRES);
    private static final Field<String> NAME = DSL.field("name", String.class);
    private static final Field<String> EMAIL = DSL.field("email", String.class);

    @Test
    void should_add_no_condition_when_there_is_no_term() {
        // "No search" must be an identity for AND, so a service can always combine it in.
        assertThat(rendered(Search.containsIgnoreCase(null, NAME))).doesNotContainIgnoringCase("like");
        assertThat(rendered(Search.containsIgnoreCase("   ", NAME))).doesNotContainIgnoringCase("like");
    }

    @Test
    void should_match_a_term_anywhere_in_the_field_case_insensitively() {
        String sql = rendered(Search.containsIgnoreCase("acme", NAME));

        assertThat(sql).contains("'%acme%'");
        // PostgreSQL renders the case-insensitive match as ILIKE, with the escape character declared —
        // without the explicit ESCAPE, the escaped wildcards below would not be literal.
        assertThat(sql).containsIgnoringCase("ilike");
        assertThat(sql).contains("escape '\\'");
    }

    @Test
    void should_or_the_searchable_fields_together() {
        String sql = rendered(Search.containsIgnoreCase("acme", NAME, EMAIL));

        assertThat(sql).contains("name");
        assertThat(sql).contains("email");
        assertThat(sql).containsIgnoringCase(" or ");
    }

    @Test
    void should_escape_like_wildcards_so_a_search_stays_a_search() {
        // Searching for `%` finds rows containing `%`, never every row.
        assertThat(rendered(Search.containsIgnoreCase("100%", NAME))).contains("'%100\\%%'");
        assertThat(rendered(Search.containsIgnoreCase("a_b", NAME))).contains("'%a\\_b%'");
        // The escape character itself must survive as a literal.
        assertThat(rendered(Search.containsIgnoreCase("c:\\tmp", NAME)))
                .contains("'%c:\\\\tmp%'");
    }

    @Test
    void should_trim_the_term_before_matching() {
        assertThat(rendered(Search.containsIgnoreCase("  acme  ", NAME))).contains("'%acme%'");
    }

    @Test
    void should_match_the_in_memory_twin_the_same_way() {
        // `matches` is for the one row that is not in the database — the synthetic platform tenant, which is
        // a row of the list and must be searched like one — so it mirrors the SQL rule exactly.
        assertThat(Search.matches(null, "Keystone")).isTrue();
        assertThat(Search.matches("   ", "Keystone")).isTrue();
        assertThat(Search.matches("key", "Keystone")).isTrue();
        assertThat(Search.matches("KEY", "Keystone")).isTrue();
        assertThat(Search.matches("acme", "Keystone")).isFalse();
        assertThat(Search.matches("acme", null, "Acme Corp")).isTrue();
        assertThat(Search.matches("acme")).isFalse();
    }

    private static String rendered(Condition condition) {
        return CTX.renderInlined(condition);
    }
}

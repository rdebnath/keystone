package com.chetana.keystone.data;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Locale;
import java.util.stream.Stream;

/**
 * Server-side search as a jOOQ {@link Condition}: a case-insensitive <em>contains</em> over the fields a
 * resource documents as searchable.
 *
 * <p>Search has to happen in SQL, not in the client: with a paged list the client holds one page, so a
 * search applied to it would only ever search that page. This is the one place that decides what a search
 * term means in SQL, so all resources behave the same:
 *
 * <ul>
 *   <li>an absent/blank term is {@link DSL#noCondition()}, so a service can always {@code AND} the result
 *       in without branching;</li>
 *   <li>the term is a <strong>bound parameter</strong> — never string-concatenated — so it cannot change
 *       the statement's meaning;</li>
 *   <li>the LIKE metacharacters {@code %}, {@code _} and {@code \} are escaped with an explicit
 *       {@code ESCAPE} character, so a user searching for {@code %} finds rows containing {@code %},
 *       not every row.</li>
 * </ul>
 */
public final class Search {

    private static final char ESCAPE = '\\';

    private Search() {
    }

    /**
     * A condition matching {@code term} anywhere in any of {@code fields}, case-insensitively — the
     * fields are OR'ed, which is what a search box over "name, slug or country" means.
     */
    @SafeVarargs
    public static Condition containsIgnoreCase(String term, Field<?>... fields) {
        if (term == null || term.isBlank()) {
            return DSL.noCondition();
        }
        String pattern = "%" + escape(term.trim()) + "%";
        return Stream.of(fields)
                .map(field -> field.likeIgnoreCase(pattern, ESCAPE))
                .reduce(Condition::or)
                .orElseGet(DSL::noCondition);
    }

    /**
     * Whether {@code term} matches any of {@code values} — the in-memory twin of
     * {@link #containsIgnoreCase}, for the rows this class cannot see: the synthetic platform tenant is
     * not in the database, but it is a row of the list and must be searched like one.
     */
    public static boolean matches(String term, String... values) {
        if (term == null || term.isBlank()) {
            return true;
        }
        String needle = term.trim().toLowerCase(Locale.ROOT);
        return Stream.of(values)
                .anyMatch(value -> value != null && value.toLowerCase(Locale.ROOT).contains(needle));
    }

    /** Prefixes each LIKE metacharacter with the escape character, leaving everything else literal. */
    private static String escape(String term) {
        StringBuilder escaped = new StringBuilder(term.length() + 8);
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == ESCAPE || c == '%' || c == '_') {
                escaped.append(ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}

package com.chetana.keystone.common.query;

import com.chetana.keystone.common.error.ValidationException;

/**
 * The search term of a list request ({@code q}): the one place that decides what a term is, so every
 * resource and every plane treats {@code null}, {@code ""} and {@code "  acme  "} identically.
 *
 * <p>Search is a <em>case-insensitive contains</em> over a resource's documented columns — not a query
 * language — so a term is only ever data. It is bounded in length because an unbounded pattern is an
 * unbounded amount of work for the database, and an absent/blank term means "no search" rather than
 * "match the empty string".
 */
public final class SearchTerm {

    /** The longest accepted term; longer is a {@code 422} rather than a silently truncated search. */
    public static final int MAX_LENGTH = 100;

    private SearchTerm() {
    }

    /**
     * Normalizes a raw query parameter: {@code null} stays {@code null}, a blank or whitespace-only value
     * becomes {@code null} (no search), and anything longer than {@link #MAX_LENGTH} is rejected.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String term = raw.trim();
        if (term.isEmpty()) {
            return null;
        }
        if (term.length() > MAX_LENGTH) {
            throw new ValidationException("q must be at most " + MAX_LENGTH + " characters");
        }
        return term;
    }
}

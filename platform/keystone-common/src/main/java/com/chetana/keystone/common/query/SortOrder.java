package com.chetana.keystone.common.query;

import com.chetana.keystone.common.error.ValidationException;

import java.util.Locale;

/**
 * The direction a paged list is ordered in.
 *
 * <p>Parsed case-insensitively from the wire ({@code asc} / {@code desc}), because a query parameter is
 * written by hand, a client or a bookmark; an absent value is {@link #ASC}, which is the direction every
 * Keystone list defaults to. Anything else is a {@link ValidationException} — a {@code 422} — rather than
 * a silently ignored parameter, so a typo does not return the rows in an unexpected order.
 */
public enum SortOrder {

    ASC,
    DESC;

    /** Parses {@code asc}/{@code desc} case-insensitively; absent or blank is {@link #ASC}. */
    public static SortOrder parse(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> ASC;
            case "desc" -> DESC;
            default -> throw new ValidationException("order must be asc or desc: " + value);
        };
    }

    /** The wire form, for building a follow-up request or describing the query in a log line. */
    public String wire() {
        return name().toLowerCase(Locale.ROOT);
    }
}

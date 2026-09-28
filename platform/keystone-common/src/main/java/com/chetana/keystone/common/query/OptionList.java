package com.chetana.keystone.common.query;

import java.util.List;

/**
 * The complete set of choices for a picker — a tenant/owner dropdown, a role checklist — as opposed to
 * {@link Page}, which serves the rows of a list screen.
 *
 * <p>A picker cannot be fed by a page (it would silently offer only the first page), so a resource that
 * has one exposes an unpaged {@code …/options} route. "Unpaged" still needs a ceiling: this envelope
 * reports {@link #truncated()} instead of quietly dropping rows, so the client can say what it is
 * showing.
 *
 * <p>To detect truncation, the query fetches {@link #MAX_OPTIONS} + 1 rows and hands them to
 * {@link #of(List)}: the extra row is the signal, and it is never returned.
 */
public record OptionList<T>(List<T> items, boolean truncated) {

    /** The most options a picker response carries, however many rows exist. */
    public static final int MAX_OPTIONS = 500;

    public OptionList {
        items = List.copyOf(items);
    }

    /** Wraps up to {@link #MAX_OPTIONS} rows, reporting whether the source had more than that. */
    public static <T> OptionList<T> of(List<T> items) {
        if (items.size() <= MAX_OPTIONS) {
            return new OptionList<>(items, false);
        }
        return new OptionList<>(items.subList(0, MAX_OPTIONS), true);
    }
}

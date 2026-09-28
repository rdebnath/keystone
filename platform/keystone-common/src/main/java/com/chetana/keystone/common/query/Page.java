package com.chetana.keystone.common.query;

import java.util.List;

/**
 * One page of a list: the rows, the window they were taken from, and the totals a client needs to render
 * a pager without guessing.
 *
 * <p>The totals are <strong>components, not derived accessors</strong>: the envelope Jackson writes is the
 * whole contract, so a client reads {@code totalPages} instead of recomputing it and disagreeing with the
 * server about which page is last. {@link #of(List, PageRequest, long)} is the only place that does the
 * arithmetic — a service fetches the window and its count, and never builds these fields by hand.
 */
public record Page<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    public Page {
        items = List.copyOf(items);
    }

    /**
     * Builds the envelope for {@code request} from one fetched window and the total number of rows that
     * matched the same condition. A page past the end is <em>not</em> an error: it yields empty
     * {@code items} with the real totals, so a client on a stale page can correct itself.
     */
    public static <T> Page<T> of(List<T> items, PageRequest request, long totalElements) {
        int totalPages = totalPages(totalElements, request.size());
        return new Page<>(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages,
                request.page() + 1 < totalPages,
                request.page() > 0);
    }

    /** The empty page of a request: what a list answers when nothing matched at all. */
    public static <T> Page<T> empty(PageRequest request) {
        return of(List.of(), request, 0);
    }

    /** Ceiling division of the total by the page size; no rows means no pages. */
    private static int totalPages(long totalElements, int size) {
        return (int) ((totalElements + size - 1) / size);
    }
}

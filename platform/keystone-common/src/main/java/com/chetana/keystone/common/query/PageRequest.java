package com.chetana.keystone.common.query;

import com.chetana.keystone.common.error.ValidationException;

import java.util.regex.Pattern;

/**
 * The window a list request asks for: which page, how big, sorted by which key in which direction.
 *
 * <p>Every list route takes the same four parameters ({@code page}, {@code size}, {@code sort},
 * {@code order}) and every one of them is validated <em>here</em>, at the boundary, so a service can
 * trust its input and a malformed request is a {@code 422} rather than a database error:
 *
 * <ul>
 *   <li>{@code page} is 0-based and capped ({@link #MAX_PAGE}) — an unbounded offset is a cheap way to
 *       ask PostgreSQL for an expensive scan;</li>
 *   <li>{@code size} is capped ({@link #MAX_SIZE}) for the same reason, and defaults to
 *       {@link #DEFAULT_SIZE};</li>
 *   <li>{@code sort} is only <em>shaped</em> here (a field name) — which keys actually exist is the
 *       resource's business, so the service rejects an unknown key and names the ones it accepts;</li>
 *   <li>an absent {@code sort} means "the resource's default order" and an absent {@code order} is
 *       {@link SortOrder#ASC}.</li>
 * </ul>
 */
public record PageRequest(int page, int size, String sort, SortOrder order) {

    /** The page size a list uses when the caller does not ask for one. */
    public static final int DEFAULT_SIZE = 25;

    /** The largest page a caller may ask for; the cap bounds the work one request can request. */
    public static final int MAX_SIZE = 100;

    /** The largest page number a caller may ask for; beyond this the offset cost is not worth serving. */
    public static final int MAX_PAGE = 10_000;

    private static final int MAX_SORT_LENGTH = 32;
    private static final Pattern SORT_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9]*");

    public PageRequest {
        if (page < 0 || page > MAX_PAGE) {
            throw new ValidationException("page must be between 0 and " + MAX_PAGE + ": " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ValidationException("size must be between 1 and " + MAX_SIZE + ": " + size);
        }
        sort = normalizeSort(sort);
        order = order == null ? SortOrder.ASC : order;
    }

    /** The first page of the default size with no sort key — i.e. the resource's own default order. */
    public static PageRequest defaults() {
        return new PageRequest(0, DEFAULT_SIZE, null, SortOrder.ASC);
    }

    /** The SQL {@code OFFSET} this page starts at ({@code page * size}; safe as an int under the caps). */
    public int offset() {
        return page * size;
    }

    /** Whether the caller named a sort key; when false the resource's default order applies. */
    public boolean sorted() {
        return sort != null;
    }

    /** A blank key is "no sort"; a key that could not be a column name is rejected before it reaches SQL. */
    private static String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        String key = sort.trim();
        if (key.length() > MAX_SORT_LENGTH || !SORT_KEY.matcher(key).matches()) {
            throw new ValidationException("sort must be a field name: " + sort);
        }
        return key;
    }
}

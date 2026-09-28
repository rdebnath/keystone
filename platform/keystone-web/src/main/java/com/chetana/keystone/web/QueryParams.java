package com.chetana.keystone.web;

import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.query.PageRequest;
import com.chetana.keystone.common.query.SearchTerm;
import com.chetana.keystone.common.query.SortOrder;
import io.javalin.http.Context;

/**
 * Reads the shared list query parameters, so every list handler parses them the same way and in one line:
 *
 * <pre>{@code
 * ctx.json(service.list(caller, filter, QueryParams.search(ctx), QueryParams.page(ctx)));
 * }</pre>
 *
 * <p>HTTP parsing stays in the web layer: the values become the framework-free vocabulary of
 * {@code com.chetana.keystone.common.query}, which is what a service takes. An unparsable value is a
 * {@link ValidationException} ({@code 422}) — a query parameter is input like any other, and it is never
 * ignored because it was inconvenient to validate.
 */
public final class QueryParams {

    private QueryParams() {
    }

    /** {@code page}, {@code size}, {@code sort}, {@code order} → a validated {@link PageRequest}. */
    public static PageRequest page(Context ctx) {
        return new PageRequest(
                integer(ctx, "page", 0),
                integer(ctx, "size", PageRequest.DEFAULT_SIZE),
                ctx.queryParam("sort"),
                order(ctx, SortOrder.ASC));
    }

    /** The {@code q} search term, normalized ({@code null} means "no search"). */
    public static String search(Context ctx) {
        return SearchTerm.normalize(ctx.queryParam("q"));
    }

    /** The {@code order} parameter, or {@code fallback} when it is absent. */
    public static SortOrder order(Context ctx, SortOrder fallback) {
        String raw = ctx.queryParam("order");
        return raw == null || raw.isBlank() ? fallback : SortOrder.parse(raw);
    }

    /** A whole-number parameter, or {@code fallback} when absent; a non-number is a {@code 422}. */
    private static int integer(Context ctx, String name, int fallback) {
        String raw = ctx.queryParam(name);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " must be a whole number: " + raw);
        }
    }
}

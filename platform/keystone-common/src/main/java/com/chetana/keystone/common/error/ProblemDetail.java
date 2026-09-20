package com.chetana.keystone.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.util.Map;

/**
 * An RFC 9457 {@code application/problem+json} error body.
 *
 * <p>Fields that are absent are omitted from serialization; {@code errors} carries
 * field-level validation failures keyed by field name.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        URI type,
        String title,
        int status,
        String detail,
        URI instance,
        Map<String, String> errors) {

    public static ProblemDetail of(int status, String title, String detail) {
        return new ProblemDetail(null, title, status, detail, null, null);
    }

    public ProblemDetail withErrors(Map<String, String> errors) {
        return new ProblemDetail(type, title, status, detail, instance, Map.copyOf(errors));
    }
}

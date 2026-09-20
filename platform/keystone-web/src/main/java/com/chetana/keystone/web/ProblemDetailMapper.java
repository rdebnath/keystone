package com.chetana.keystone.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ErrorCode;
import com.chetana.keystone.common.error.KeystoneException;
import com.chetana.keystone.common.error.ProblemDetail;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps {@link KeystoneException}s and unexpected exceptions to an RFC 9457
 * {@code application/problem+json} response without leaking internals.
 */
@Singleton
public final class ProblemDetailMapper {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailMapper.class);

    private final ObjectMapper objectMapper;

    @Inject
    public ProblemDetailMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handleKeystoneException(KeystoneException e, Context ctx) {
        int status = statusOf(e.code());
        write(ctx, status, ProblemDetail.of(status, e.code().name(), e.getMessage()));
    }

    public void handleUnexpectedException(Exception e, Context ctx) {
        log.error("Unhandled exception", e);
        write(ctx, 500, ProblemDetail.of(500, "INTERNAL", "An unexpected error occurred."));
    }

    private void write(Context ctx, int status, ProblemDetail problem) {
        ctx.status(status);
        ctx.contentType("application/problem+json");
        ctx.result(writeJson(problem));
    }

    private String writeJson(ProblemDetail problem) {
        try {
            return objectMapper.writeValueAsString(problem);
        } catch (Exception e) {
            return "{\"title\":\"INTERNAL\",\"status\":500}";
        }
    }

    private static int statusOf(ErrorCode code) {
        return switch (code) {
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case VALIDATION -> 422;
            case ACCESS_DENIED -> 403;
            case INTERNAL -> 500;
        };
    }
}

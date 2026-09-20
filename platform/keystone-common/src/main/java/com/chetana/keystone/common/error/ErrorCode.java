package com.chetana.keystone.common.error;

/**
 * Stable, machine-readable error codes for the Keystone platform.
 *
 * <p>New codes are added here rather than scattered as string literals so the
 * API layer can map them to a {@code ProblemDetail} (RFC 9457) consistently.
 */
public enum ErrorCode {
    NOT_FOUND,
    CONFLICT,
    VALIDATION,
    ACCESS_DENIED,
    INTERNAL
}

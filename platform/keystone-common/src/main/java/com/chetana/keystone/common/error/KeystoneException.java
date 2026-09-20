package com.chetana.keystone.common.error;

/**
 * Base unchecked exception for the Keystone platform.
 *
 * <p>Every business failure carries a stable {@link ErrorCode} so the API layer
 * can map it to a {@code ProblemDetail} (RFC 9457) without leaking internals.
 */
public abstract class KeystoneException extends RuntimeException {

    private final ErrorCode code;

    protected KeystoneException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    protected KeystoneException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}

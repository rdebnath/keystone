package com.chetana.keystone.common.error;

public final class ValidationException extends KeystoneException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION, message);
    }
}

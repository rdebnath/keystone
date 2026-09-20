package com.chetana.keystone.common.error;

public final class ConflictException extends KeystoneException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}

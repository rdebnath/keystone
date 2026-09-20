package com.chetana.keystone.common.error;

public final class NotFoundException extends KeystoneException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}

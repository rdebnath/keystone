package com.chetana.keystone.common.error;

public final class AccessDeniedException extends KeystoneException {

    public AccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}

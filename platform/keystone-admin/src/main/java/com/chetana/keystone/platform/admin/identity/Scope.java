package com.chetana.keystone.platform.admin.identity;

import com.chetana.keystone.common.error.ValidationException;

import java.util.Locale;

/**
 * Authorization scope of a role or permission: {@code PLATFORM} (cross-tenant) or
 * {@code TENANT} (within one customer).
 */
public enum Scope {

    PLATFORM,
    TENANT;

    public static Scope from(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("scope must not be blank");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("scope must be PLATFORM or TENANT");
        }
    }
}

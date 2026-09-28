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

    /**
     * The {@code scope} filter of a list request: an absent value means every scope the caller can see.
     * Parsed like {@code Ids.optionalUuid} — absent is not an error, a present-but-invalid value is.
     */
    public static Scope optional(String value) {
        return value == null || value.isBlank() ? null : from(value);
    }
}

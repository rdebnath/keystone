package com.chetana.keystone.platform.admin;

import com.chetana.keystone.common.error.ValidationException;

import java.util.UUID;

/**
 * Parses path parameters into UUIDs, rejecting malformed input as a validation error rather than
 * a 500.
 */
public final class Ids {

    private Ids() {
    }

    public static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid id: " + value);
        }
    }

    /** An absent query parameter means "no filter"; a present one must still parse. */
    public static UUID optionalUuid(String value) {
        return value == null || value.isBlank() ? null : uuid(value);
    }
}

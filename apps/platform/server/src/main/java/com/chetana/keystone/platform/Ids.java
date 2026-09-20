package com.chetana.keystone.platform;

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
}

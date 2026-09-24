package com.chetana.keystone.platform.admin.user;

import com.chetana.keystone.common.error.ValidationException;

import java.util.Locale;

/**
 * A login username — the local-part of {@code username@tenantid}.
 */
public final class Username {

    private Username() {
    }

    public static String normalize(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("username must not be blank");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("@")) {
            throw new ValidationException("username must not contain '@'");
        }
        return normalized;
    }
}

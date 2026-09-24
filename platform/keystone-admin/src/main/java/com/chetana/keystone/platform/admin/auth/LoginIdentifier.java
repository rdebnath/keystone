package com.chetana.keystone.platform.admin.auth;

import com.chetana.keystone.common.error.ValidationException;

import java.util.Locale;

/**
 * A parsed login identifier in {@code username@tenantid} form. Both parts are normalized to
 * lowercase.
 */
public record LoginIdentifier(String username, String tenantSlug) {

    public static LoginIdentifier parse(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ValidationException("identifier must not be blank");
        }
        int at = identifier.lastIndexOf('@');
        if (at <= 0 || at == identifier.length() - 1) {
            throw new ValidationException("identifier must be username@tenantid");
        }
        return new LoginIdentifier(
                identifier.substring(0, at).toLowerCase(Locale.ROOT),
                identifier.substring(at + 1).toLowerCase(Locale.ROOT));
    }
}

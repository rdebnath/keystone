package com.chetana.keystone.platform.admin.user;

import java.util.Locale;

/**
 * Derives the virtual/fake Supabase email from a username + tenant slug. The email is an
 * identifier only and is never delivered.
 */
public final class Emails {

    private Emails() {
    }

    public static String derive(String username, String tenantSlug, String providedEmail) {
        if (providedEmail != null && !providedEmail.isBlank()) {
            return providedEmail.trim().toLowerCase(Locale.ROOT);
        }
        return username + "@" + tenantSlug + ".com";
    }
}

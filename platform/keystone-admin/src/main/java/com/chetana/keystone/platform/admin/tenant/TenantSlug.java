package com.chetana.keystone.platform.admin.tenant;

import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.platform.admin.PlatformSchema;

import java.util.Locale;

/**
 * A tenant slug — the {@code tenantid} part of {@code username@tenantid}. DNS-safe and unique.
 */
public final class TenantSlug {

    private TenantSlug() {
    }

    public static String normalize(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("slug must not be blank");
        }
        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9-]+")) {
            throw new ValidationException("slug must contain only lowercase letters, digits, and hyphens");
        }
        return normalized;
    }

    /**
     * Whether {@code slug} is the reserved platform slug. Such a tenant would be unreachable: the login
     * resolver reads it as the platform plane, not as a customer tenant.
     */
    public static boolean isReserved(String slug) {
        return PlatformSchema.RESERVED_SLUG.equals(slug);
    }
}

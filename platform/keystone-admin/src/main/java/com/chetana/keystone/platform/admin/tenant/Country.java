package com.chetana.keystone.platform.admin.tenant;

import com.chetana.keystone.common.error.ValidationException;

import java.util.Locale;
import java.util.Set;

/**
 * A tenant's country, as an ISO 3166-1 alpha-2 code ({@code IN}, {@code DE}, {@code US}).
 *
 * <p>Optional: {@link #normalizeOptional} answers {@code null} for a blank value, which is how a tenant
 * that has not recorded a country is stored. The accepted set is the JDK's officially assigned codes
 * ({@link Locale.IsoCountryCode#PART1_ALPHA2}), so the rule lives in one place and no database
 * constraint has to be migrated when the list moves.
 */
public final class Country {

    private Country() {
    }

    /**
     * The stored form of an optional country: {@code null} when none is recorded, otherwise the
     * canonical upper-case code.
     */
    public static String normalizeOptional(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return normalize(country);
    }

    /**
     * Normalizes and validates a country: trimmed and upper-cased, so {@code "in"} and {@code " IN "}
     * both store {@code IN}.
     */
    public static String normalize(String country) {
        if (country == null || country.isBlank()) {
            throw new ValidationException("country must not be blank");
        }
        String normalized = country.trim().toUpperCase(Locale.ROOT);
        if (!isValid(normalized)) {
            throw new ValidationException("country must be an ISO 3166-1 alpha-2 code, e.g. IN: " + normalized);
        }
        return normalized;
    }

    /** Whether {@code country} is a canonical ISO 3166-1 alpha-2 code. */
    public static boolean isValid(String country) {
        return country != null
                && Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2).contains(country);
    }
}

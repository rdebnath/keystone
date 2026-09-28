package com.chetana.keystone.platform.admin.user;

import com.chetana.keystone.common.error.ValidationException;

import java.util.regex.Pattern;

/**
 * A user's phone number in E.164 form ({@code +919876543210}): a leading {@code +}, then the country
 * and subscriber digits — at most 15 digits in total, the E.164 maximum, and never a leading zero.
 *
 * <p>Optional: {@link #normalizeOptional} answers {@code null} for a blank value. Numbers are stored in
 * one canonical form so a later consumer (an SMS or dialling integration) needs no parsing, and the
 * separators people type — spaces, hyphens, dots and parentheses — are removed rather than rejected.
 */
public final class PhoneNumber {

    /** E.164: '+', a non-zero leading digit, then digits up to a total of 15. */
    private static final Pattern E164 = Pattern.compile("\\+[1-9]\\d{6,14}");

    /** The separators a human types, stripped before validation. */
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-.()]");

    private PhoneNumber() {
    }

    /**
     * The stored form of an optional phone number: {@code null} when none is recorded, otherwise the
     * canonical E.164 form.
     */
    public static String normalizeOptional(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        return normalize(phoneNumber);
    }

    /** Normalizes and validates a phone number, e.g. {@code "+91 98765 43210"} to {@code "+919876543210"}. */
    public static String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ValidationException("phoneNumber must not be blank");
        }
        String normalized = SEPARATORS.matcher(phoneNumber.trim()).replaceAll("");
        if (!E164.matcher(normalized).matches()) {
            throw new ValidationException("phoneNumber must be in E.164 form, e.g. +919876543210: " + normalized);
        }
        return normalized;
    }
}

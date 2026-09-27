package com.chetana.keystone.security;

import java.time.Instant;

/**
 * The access-token claims Keystone models, mapped from the provider's raw claim set by
 * {@link JwtAuthenticator}.
 *
 * <p>Only what the platform acts on is modelled, so services depend on typed fields instead of
 * stringly-keyed lookups into a claim map (see docs/CODING_GUIDELINES_BACKEND.md §13). Optional
 * claims are {@code null} when the provider did not issue them — a record component cannot be an
 * {@code Optional} (see §3).
 *
 * @param email     the identity's {@code email} claim, or {@code null} when absent
 * @param role      the provider-issued {@code role} claim (e.g. Supabase's {@code authenticated}),
 *                  or {@code null} when absent
 * @param issuedAt  the {@code iat} claim, or {@code null} when absent
 * @param expiresAt the {@code exp} claim, or {@code null} when absent
 */
public record Claims(String email, String role, Instant issuedAt, Instant expiresAt) {

    /** An empty claim set — an identity carried only by its subject (e.g. a test token double). */
    public static Claims empty() {
        return new Claims(null, null, null, null);
    }
}

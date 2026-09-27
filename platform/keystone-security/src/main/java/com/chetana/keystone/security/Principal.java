package com.chetana.keystone.security;

/**
 * An authenticated caller: the JWT {@code sub} plus the provider claims Keystone models as
 * {@link Claims}. The raw token claim map is never exposed (see
 * docs/CODING_GUIDELINES_BACKEND.md §13).
 */
public record Principal(String subject, Claims claims) {
}

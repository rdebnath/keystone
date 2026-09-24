package com.chetana.keystone.platform.admin.supabase;

/**
 * A Supabase Auth session returned by the password-grant token endpoint, handed back to the client
 * after a backend-proxied login.
 */
public record Session(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}

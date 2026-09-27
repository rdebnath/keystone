package com.chetana.keystone.platform.admin.identity;

/**
 * Change-own-password request for the backend-proxied change-password flow
 * ({@code POST /api/v1/me/password}).
 *
 * <p>{@code currentPassword} is required — and verified against Supabase Auth — whenever the caller is
 * <em>not</em> in the forced first-login state ({@code users.must_change_password}), because the
 * endpoint is authenticated only by a bearer token: proving the current password is what keeps a
 * stolen token from taking the account over. The forced flow omits it (the caller authenticated with
 * that password moments earlier), so its body is unchanged.
 */
public record ChangePasswordRequest(String currentPassword, String password) {
}

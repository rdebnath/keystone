package com.chetana.keystone.platform.admin.user;

/**
 * Reset-another-user's-password request ({@code PUT /api/v1/users/{id}/password}).
 *
 * <p>An administrator sets a <em>temporary</em> password for a user: it is handed straight to Supabase
 * Auth and never stored in the platform schema, and it re-arms the forced first-login change
 * ({@code users.must_change_password = true}) — exactly like the {@code temporaryPassword} of
 * {@code POST /api/v1/users}. A user changing their <em>own</em> password uses
 * {@code POST /api/v1/me/password}, which proves the current password; this request is therefore
 * rejected for the caller's own account.
 */
public record ResetPasswordRequest(String temporaryPassword) {
}

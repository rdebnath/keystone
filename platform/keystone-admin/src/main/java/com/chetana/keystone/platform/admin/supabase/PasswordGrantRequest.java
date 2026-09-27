package com.chetana.keystone.platform.admin.supabase;

/**
 * Body of a GoTrue password-grant token request
 * ({@code POST /auth/v1/token?grant_type=password}).
 */
record PasswordGrantRequest(String email, String password) {
}

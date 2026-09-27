package com.chetana.keystone.platform.admin.supabase;

/**
 * Body of a GoTrue admin "update user" request ({@code PUT /auth/v1/admin/users/{id}}).
 */
record UpdatePasswordRequest(String password) {
}

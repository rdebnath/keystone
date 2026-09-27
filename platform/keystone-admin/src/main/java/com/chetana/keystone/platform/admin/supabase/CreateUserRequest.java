package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body of a GoTrue admin "create user" request ({@code POST /auth/v1/admin/users}) — a typed
 * record rather than an ad-hoc map, so the wire field names live in one place (see
 * docs/CODING_GUIDELINES_BACKEND.md §13).
 */
record CreateUserRequest(
        String email,
        String password,
        @JsonProperty("email_confirm") boolean emailConfirm) {
}

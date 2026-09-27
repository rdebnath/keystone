package com.chetana.keystone.platform.admin.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A GoTrue "list users" page ({@code { "users": [...] }}) as returned by
 * {@code GET /auth/v1/admin/users}. The page metadata ({@code aud}, {@code next_page}) is not
 * needed here and is ignored; a missing {@code users} array normalizes to an empty list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ListUsersPage(List<GoTrueUser> users) {

    ListUsersPage {
        users = users == null ? List.of() : List.copyOf(users);
    }
}

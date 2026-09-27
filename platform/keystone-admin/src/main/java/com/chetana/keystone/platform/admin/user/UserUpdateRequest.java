package com.chetana.keystone.platform.admin.user;

import java.util.List;

/**
 * Update-user request ({@code PATCH /api/v1/users/{id}}): renames the user and replaces its role set
 * in one transaction. An empty {@code roles} list clears the user's roles.
 *
 * <p>{@code email} is deliberately absent. It is the virtual Supabase Auth identity bound to
 * {@code users.sub} and the counterpart of the account the login flow authenticates with, so it is
 * not editable from the console.
 */
public record UserUpdateRequest(String username, List<String> roles) {
}

package com.chetana.keystone.platform.admin.user;

import java.util.List;

/**
 * Update-user request ({@code PATCH /api/v1/users/{id}}): renames the user, sets its phone number and
 * replaces its role set in one transaction. An empty {@code roles} list clears the user's roles, and a
 * blank or absent {@code phoneNumber} clears the number.
 *
 * <p>{@code email} is deliberately absent. It is the virtual Supabase Auth identity bound to
 * {@code users.sub} and the counterpart of the account the login flow authenticates with, so it is
 * not editable from the console. The phone number is not an identity: it is plain profile data, so it
 * is editable here.
 */
public record UserUpdateRequest(String username, String phoneNumber, List<String> roles) {
}

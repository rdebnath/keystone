package com.chetana.keystone.platform.admin.user;

import java.util.List;
import java.util.UUID;

/**
 * Create-user request. {@code username} is the login local-part; {@code tenantId} is optional
 * ({@code null} = platform user). {@code email} is optional — when blank it defaults to
 * {@code username@tenantid.com}.
 */
public record UserRequest(String username, UUID tenantId, String email, String temporaryPassword, List<String> roles) {
}

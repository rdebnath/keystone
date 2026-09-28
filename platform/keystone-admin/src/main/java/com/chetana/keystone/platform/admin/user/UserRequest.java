package com.chetana.keystone.platform.admin.user;

import java.util.List;
import java.util.UUID;

/**
 * Create-user request. {@code username} is the login local-part; {@code tenantId} is optional
 * ({@code null} = platform user). {@code email} is optional — when blank it defaults to
 * {@code username@tenantid.com}. {@code phoneNumber} is optional — E.164 ({@code +919876543210}),
 * separators tolerated; blank or absent records no number.
 */
public record UserRequest(
        String username,
        UUID tenantId,
        String email,
        String phoneNumber,
        String temporaryPassword,
        List<String> roles) {
}

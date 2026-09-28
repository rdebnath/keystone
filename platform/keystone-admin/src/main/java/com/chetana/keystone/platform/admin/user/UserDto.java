package com.chetana.keystone.platform.admin.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A user. {@code phoneNumber} is the user's E.164 phone number ({@code +919876543210}), or {@code null}
 * when none is recorded.
 */
public record UserDto(
        UUID id,
        String sub,
        String username,
        String email,
        String phoneNumber,
        UUID tenantId,
        boolean mustChangePassword,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

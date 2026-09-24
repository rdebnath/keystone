package com.chetana.keystone.platform.admin.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID id,
        String sub,
        String username,
        String email,
        UUID tenantId,
        boolean mustChangePassword,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

package com.chetana.keystone.platform.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID id,
        String sub,
        String email,
        UUID tenantId,
        boolean mustChangePassword,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

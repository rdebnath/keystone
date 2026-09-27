package com.chetana.keystone.platform.admin.identity;

import java.util.List;
import java.util.UUID;

/**
 * The authenticated caller's profile and effective permissions (exposed by {@code GET /me}).
 */
public record MeDto(
        String sub,
        String username,
        UUID tenantId,
        boolean mustChangePassword,
        List<String> permissions) {
}

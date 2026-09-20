package com.chetana.keystone.platform.identity;

import java.util.List;
import java.util.UUID;

/**
 * The authenticated caller's profile and effective permissions (exposed by {@code GET /me}).
 */
public record MeDto(
        String sub,
        UUID tenantId,
        boolean mustChangePassword,
        List<String> permissions) {
}

package com.chetana.keystone.platform.admin.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A tenant. {@code platform} marks the synthetic platform plane row ({@code name} {@code Keystone},
 * {@code id} reserved) that {@code list()} returns ahead of the persisted tenants; it has no
 * timestamps and cannot be modified.
 */
public record TenantDto(
        UUID id,
        String name,
        String slug,
        boolean platform,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

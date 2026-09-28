package com.chetana.keystone.platform.admin.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A tenant. {@code platform} marks the synthetic platform plane row ({@code name} {@code Keystone},
 * {@code id} reserved) that {@code list()} returns ahead of the persisted tenants; it has no
 * timestamps, no country and cannot be modified.
 *
 * <p>{@code country} is the tenant's ISO 3166-1 alpha-2 code ({@code IN}, {@code DE}), or {@code null}
 * when none is recorded.
 */
public record TenantDto(
        UUID id,
        String name,
        String slug,
        String country,
        boolean platform,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

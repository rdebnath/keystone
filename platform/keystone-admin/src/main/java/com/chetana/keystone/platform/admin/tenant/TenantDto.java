package com.chetana.keystone.platform.admin.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantDto(UUID id, String name, String slug, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}

package com.chetana.keystone.platform.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantDto(UUID id, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}

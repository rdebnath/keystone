package com.chetana.keystone.platform.admin.permission;

import com.chetana.keystone.platform.admin.identity.Scope;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A catalog permission. {@code tenantId} is its <em>owner</em>: {@code null} is the global,
 * platform-defined catalog (assignable by the platform plane and every tenant); any other value is the
 * tenant that defined the permission for itself.
 */
public record PermissionDto(UUID id, String code, Scope scope, UUID tenantId,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}


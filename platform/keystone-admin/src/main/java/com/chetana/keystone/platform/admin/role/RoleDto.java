package com.chetana.keystone.platform.admin.role;

import com.chetana.keystone.platform.admin.identity.Scope;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A role with its granted permission codes. {@code tenantId} is its <em>owner</em>: {@code null} is the
 * global, platform-defined catalog (usable by the platform plane and every tenant); any other value is
 * the tenant that owns the row. {@code scope} is independent of the owner and says what the role is
 * <em>about</em> ({@code PLATFORM} = cross-tenant, {@code TENANT} = within one customer).
 */
public record RoleDto(
        UUID id,
        String code,
        Scope scope,
        UUID tenantId,
        List<String> permissions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}


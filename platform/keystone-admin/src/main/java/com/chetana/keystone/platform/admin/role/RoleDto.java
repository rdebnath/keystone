package com.chetana.keystone.platform.admin.role;

import com.chetana.keystone.platform.admin.identity.Scope;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String code,
        Scope scope,
        List<String> permissions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

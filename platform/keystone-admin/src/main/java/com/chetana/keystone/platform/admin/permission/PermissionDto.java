package com.chetana.keystone.platform.admin.permission;

import com.chetana.keystone.platform.admin.identity.Scope;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PermissionDto(UUID id, String code, Scope scope, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}

package com.chetana.keystone.platform.admin.permission;

import com.chetana.keystone.platform.admin.identity.Scope;

import java.util.UUID;

/**
 * Create-permission request. {@code tenantId} is the optional <em>owner</em>: omitted (or {@code null})
 * on the platform plane adds a global catalog permission; the tenant plane refuses it and always uses the
 * caller's own tenant.
 */
public record PermissionRequest(String code, Scope scope, UUID tenantId) {
}


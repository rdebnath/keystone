package com.chetana.keystone.platform.admin.role;

import com.chetana.keystone.platform.admin.identity.Scope;

import java.util.List;
import java.util.UUID;

/**
 * Create/update-role request. {@code tenantId} is the optional <em>owner</em>: omitted (or {@code null})
 * on the platform plane creates a global role; the tenant plane refuses it outright and always uses the
 * caller's own tenant, so a tenant admin has no way to name another tenant.
 */
public record RoleRequest(String code, Scope scope, UUID tenantId, List<String> permissions) {
}


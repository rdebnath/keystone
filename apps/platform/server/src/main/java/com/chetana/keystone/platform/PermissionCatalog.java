package com.chetana.keystone.platform;

import java.util.List;

/**
 * The platform-defined permission catalog, seeded idempotently at bootstrap. The wildcard {@code *}
 * grants all permissions and is assigned only to the {@code platform-admin} role.
 */
public final class PermissionCatalog {

    public static final String WILDCARD = "*";
    public static final String PLATFORM_ADMIN_ROLE = "platform-admin";

    public static final List<Permission> PERMISSIONS = List.of(
            new Permission("*", "PLATFORM"),
            new Permission("platform:tenant:create", "PLATFORM"),
            new Permission("platform:tenant:read", "PLATFORM"),
            new Permission("platform:tenant:update", "PLATFORM"),
            new Permission("platform:tenant:delete", "PLATFORM"),
            new Permission("platform:role:create", "PLATFORM"),
            new Permission("platform:role:read", "PLATFORM"),
            new Permission("platform:role:update", "PLATFORM"),
            new Permission("platform:role:delete", "PLATFORM"),
            new Permission("platform:permission:create", "PLATFORM"),
            new Permission("platform:permission:read", "PLATFORM"),
            new Permission("platform:permission:update", "PLATFORM"),
            new Permission("platform:permission:delete", "PLATFORM"),
            new Permission("platform:user:create", "PLATFORM"),
            new Permission("platform:user:read", "PLATFORM"),
            new Permission("platform:user:update", "PLATFORM"),
            new Permission("platform:user:delete", "PLATFORM"),
            new Permission("platform:user:assign-role", "PLATFORM"),
            new Permission("tenant:role:create", "TENANT"),
            new Permission("tenant:role:read", "TENANT"),
            new Permission("tenant:role:update", "TENANT"),
            new Permission("tenant:role:delete", "TENANT"),
            new Permission("tenant:permission:create", "TENANT"),
            new Permission("tenant:permission:read", "TENANT"),
            new Permission("tenant:permission:update", "TENANT"),
            new Permission("tenant:permission:delete", "TENANT"),
            new Permission("tenant:user:create", "TENANT"),
            new Permission("tenant:user:read", "TENANT"),
            new Permission("tenant:user:update", "TENANT"),
            new Permission("tenant:user:delete", "TENANT"),
            new Permission("tenant:user:assign-role", "TENANT"));

    private PermissionCatalog() {
    }

    public record Permission(String code, String scope) {
    }
}

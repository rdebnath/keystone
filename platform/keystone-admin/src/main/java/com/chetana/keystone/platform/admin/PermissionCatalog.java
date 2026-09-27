package com.chetana.keystone.platform.admin;

import com.chetana.keystone.platform.admin.identity.Access;

import java.util.ArrayList;
import java.util.List;

/**
 * The platform-defined permission catalog, seeded idempotently at bootstrap. Every resource carries
 * exactly two access levels — read/write ({@code <resource>:read-write}) and read-only
 * ({@code <resource>:read-only}). The wildcard {@code *} grants all permissions and is assigned
 * only to the {@code platform-admin} role.
 */
public final class PermissionCatalog {

    public static final String WILDCARD = "*";
    public static final String PLATFORM_ADMIN_ROLE = "platform-admin";

    public static final String PLATFORM_TENANT = "platform:tenant";
    public static final String PLATFORM_ROLE = "platform:role";
    public static final String PLATFORM_PERMISSION = "platform:permission";
    public static final String PLATFORM_USER = "platform:user";
    public static final String TENANT_ROLE = "tenant:role";
    public static final String TENANT_PERMISSION = "tenant:permission";
    public static final String TENANT_USER = "tenant:user";

    /** Every resource a level can be granted on, with the authorization scope it belongs to. */
    public static final List<Resource> RESOURCES = List.of(
            new Resource(PLATFORM_TENANT, "PLATFORM"),
            new Resource(PLATFORM_ROLE, "PLATFORM"),
            new Resource(PLATFORM_PERMISSION, "PLATFORM"),
            new Resource(PLATFORM_USER, "PLATFORM"),
            new Resource(TENANT_ROLE, "TENANT"),
            new Resource(TENANT_PERMISSION, "TENANT"),
            new Resource(TENANT_USER, "TENANT"));

    /** Two access levels per resource, plus the wildcard. */
    public static final List<Permission> PERMISSIONS = seeds();

    private PermissionCatalog() {
    }

    /** The code that carries {@code access} on {@code resource}, e.g. {@code platform:tenant:read-write}. */
    public static String code(String resource, Access access) {
        return resource + ":" + access.suffix();
    }

    /**
     * The codes a check for {@code resource} at {@code access} accepts: a read/write grant always
     * satisfies a read-only check, a read-only grant never satisfies a read/write check.
     */
    public static List<String> acceptedCodes(String resource, Access access) {
        if (access == Access.READ_WRITE) {
            return List.of(code(resource, Access.READ_WRITE));
        }
        return List.of(code(resource, Access.READ_ONLY), code(resource, Access.READ_WRITE));
    }

    /** Whether {@code code} ends in one of the two access levels. */
    public static boolean hasAccessLevel(String code) {
        int separator = code.lastIndexOf(':');
        return separator > 0
                && separator < code.length() - 1
                && Access.suffixes().contains(code.substring(separator + 1));
    }

    private static List<Permission> seeds() {
        List<Permission> seeds = new ArrayList<>();
        seeds.add(new Permission(WILDCARD, "PLATFORM"));
        for (Resource resource : RESOURCES) {
            for (Access access : Access.values()) {
                seeds.add(new Permission(code(resource.code(), access), resource.scope()));
            }
        }
        return List.copyOf(seeds);
    }

    public record Resource(String code, String scope) {
    }

    public record Permission(String code, String scope) {
    }
}

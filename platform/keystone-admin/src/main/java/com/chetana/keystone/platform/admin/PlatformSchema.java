package com.chetana.keystone.platform.admin;

import java.util.UUID;

/**
 * Shared constants for the platform admin schema, its reserved tenant slug, and the synthetic
 * platform "tenant".
 */
public final class PlatformSchema {

    /** The schema (in the shared database) that hosts the platform admin tables. */
    public static final String SCHEMA = "platform";

    /** Reserved slug for the platform admin plane (no {@code tenants} row; {@code users.tenant_id = NULL}). */
    public static final String RESERVED_SLUG = "keystone";

    /** Display name of the platform plane when it is listed alongside the tenants. */
    public static final String PLATFORM_TENANT_NAME = "Keystone";

    /**
     * Reserved id of the platform plane. {@code GET /api/v1/tenants} surfaces it as the synthetic
     * {@link #PLATFORM_TENANT_NAME} tenant so the console can list platform users exactly like a
     * tenant's users. It is never persisted: tenant ids are random v4 UUIDs, so this all-zero value
     * cannot collide with a real tenant, and writes addressing it are rejected.
     */
    public static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private PlatformSchema() {
    }

    /** Whether {@code tenantId} denotes the platform plane rather than a persisted tenant. */
    public static boolean isPlatformTenant(UUID tenantId) {
        return PLATFORM_TENANT_ID.equals(tenantId);
    }
}

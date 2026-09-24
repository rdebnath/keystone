package com.chetana.keystone.platform.admin;

/**
 * Shared constants for the platform admin schema and its reserved tenant slug.
 */
public final class PlatformSchema {

    /** The schema (in the shared database) that hosts the platform admin tables. */
    public static final String SCHEMA = "platform";

    /** Reserved slug for the platform admin plane (no {@code tenants} row; {@code users.tenant_id = NULL}). */
    public static final String RESERVED_SLUG = "keystone";

    private PlatformSchema() {
    }
}

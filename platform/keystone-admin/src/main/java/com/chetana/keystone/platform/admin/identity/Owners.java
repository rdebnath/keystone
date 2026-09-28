package com.chetana.keystone.platform.admin.identity;

import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.platform.admin.PlatformSchema;

import java.util.UUID;

/**
 * Normalizes the <em>owner</em> of a role or permission: {@code null} is the global, platform-defined
 * catalog (usable by the platform plane and every tenant); any other value is the id of the tenant that
 * owns the row.
 *
 * <p>The reserved platform tenant id is never an owner — the platform plane owns no rows, it addresses
 * the global ones by omitting the owner — so accepting it would create a row nobody could ever reach.
 */
public final class Owners {

    private Owners() {
    }

    public static UUID normalize(UUID requested) {
        if (PlatformSchema.isPlatformTenant(requested)) {
            throw new ValidationException("The platform plane owns no rows: omit tenantId for a global one");
        }
        return requested;
    }
}

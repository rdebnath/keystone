package com.chetana.keystone.platform.admin.identity;

import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.platform.admin.PermissionCatalog;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * The authorization plane a caller acts in — resolved from the authenticated caller, never from the
 * request — together with the permissions that caller holds.
 *
 * <p>The <strong>platform plane</strong> sees everything and owns the global rows. A <strong>tenant
 * caller</strong> sees the global catalog plus its own rows and may only touch its own; its tenant id
 * <em>is</em> the scope, so no request parameter can widen it (there is no cross-tenant id to tamper
 * with). The reserved platform tenant id is never a tenant scope.
 */
public record CallerScope(boolean platform, UUID tenantId, Set<String> permissions) {

    public CallerScope {
        if (platform && tenantId != null) {
            throw new IllegalArgumentException("the platform plane has no tenant");
        }
        if (!platform && tenantId == null) {
            throw new IllegalArgumentException("a tenant caller needs a tenant");
        }
    }

    /** The platform plane, holding whatever {@code permissions} were resolved for the caller. */
    public static CallerScope platformPlane(Set<String> permissions) {
        return new CallerScope(true, null, Set.copyOf(permissions));
    }

    /** One tenant's plane. The tenant comes from the caller's own row, never from a request. */
    public static CallerScope tenant(UUID tenantId, Set<String> permissions) {
        if (tenantId == null) {
            throw new AccessDeniedException("A tenant scope is required for this operation");
        }
        return new CallerScope(false, tenantId, Set.copyOf(permissions));
    }

    /**
     * Escalation guardrail ({@code docs/ARCHITECTURE.md} §9.5 "grant only what you hold"): a tenant
     * admin may only grant codes it already holds, and never the wildcard. The platform plane is
     * unrestricted — its set already contains the wildcard.
     *
     * <p>"Holds" follows the same implication the guard uses: a read/write grant covers the read-only
     * code of the same resource, so an admin that holds {@code tenant:user:read-write} may hand out
     * {@code tenant:user:read-only} (write implies read).
     */
    public void requireGrantable(Collection<String> codes) {
        if (platform) {
            return;
        }
        for (String code : codes) {
            if (PermissionCatalog.WILDCARD.equals(code) || !holds(code)) {
                throw new ValidationException("Cannot grant a permission you do not hold: " + code);
            }
        }
    }

    /**
     * Whether the caller holds {@code code}, following the same implication the guard uses: a read/write
     * grant covers the read-only code of the same resource, so an admin holding
     * {@code tenant:user:read-write} also "holds" {@code tenant:user:read-only}.
     */
    public boolean holds(String code) {
        if (permissions.contains(code)) {
            return true;
        }
        String readWriteSuffix = ":" + Access.READ_WRITE.suffix();
        if (!PermissionCatalog.hasAccessLevel(code) || code.endsWith(readWriteSuffix)) {
            return false;
        }
        String sibling = code.substring(0, code.lastIndexOf(':')) + readWriteSuffix;
        return permissions.contains(sibling);
    }
}

package com.chetana.keystone.platform.admin.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.PlatformSchema;
import com.chetana.keystone.platform.admin.data.Platform;

import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.Tables.TENANTS;

/**
 * Resolves a tenant slug to its id. The reserved slug {@code keystone} resolves to {@code null},
 * i.e. the platform plane ({@code users.tenant_id IS NULL}).
 */
@Singleton
public final class TenantResolver {

    private final DataAccess data;

    @Inject
    public TenantResolver(@Platform DataAccess data) {
        this.data = data;
    }

    public UUID resolveBySlug(String slug) {
        if (PlatformSchema.RESERVED_SLUG.equals(slug)) {
            return null;
        }
        UUID id = data.read().select(TENANTS.ID).from(TENANTS).where(TENANTS.SLUG.eq(slug)).fetchOne(TENANTS.ID);
        if (id == null) {
            throw new NotFoundException("Tenant not found: " + slug);
        }
        return id;
    }
}

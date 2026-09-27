package com.chetana.keystone.platform.admin.tenant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.PlatformSchema;
import com.chetana.keystone.platform.admin.data.Platform;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.TENANTS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USER_ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USERS;

/**
 * Use-case service for tenant (customer) management.
 */
@Singleton
public final class TenantService {

    private final DataAccess data;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public TenantService(@Platform DataAccess data, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.data = data;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    public List<TenantDto> list() {
        var tenants = data.read().selectFrom(TENANTS)
                .orderBy(TENANTS.NAME)
                .fetch()
                .map(r -> new TenantDto(r.getId(), r.getName(), r.getSlug(), false, r.getCreatedAt(), r.getUpdatedAt()));
        return Stream.concat(Stream.of(platformTenant()), tenants.stream()).toList();
    }

    public TenantDto create(TenantRequest request) {
        String name = validateName(request);
        String slug = validateSlug(request.slug());
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        int inserted = data.write().insertInto(TENANTS, TENANTS.ID, TENANTS.NAME, TENANTS.SLUG, TENANTS.CREATED_AT, TENANTS.UPDATED_AT)
                .values(id, name, slug, now, now)
                .onConflictDoNothing()
                .execute();
        if (inserted == 0) {
            throw new ConflictException("Tenant already exists: " + slug);
        }
        return new TenantDto(id, name, slug, false, now, now);
    }

    public TenantDto update(UUID id, TenantRequest request) {
        requireModifiableTenant(id);
        String name = validateName(request);
        String slug = validateSlug(request.slug());
        var existing = data.read().selectFrom(TENANTS).where(TENANTS.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("Tenant not found: " + id);
        }
        OffsetDateTime now = now();
        data.write().update(TENANTS)
                .set(TENANTS.NAME, name)
                .set(TENANTS.SLUG, slug)
                .set(TENANTS.UPDATED_AT, now)
                .where(TENANTS.ID.eq(id))
                .execute();
        return new TenantDto(id, name, slug, false, existing.getCreatedAt(), now);
    }

    public void delete(UUID id) {
        requireModifiableTenant(id);
        int users = data.read().fetchCount(USERS, USERS.TENANT_ID.eq(id));
        if (users > 0) {
            throw new ConflictException("Cannot delete tenant with users: " + id);
        }
        data.transaction(tx -> {
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.TENANT_ID.eq(id)).execute();
            tx.deleteFrom(TENANTS).where(TENANTS.ID.eq(id)).execute();
        });
    }

    /** The synthetic platform plane row: platform users ({@code users.tenant_id IS NULL}) live here. */
    private static TenantDto platformTenant() {
        return new TenantDto(
                PlatformSchema.PLATFORM_TENANT_ID,
                PlatformSchema.PLATFORM_TENANT_NAME,
                PlatformSchema.RESERVED_SLUG,
                true,
                null,
                null);
    }

    private static String validateName(TenantRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("name must not be blank");
        }
        return request.name().trim();
    }

    /**
     * Normalizes the slug and rejects the reserved platform slug: {@code keystone} is the platform
     * plane on login, so a real tenant carrying it would be unreachable.
     */
    private static String validateSlug(String rawSlug) {
        String slug = TenantSlug.normalize(rawSlug);
        if (TenantSlug.isReserved(slug)) {
            throw new ValidationException("slug is reserved: " + slug);
        }
        return slug;
    }

    /** Rejects writes aimed at the synthetic platform tenant, which has no row to modify. */
    private static void requireModifiableTenant(UUID id) {
        if (PlatformSchema.isPlatformTenant(id)) {
            throw new ValidationException("The platform tenant cannot be modified");
        }
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

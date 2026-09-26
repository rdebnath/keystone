package com.chetana.keystone.platform.admin.tenant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.platform.admin.data.Platform;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.Tables.TENANTS;
import static com.chetana.keystone.platform.admin.jooq.Tables.USER_ROLES;
import static com.chetana.keystone.platform.admin.jooq.Tables.USERS;

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
        return data.read().selectFrom(TENANTS)
                .orderBy(TENANTS.NAME)
                .fetch()
                .map(r -> new TenantDto(r.getId(), r.getName(), r.getSlug(), r.getCreatedAt(), r.getUpdatedAt()));
    }

    public TenantDto create(TenantRequest request) {
        String name = validateName(request);
        String slug = TenantSlug.normalize(request.slug());
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        int inserted = data.write().insertInto(TENANTS, TENANTS.ID, TENANTS.NAME, TENANTS.SLUG, TENANTS.CREATED_AT, TENANTS.UPDATED_AT)
                .values(id, name, slug, now, now)
                .onConflictDoNothing()
                .execute();
        if (inserted == 0) {
            throw new ConflictException("Tenant already exists: " + slug);
        }
        return new TenantDto(id, name, slug, now, now);
    }

    public TenantDto update(UUID id, TenantRequest request) {
        String name = validateName(request);
        String slug = TenantSlug.normalize(request.slug());
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
        return new TenantDto(id, name, slug, existing.getCreatedAt(), now);
    }

    public void delete(UUID id) {
        int users = data.read().fetchCount(USERS, USERS.TENANT_ID.eq(id));
        if (users > 0) {
            throw new ConflictException("Cannot delete tenant with users: " + id);
        }
        data.transaction(tx -> {
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.TENANT_ID.eq(id)).execute();
            tx.deleteFrom(TENANTS).where(TENANTS.ID.eq(id)).execute();
        });
    }

    private static String validateName(TenantRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("name must not be blank");
        }
        return request.name().trim();
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

package com.chetana.keystone.platform.admin.tenant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.platform.admin.data.Platform;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

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

    private final DSLContext dsl;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public TenantService(@Platform DSLContext dsl, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    public List<TenantDto> list() {
        return dsl.selectFrom(TENANTS)
                .orderBy(TENANTS.NAME)
                .fetch()
                .map(r -> new TenantDto(r.getId(), r.getName(), r.getSlug(), r.getCreatedAt(), r.getUpdatedAt()));
    }

    public TenantDto create(TenantRequest request) {
        String name = validateName(request);
        String slug = TenantSlug.normalize(request.slug());
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        int inserted = dsl.insertInto(TENANTS, TENANTS.ID, TENANTS.NAME, TENANTS.SLUG, TENANTS.CREATED_AT, TENANTS.UPDATED_AT)
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
        var existing = dsl.selectFrom(TENANTS).where(TENANTS.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("Tenant not found: " + id);
        }
        OffsetDateTime now = now();
        dsl.update(TENANTS)
                .set(TENANTS.NAME, name)
                .set(TENANTS.SLUG, slug)
                .set(TENANTS.UPDATED_AT, now)
                .where(TENANTS.ID.eq(id))
                .execute();
        return new TenantDto(id, name, slug, existing.getCreatedAt(), now);
    }

    public void delete(UUID id) {
        int users = dsl.fetchCount(USERS, USERS.TENANT_ID.eq(id));
        if (users > 0) {
            throw new ConflictException("Cannot delete tenant with users: " + id);
        }
        dsl.transaction(configuration -> {
            DSLContext tx = DSL.using(configuration);
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

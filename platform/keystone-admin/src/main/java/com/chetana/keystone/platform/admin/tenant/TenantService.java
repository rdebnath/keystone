package com.chetana.keystone.platform.admin.tenant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.query.OptionList;
import com.chetana.keystone.common.query.Page;
import com.chetana.keystone.common.query.PageRequest;
import com.chetana.keystone.common.query.SortOrder;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.data.Search;
import com.chetana.keystone.platform.admin.PlatformSchema;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.jooq.platform.tables.records.TenantsRecord;
import com.chetana.keystone.platform.admin.role.RoleSeeder;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLE_PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLES;
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
    private final RoleSeeder roleSeeder;

    @Inject
    public TenantService(@Platform DataAccess data, IdGenerator idGenerator, DateTimeService dateTimeService,
                         RoleSeeder roleSeeder) {
        this.data = data;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
        this.roleSeeder = roleSeeder;
    }

    /**
     * One page of tenants in the console's order: the synthetic platform row first, then the persisted
     * tenants by the requested sort key ({@code name} by default).
     *
     * <p>{@code search} matches name, slug or country, and the platform row takes part in it like any other
     * row — present with no term, and present only while the term matches it otherwise, because a result
     * may never contain a row that does not match. Paging happens in SQL ({@link PlatformRowWindow} maps
     * the page onto the table around the synthetic row), so a search covers every tenant, not a page.
     */
    public Page<TenantDto> list(PageRequest page, String search) {
        Condition filter = Search.containsIgnoreCase(search, TENANTS.NAME, TENANTS.SLUG, TENANTS.COUNTRY);
        long realTotal = data.read().fetchCount(TENANTS, filter);
        boolean platformIncluded = platformRowMatches(search);
        PlatformRowWindow window = PlatformRowWindow.of(page, platformIncluded, realTotal);
        List<TenantDto> tenants = window.realLimit() == 0 ? List.of() : data.read()
                .selectFrom(TENANTS)
                .where(filter)
                .orderBy(orderOf(page))
                .limit(window.realLimit())
                .offset(window.realOffset())
                .fetch(TenantService::toDto);
        List<TenantDto> items = window.includesPlatformRow()
                ? Stream.concat(Stream.of(platformTenant()), tenants.stream()).toList()
                : tenants;
        return Page.of(items, page, realTotal + (platformIncluded ? 1 : 0));
    }

    /**
     * Every tenant, for the console's pickers (the tenant and owner dropdowns).
     *
     * <p>Unpaged by design — a dropdown must offer all its choices, and a page would silently offer only
     * the first one — but capped by {@link OptionList}, which reports truncation rather than dropping rows
     * quietly. The extra row beyond the cap is fetched purely to detect that case, and never returned.
     */
    public OptionList<TenantDto> options() {
        List<TenantDto> tenants = data.read()
                .selectFrom(TENANTS)
                .orderBy(TENANTS.NAME)
                .limit(OptionList.MAX_OPTIONS + 1)
                .fetch(TenantService::toDto);
        return OptionList.of(Stream.concat(Stream.of(platformTenant()), tenants.stream()).toList());
    }

    public TenantDto create(TenantRequest request) {
        String name = validateName(request);
        String slug = validateSlug(request.slug());
        String country = validateCountry(request);
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        data.transaction(tx -> {
            int inserted = tx.insertInto(TENANTS, TENANTS.ID, TENANTS.NAME, TENANTS.SLUG, TENANTS.COUNTRY,
                            TENANTS.CREATED_AT, TENANTS.UPDATED_AT)
                    .values(id, name, slug, country, now, now)
                    .onConflictDoNothing()
                    .execute();
            if (inserted == 0) {
                throw new ConflictException("Tenant already exists: " + slug);
            }
            // Every tenant gets its own administrator role in the same transaction — a tenant that exists
            // without one could not be administered, and the platform admin assigns it to the first user.
            roleSeeder.ensureTenantAdminRole(tx, id, now);
        });
        return new TenantDto(id, name, slug, country, false, now, now);
    }

    public TenantDto update(UUID id, TenantRequest request) {
        requireModifiableTenant(id);
        String name = validateName(request);
        String slug = validateSlug(request.slug());
        String country = validateCountry(request);
        var existing = data.read().selectFrom(TENANTS).where(TENANTS.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("Tenant not found: " + id);
        }
        OffsetDateTime now = now();
        data.write().update(TENANTS)
                .set(TENANTS.NAME, name)
                .set(TENANTS.SLUG, slug)
                .set(TENANTS.COUNTRY, country)
                .set(TENANTS.UPDATED_AT, now)
                .where(TENANTS.ID.eq(id))
                .execute();
        return new TenantDto(id, name, slug, country, false, existing.getCreatedAt(), now);
    }

    public void delete(UUID id) {
        requireModifiableTenant(id);
        int users = data.read().fetchCount(USERS, USERS.TENANT_ID.eq(id));
        if (users > 0) {
            throw new ConflictException("Cannot delete tenant with users: " + id);
        }
        data.transaction(tx -> {
            deleteOwnedRoles(tx, id);
            deleteOwnedPermissions(tx, id);
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.TENANT_ID.eq(id)).execute();
            tx.deleteFrom(TENANTS).where(TENANTS.ID.eq(id)).execute();
        });
    }

    /**
     * Removes the roles the tenant owns, with their grants and assignments, before the tenant row: the
     * {@code fk_roles_tenant} foreign key would otherwise block the delete. Sub-selects rather than joins
     * keep each statement a plain delete.
     */
    private static void deleteOwnedRoles(DSLContext tx, UUID tenantId) {
        var owned = DSL.select(ROLES.ID).from(ROLES).where(ROLES.TENANT_ID.eq(tenantId));
        tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.ROLE_ID.in(owned)).execute();
        tx.deleteFrom(USER_ROLES).where(USER_ROLES.ROLE_ID.in(owned)).execute();
        tx.deleteFrom(ROLES).where(ROLES.TENANT_ID.eq(tenantId)).execute();
    }

    /** Removes the permissions the tenant defined, plus the grants that referenced them. */
    private static void deleteOwnedPermissions(DSLContext tx, UUID tenantId) {
        var owned = DSL.select(PERMISSIONS.ID).from(PERMISSIONS).where(PERMISSIONS.TENANT_ID.eq(tenantId));
        tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.PERMISSION_ID.in(owned)).execute();
        tx.deleteFrom(PERMISSIONS).where(PERMISSIONS.TENANT_ID.eq(tenantId)).execute();
    }

    /** Whether the synthetic platform row matches {@code search} — it is a row of the list, so it is searched. */
    private static boolean platformRowMatches(String search) {
        return Search.matches(search, PlatformSchema.PLATFORM_TENANT_NAME, PlatformSchema.RESERVED_SLUG);
    }

    /**
     * The requested sort order, always with {@code id} as the tiebreaker: a page window is only stable when
     * the order is <em>total</em>. Without a tiebreaker two rows could compare equal, and PostgreSQL is
     * free to return them in either order per query — which shows up as a row appearing on two pages, or
     * on none.
     */
    private static List<OrderField<?>> orderOf(PageRequest page) {
        if (!page.sorted()) {
            return List.of(TENANTS.NAME.asc(), TENANTS.ID.asc());
        }
        Field<?> key = switch (page.sort()) {
            case "name" -> TENANTS.NAME;
            case "slug" -> TENANTS.SLUG;
            case "country" -> TENANTS.COUNTRY;
            case "createdAt" -> TENANTS.CREATED_AT;
            case "updatedAt" -> TENANTS.UPDATED_AT;
            default -> throw new ValidationException("Unknown sort key: " + page.sort()
                    + " (allowed: name, slug, country, createdAt, updatedAt)");
        };
        return List.of(direction(key, page.order()), TENANTS.ID.asc());
    }

    private static OrderField<?> direction(Field<?> field, SortOrder order) {
        return order == SortOrder.DESC ? field.desc() : field.asc();
    }

    private static TenantDto toDto(TenantsRecord record) {
        return new TenantDto(record.getId(), record.getName(), record.getSlug(), record.getCountry(), false,
                record.getCreatedAt(), record.getUpdatedAt());
    }

    /** The synthetic platform plane row: platform users ({@code users.tenant_id IS NULL}) live here. */
    private static TenantDto platformTenant() {
        return new TenantDto(
                PlatformSchema.PLATFORM_TENANT_ID,
                PlatformSchema.PLATFORM_TENANT_NAME,
                PlatformSchema.RESERVED_SLUG,
                null,
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
     * Normalizes the optional country ({@code null} — none recorded — is a valid value), so a blank or
     * absent field clears it rather than storing an empty string.
     */
    private static String validateCountry(TenantRequest request) {
        return Country.normalizeOptional(request.country());
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

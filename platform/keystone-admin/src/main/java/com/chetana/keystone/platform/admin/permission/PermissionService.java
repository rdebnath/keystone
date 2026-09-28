package com.chetana.keystone.platform.admin.permission;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
import com.chetana.keystone.common.error.ConflictException;
import com.chetana.keystone.common.error.NotFoundException;
import com.chetana.keystone.common.error.ValidationException;
import com.chetana.keystone.common.id.IdGenerator;
import com.chetana.keystone.common.query.Page;
import com.chetana.keystone.common.query.PageRequest;
import com.chetana.keystone.common.query.SortOrder;
import com.chetana.keystone.common.time.DateTimeService;
import com.chetana.keystone.data.DataAccess;
import com.chetana.keystone.data.Search;
import com.chetana.keystone.platform.admin.PermissionCatalog;
import com.chetana.keystone.platform.admin.PlatformSchema;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.identity.Access;
import com.chetana.keystone.platform.admin.identity.CallerScope;
import com.chetana.keystone.platform.admin.identity.Owners;
import com.chetana.keystone.platform.admin.identity.Scope;
import com.chetana.keystone.platform.admin.jooq.platform.tables.records.PermissionsRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLE_PERMISSIONS;

/**
 * Use-case service for the permission catalog across the two planes.
 *
 * <p>A permission's <strong>owner</strong> is its {@code tenant_id}: {@code null} is the global catalog
 * (assignable by the platform plane and every tenant); a tenant id is a permission that tenant defined
 * for itself, which only its own roles can hold. On the tenant plane the owner is derived from the
 * caller, never from the request.
 *
 * <p>As with roles, a tenant may not shadow the global catalog: a code the catalog already carries cannot
 * be re-defined by a tenant, so a code never resolves to two rows for the same caller.
 */
@Singleton
public final class PermissionService {

    private final DataAccess data;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public PermissionService(@Platform DataAccess data, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.data = data;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    /**
     * One page of the permissions {@code caller} may see — the same owner rule as the role list, with
     * optional {@code scope} and {@code access} filters and a {@code search} term matched against the code.
     *
     * <p>Count and window share one condition, so a search covers the whole catalog the caller may see.
     */
    public Page<PermissionDto> list(CallerScope caller, UUID filter, Scope scope, Access access, String search,
                                    PageRequest page) {
        Condition where = ownerFilter(effectiveOwner(caller, filter))
                .and(scopeFilter(scope))
                .and(accessFilter(access))
                .and(Search.containsIgnoreCase(search, PERMISSIONS.CODE));
        long total = data.read().fetchCount(PERMISSIONS, where);
        return Page.of(data.read()
                .selectFrom(PERMISSIONS)
                .where(where)
                .orderBy(orderOf(page))
                .limit(page.size())
                .offset(page.offset())
                .fetch(PermissionService::toDto), page, total);
    }

    private static PermissionDto toDto(PermissionsRecord permission) {
        return new PermissionDto(
                permission.getId(),
                permission.getCode(),
                Scope.from(permission.getScope()),
                permission.getTenantId(),
                permission.getCreatedAt(),
                permission.getUpdatedAt());
    }

    /** The optional scope filter: absent means every scope the caller can see. */
    private static Condition scopeFilter(Scope scope) {
        return scope == null ? DSL.noCondition() : PERMISSIONS.SCOPE.eq(scope.name());
    }

    /**
     * The optional access-level filter: absent means both levels.
     *
     * <p>The level is the last segment of the code, so the filter is a **suffix match** on that segment. It is
     * a true filter on the code, not on a separate column: the wildcard ({@code *}) carries no level and so
     * belongs to neither level's set, and a code a tenant defined is filtered like any other.
     */
    private static Condition accessFilter(Access access) {
        return access == null ? DSL.noCondition() : PERMISSIONS.CODE.endsWith(":" + access.suffix());
    }

    /**
     * The requested sort order, with {@code id} as the tiebreaker so a page window is stable. The default
     * keeps the console's own order: the global catalog first, then each tenant's rows, each by code.
     */
    private static List<OrderField<?>> orderOf(PageRequest page) {
        if (!page.sorted()) {
            return List.of(PERMISSIONS.TENANT_ID.asc().nullsFirst(), PERMISSIONS.CODE.asc(),
                    PERMISSIONS.ID.asc());
        }
        Field<?> key = switch (page.sort()) {
            case "code" -> PERMISSIONS.CODE;
            case "createdAt" -> PERMISSIONS.CREATED_AT;
            case "updatedAt" -> PERMISSIONS.UPDATED_AT;
            default -> throw new ValidationException("Unknown sort key: " + page.sort()
                    + " (allowed: code, createdAt, updatedAt)");
        };
        return List.of(direction(key, page.order()), PERMISSIONS.ID.asc());
    }

    private static OrderField<?> direction(Field<?> field, SortOrder order) {
        return order == SortOrder.DESC ? field.desc() : field.asc();
    }

    /** Adds a permission owned by the caller's plane. */
    public PermissionDto create(CallerScope caller, PermissionRequest request) {
        validateCode(request.code());
        UUID owner = ownerOf(caller, request.tenantId());
        Scope scope = scopeOf(caller, request.scope());
        requireOwnableScope(owner, scope);
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        return data.transactionResult(tx -> {
            requireNotShadowingGlobalCatalog(tx, request.code(), owner);
            int inserted = tx.insertInto(PERMISSIONS, PERMISSIONS.ID, PERMISSIONS.CODE, PERMISSIONS.SCOPE,
                            PERMISSIONS.TENANT_ID, PERMISSIONS.CREATED_AT, PERMISSIONS.UPDATED_AT)
                    .values(id, request.code(), scope.name(), owner, now, now)
                    .onConflictDoNothing()
                    .execute();
            if (inserted == 0) {
                throw new ConflictException("Permission already exists: " + request.code()
                        + (owner == null ? " (global)" : " (this tenant)"));
            }
            return new PermissionDto(id, request.code(), scope, owner, now, now);
        });
    }

    public void delete(CallerScope caller, UUID id) {
        var existing = data.read().selectFrom(PERMISSIONS).where(PERMISSIONS.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("Permission not found: " + id);
        }
        requireManageable(caller, existing.getId(), existing.getCode(), existing.getTenantId());
        data.transaction(tx -> {
            tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.PERMISSION_ID.eq(id)).execute();
            tx.deleteFrom(PERMISSIONS).where(PERMISSIONS.ID.eq(id)).execute();
        });
    }

    /**
     * The wildcard is refused outright — deleting it would strip the platform admin of everything until
     * the next bootstrap — and a tenant may only touch its own rows.
     */
    private static void requireManageable(CallerScope caller, UUID id, String code, UUID tenantId) {
        if (PermissionCatalog.WILDCARD.equals(code)) {
            throw new AccessDeniedException("The wildcard permission cannot be deleted: " + code);
        }
        if (caller.platform()) {
            return;
        }
        if (tenantId == null) {
            throw new AccessDeniedException("Global permissions are managed by the platform: " + code);
        }
        if (!tenantId.equals(caller.tenantId())) {
            throw new NotFoundException("Permission not found: " + id);
        }
    }

    /** Refuses a tenant-owned code the global catalog already uses, so a code never means two things. */
    private static void requireNotShadowingGlobalCatalog(DSLContext tx, String code, UUID owner) {
        if (owner == null) {
            return;
        }
        if (tx.fetchExists(PERMISSIONS, PERMISSIONS.CODE.eq(code).and(PERMISSIONS.TENANT_ID.isNull()))) {
            throw new ConflictException("A global permission already uses this code: " + code);
        }
    }

    private static UUID ownerOf(CallerScope caller, UUID requested) {
        if (caller.platform()) {
            return Owners.normalize(requested);
        }
        if (requested != null) {
            throw new ValidationException("A tenant does not choose the owner: it is the caller's tenant");
        }
        return caller.tenantId();
    }

    /** The tenant plane only ever owns TENANT-scope rows; the platform plane sends the scope. */
    private static Scope scopeOf(CallerScope caller, Scope requested) {
        if (!caller.platform()) {
            return Scope.TENANT;
        }
        if (requested == null) {
            throw new ValidationException("scope must not be null");
        }
        return requested;
    }

    private static void requireOwnableScope(UUID owner, Scope scope) {
        if (owner != null && scope == Scope.PLATFORM) {
            throw new ValidationException("A tenant-owned permission must be TENANT scope: " + scope);
        }
    }

    private static UUID effectiveOwner(CallerScope caller, UUID filter) {
        if (caller.platform()) {
            return filter;
        }
        if (filter != null) {
            throw new ValidationException("A tenant cannot choose the tenant to list");
        }
        return caller.tenantId();
    }

    private static Condition ownerFilter(UUID owner, TableField<?, UUID> field) {
        if (owner == null) {
            return DSL.noCondition();
        }
        if (PlatformSchema.isPlatformTenant(owner)) {
            return field.isNull();
        }
        return field.isNull().or(field.eq(owner));
    }

    private static Condition ownerFilter(UUID owner) {
        return ownerFilter(owner, PERMISSIONS.TENANT_ID);
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("code must not be blank");
        }
        if (!PermissionCatalog.hasAccessLevel(code)) {
            throw new ValidationException("code must end with an access level ("
                    + String.join(", ", Access.suffixes()) + "): " + code);
        }
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

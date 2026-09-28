package com.chetana.keystone.platform.admin.role;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.chetana.keystone.common.error.AccessDeniedException;
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
import com.chetana.keystone.platform.admin.PermissionCatalog;
import com.chetana.keystone.platform.admin.PlatformSchema;
import com.chetana.keystone.platform.admin.data.Platform;
import com.chetana.keystone.platform.admin.identity.CallerScope;
import com.chetana.keystone.platform.admin.identity.Owners;
import com.chetana.keystone.platform.admin.identity.Scope;
import com.chetana.keystone.platform.admin.jooq.platform.tables.records.RolesRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.chetana.keystone.platform.admin.jooq.platform.Tables.PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLE_PERMISSIONS;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.ROLES;
import static com.chetana.keystone.platform.admin.jooq.platform.Tables.USER_ROLES;

/**
 * Use-case service for role management across the two planes.
 *
 * <p>A role's <strong>owner</strong> is its {@code tenant_id}: {@code null} is the global,
 * platform-defined catalog the platform plane and every tenant can use; a tenant id is a role only that
 * tenant (and the platform plane) can see or assign. On the tenant plane the owner is <em>derived from
 * the caller</em>, so a tenant admin has no way to name another tenant, and it is immutable once the role
 * exists.
 *
 * <p>A tenant may not shadow the global catalog: creating or renaming a tenant role to a code that
 * already exists globally is refused, so a code never resolves to two rows for the same caller.
 */
@Singleton
public final class RoleService {

    private final DataAccess data;
    private final IdGenerator idGenerator;
    private final DateTimeService dateTimeService;

    @Inject
    public RoleService(@Platform DataAccess data, IdGenerator idGenerator, DateTimeService dateTimeService) {
        this.data = data;
        this.idGenerator = idGenerator;
        this.dateTimeService = dateTimeService;
    }

    /**
     * The roles {@code caller} may see. A tenant caller gets the global catalog plus its own rows; the
     * platform plane gets everything, optionally narrowed by {@code filter} — {@code null} is everything,
     * the reserved platform id is the global catalog only, a tenant id is the global rows plus that
     * tenant's.
     */
    /**
     * One page of the roles {@code caller} may see. A tenant caller gets the global catalog plus its own
     * rows; the platform plane gets everything, optionally narrowed by {@code filter} — {@code null} is
     * everything, the reserved platform id is the global catalog only, a tenant id is the global rows plus
     * that tenant's. A present {@code scope} filters on what a row is <em>about</em>, and {@code search}
     * matches the code.
     *
     * <p>The count and the window run against the same condition, so a search covers every role the caller
     * may see; the grants of the returned roles come from one grouped query over that page's ids.
     */
    public Page<RoleDto> list(CallerScope caller, UUID filter, Scope scope, String search, PageRequest page) {
        Condition where = ownerFilter(effectiveOwner(caller, filter), ROLES.TENANT_ID)
                .and(scopeFilter(scope))
                .and(Search.containsIgnoreCase(search, ROLES.CODE));
        long total = data.read().fetchCount(ROLES, where);
        var roles = data.read().selectFrom(ROLES)
                .where(where)
                .orderBy(orderOf(page))
                .limit(page.size())
                .offset(page.offset())
                .fetch();
        Map<UUID, List<String>> permissionsByRole = permissionsOf(roles.getValues(ROLES.ID));
        return Page.of(roles.map(role -> toDto(role, permissionsByRole)), page, total);
    }

    /**
     * Every role {@code caller} may see, for the console's pickers — the user editor's role checklist,
     * which must offer all of them.
     *
     * <p>Same visibility rule as {@link #list}, unpaged by design, and capped by {@link OptionList}, which
     * reports truncation instead of quietly dropping choices.
     */
    public OptionList<RoleDto> options(CallerScope caller, UUID filter) {
        var roles = data.read().selectFrom(ROLES)
                .where(ownerFilter(effectiveOwner(caller, filter), ROLES.TENANT_ID))
                .orderBy(ROLES.TENANT_ID.asc().nullsFirst(), ROLES.CODE)
                .limit(OptionList.MAX_OPTIONS + 1)
                .fetch();
        Map<UUID, List<String>> permissionsByRole = permissionsOf(roles.getValues(ROLES.ID));
        return OptionList.of(roles.map(role -> toDto(role, permissionsByRole)));
    }

    /** The grants of {@code roleIds}, grouped: one query for the page, not one per role. */
    private Map<UUID, List<String>> permissionsOf(List<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return data.read()
                .select(ROLE_PERMISSIONS.ROLE_ID, PERMISSIONS.CODE)
                .from(ROLE_PERMISSIONS)
                .join(PERMISSIONS).on(PERMISSIONS.ID.eq(ROLE_PERMISSIONS.PERMISSION_ID))
                .where(ROLE_PERMISSIONS.ROLE_ID.in(roleIds))
                .fetchGroups(ROLE_PERMISSIONS.ROLE_ID, PERMISSIONS.CODE);
    }

    private static RoleDto toDto(RolesRecord role, Map<UUID, List<String>> permissionsByRole) {
        return new RoleDto(
                role.getId(),
                role.getCode(),
                Scope.from(role.getScope()),
                role.getTenantId(),
                permissionsByRole.getOrDefault(role.getId(), List.of()).stream().sorted().toList(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }

    /** The optional scope filter: absent means every scope the caller can see. */
    private static Condition scopeFilter(Scope scope) {
        return scope == null ? DSL.noCondition() : ROLES.SCOPE.eq(scope.name());
    }

    /**
     * The requested sort order, with {@code id} as the tiebreaker so a page window is stable. The default
     * keeps the console's own order — the global catalog first, then each tenant's rows, each by code.
     */
    private static List<OrderField<?>> orderOf(PageRequest page) {
        if (!page.sorted()) {
            return List.of(ROLES.TENANT_ID.asc().nullsFirst(), ROLES.CODE.asc(), ROLES.ID.asc());
        }
        Field<?> key = switch (page.sort()) {
            case "code" -> ROLES.CODE;
            case "createdAt" -> ROLES.CREATED_AT;
            case "updatedAt" -> ROLES.UPDATED_AT;
            default -> throw new ValidationException("Unknown sort key: " + page.sort()
                    + " (allowed: code, createdAt, updatedAt)");
        };
        return List.of(direction(key, page.order()), ROLES.ID.asc());
    }

    private static OrderField<?> direction(Field<?> field, SortOrder order) {
        return order == SortOrder.DESC ? field.desc() : field.asc();
    }

    /** Creates a role owned by the caller's plane, granted {@code request.permissions()}. */
    public RoleDto create(CallerScope caller, RoleRequest request) {
        requireCode(request.code());
        UUID owner = ownerOf(caller, request.tenantId());
        Scope scope = scopeOf(caller, request.scope());
        requireOwnableScope(owner, scope);
        List<String> codes = normalized(request.permissions());
        caller.requireGrantable(codes);
        UUID id = idGenerator.nextId();
        OffsetDateTime now = now();
        return data.transactionResult(tx -> {
            requireNotShadowingGlobalCatalog(tx, request.code(), owner);
            int inserted = tx.insertInto(ROLES, ROLES.ID, ROLES.CODE, ROLES.SCOPE, ROLES.TENANT_ID,
                            ROLES.CREATED_AT, ROLES.UPDATED_AT)
                    .values(id, request.code(), scope.name(), owner, now, now)
                    .onConflictDoNothing()
                    .execute();
            if (inserted == 0) {
                throw new ConflictException("Role already exists: " + request.code()
                        + (owner == null ? " (global)" : " (this tenant)"));
            }
            grantPermissions(tx, id, owner, scope, codes);
            return new RoleDto(id, request.code(), scope, owner, codes, now, now);
        });
    }

    /** Renames a role and replaces its grants. The owner is immutable. */
    public RoleDto update(CallerScope caller, UUID id, RoleRequest request) {
        requireCode(request.code());
        Scope scope = scopeOf(caller, request.scope());
        List<String> codes = normalized(request.permissions());
        caller.requireGrantable(codes);
        var existing = data.read().selectFrom(ROLES).where(ROLES.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("Role not found: " + id);
        }
        requireManageable(caller, existing.getId(), existing.getCode(), existing.getTenantId());
        requireOwnableScope(existing.getTenantId(), scope);
        OffsetDateTime now = now();
        data.transaction(tx -> {
            requireNotShadowingGlobalCatalog(tx, request.code(), existing.getTenantId());
            tx.update(ROLES)
                    .set(ROLES.CODE, request.code())
                    .set(ROLES.SCOPE, scope.name())
                    .set(ROLES.UPDATED_AT, now)
                    .where(ROLES.ID.eq(id))
                    .execute();
            tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.ROLE_ID.eq(id)).execute();
            grantPermissions(tx, id, existing.getTenantId(), scope, codes);
        });
        return new RoleDto(id, request.code(), scope, existing.getTenantId(), codes,
                existing.getCreatedAt(), now);
    }

    public void delete(CallerScope caller, UUID id) {
        var existing = data.read().selectFrom(ROLES).where(ROLES.ID.eq(id)).fetchOne();
        if (existing == null) {
            throw new NotFoundException("Role not found: " + id);
        }
        requireManageable(caller, existing.getId(), existing.getCode(), existing.getTenantId());
        data.transaction(tx -> {
            tx.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.ROLE_ID.eq(id)).execute();
            tx.deleteFrom(USER_ROLES).where(USER_ROLES.ROLE_ID.eq(id)).execute();
            tx.deleteFrom(ROLES).where(ROLES.ID.eq(id)).execute();
        });
    }

    /**
     * Grants {@code codes} to the role. A grant must be a permission the role's owner may use: the global
     * catalog, or the owner's own. A **global** role has no owner, so it may hold the global catalog
     * <em>only</em> — its grants are handed to every tenant that holds it, so a tenant-owned permission would
     * leak that one tenant's code into everybody else's roles. A code carried by both resolves to the global
     * row — the catalog is canonical, and a tenant cannot shadow it anyway.
     */
    private static void grantPermissions(DSLContext ctx, UUID roleId, UUID owner, Scope scope, List<String> codes) {
        for (String code : codes) {
            var permission = ctx.select(PERMISSIONS.ID, PERMISSIONS.SCOPE)
                    .from(PERMISSIONS)
                    .where(PERMISSIONS.CODE.eq(code))
                    .and(grantableTo(owner))
                    .orderBy(PERMISSIONS.TENANT_ID.asc().nullsFirst())
                    .fetchAny();
            if (permission == null) {
                throw new ValidationException("Unknown permission: " + code
                        + (owner == null ? " (a global role may hold the global catalog only)" : ""));
            }
            if (!scope.name().equals(permission.get(PERMISSIONS.SCOPE))) {
                throw new ValidationException("Permission scope mismatch for role " + scope + ": " + code);
            }
            ctx.insertInto(ROLE_PERMISSIONS, ROLE_PERMISSIONS.ROLE_ID, ROLE_PERMISSIONS.PERMISSION_ID)
                    .values(roleId, permission.get(PERMISSIONS.ID))
                    .onConflictDoNothing()
                    .execute();
        }
    }

    /**
     * The permissions a role owned by {@code owner} may hold: the global catalog, plus the owner's own rows.
     *
     * <p>Deliberately **not** {@link #ownerFilter}: that one answers "which rows may this caller <em>see</em>",
     * where a platform caller listing without a filter sees everything. A grant is answered by the role's own
     * owner, and a global role owns nothing — so its grantees may hold the catalog only.
     */
    private static Condition grantableTo(UUID owner) {
        return owner == null
                ? PERMISSIONS.TENANT_ID.isNull()
                : PERMISSIONS.TENANT_ID.isNull().or(PERMISSIONS.TENANT_ID.eq(owner));
    }

    /** The owner the caller may act as: their own tenant, or the owner they named on the platform plane. */
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

    /** A tenant-owned row may never carry a cross-tenant (PLATFORM) capability. */
    private static void requireOwnableScope(UUID owner, Scope scope) {
        if (owner != null && scope == Scope.PLATFORM) {
            throw new ValidationException("A tenant-owned role must be TENANT scope: " + scope);
        }
    }

    /** The seeded administrative roles are immutable, so nobody can lock themselves out. */
    private static void requireManageable(CallerScope caller, UUID id, String code, UUID tenantId) {
        if (PermissionCatalog.isSeededAdminRole(code, tenantId)) {
            throw new AccessDeniedException("Seeded admin roles cannot be changed or deleted: " + code);
        }
        if (caller.platform()) {
            return;
        }
        if (tenantId == null) {
            throw new AccessDeniedException("Global roles are managed by the platform: " + code);
        }
        if (!tenantId.equals(caller.tenantId())) {
            throw new NotFoundException("Role not found: " + id);
        }
    }

    /** Refuses a tenant-owned code the global catalog already uses, so a code never means two things. */
    private static void requireNotShadowingGlobalCatalog(DSLContext tx, String code, UUID owner) {
        if (owner == null) {
            return;
        }
        if (tx.fetchExists(ROLES, ROLES.CODE.eq(code).and(ROLES.TENANT_ID.isNull()))) {
            throw new ConflictException("A global role already uses this code: " + code);
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

    /** {@code null} = no restriction; the global partition; or one tenant plus the global partition. */
    private static Condition ownerFilter(UUID owner, TableField<?, UUID> field) {
        if (owner == null) {
            return DSL.noCondition();
        }
        if (PlatformSchema.isPlatformTenant(owner)) {
            return field.isNull();
        }
        return field.isNull().or(field.eq(owner));
    }

    private static List<String> normalized(List<String> codes) {
        return codes == null ? List.of() : codes.stream().distinct().toList();
    }

    private static void requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("code must not be blank");
        }
    }

    private OffsetDateTime now() {
        return dateTimeService.now().atOffset(ZoneOffset.UTC);
    }
}

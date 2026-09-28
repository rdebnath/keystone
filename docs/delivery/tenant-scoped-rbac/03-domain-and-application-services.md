# Phase 3 — Domain & application services

> **Executed 2026-09-27.** Done as specified, with these notes: role seeding lives in one shared
> `role.RoleSeeder` (`ensurePlatformAdminRole` / `ensureTenantAdminRole`) that both `TenantService.create`
> and `BootstrapRunner` call, so the platform role and each tenant's role are ensured the same way and a
> half-provisioned role is completed rather than left alone. `identity.Owners.normalize` holds the
> "the platform plane owns no rows" rule, `identity.CallerScope` holds the plane/tenant/permissions and the
> escalation rule, and no separate `CallerTenant` resolver was needed (`PermissionGuard.callerScope` is
> the single seam). Two escalation bugs were found by the new tests and fixed via `CallerScope.holds`:
> a read/write grant counts as holding the read-only code of the same resource, for both granting and
> assigning roles.

**Scope** — make ownership part of the model and enforce the invariants; every lookup that today
matches on `code` alone gains the owner.

**Artifacts**

| File | Change |
| --- | --- |
| `…/admin/role/RoleDto.java` | add `UUID tenantId` (nullable; `null` = global) — before the timestamps |
| `…/admin/role/RoleRequest.java` | add `UUID tenantId` (nullable) |
| `…/admin/role/RoleService.java` | owner-aware create/update/delete/list; `(code, owner)` uniqueness; ownership rules on grants |
| `…/admin/permission/PermissionDto.java` | add `UUID tenantId` |
| `…/admin/permission/PermissionRequest.java` | add `UUID tenantId` |
| `…/admin/permission/PermissionService.java` | owner-aware create/delete/list |
| `…/admin/user/UserService.java` | `replaceRoles` resolves a role by `(code, plane)` and rejects a foreign-tenant role |
| `…/admin/tenant/TenantService.java` | `delete` cleans up the tenant's owned `role_permissions`, `roles` and `permissions` |
| `…/admin/BootstrapRunner.java` | `seedPlatformAdminRole` looks a role up by `code` alone today — constrain it to the global partition (`TENANT_ID.isNull()`), otherwise a tenant-owned `platform-admin` could be adopted as the platform's. The seeds themselves insert `tenant_id = NULL` (omitted column → `NULL`), so only the lookup changes. |
| `…/admin/identity/CallerTenant.java` (new) | resolves the authenticated caller's tenant from `users.sub`; throws `AccessDeniedException` when the caller has no tenant (platform user on a tenant route) — the tenant is never a request parameter |
| `…/admin/identity/PermissionResolver.java` | **unchanged** (its `user_roles` filter is already correct given the enforced invariant) |

**Dependencies** — phase 2 (the columns and jOOQ types must exist).

**Verification** — `mvn -pl platform/keystone-admin test`; new unit tests around the ownership rules
and lookups (phase 7). Compile before behaviour tests.

## RoleService

- `create(RoleRequest, owner, actorPermissions)` (signature below): `owner` is normalized from the
  request on the platform plane and from the caller on the tenant plane — the reserved platform
  id is rejected (`ValidationException`), `null` stays `null`. Insert `ROLES.TENANT_ID`; a duplicate
  is a `(code, tenant_id)` conflict → `ConflictException`.
- `grantPermissions(...)`: after the existing scope match, require the permission to be **assignable
  to the role's owner** — a global permission (`tenant_id IS NULL`) is always acceptable; a
  tenant-owned permission must belong to the **same** tenant as the role. Otherwise
  `ValidationException("Permission belongs to another tenant: " + code)`.
- `update(...)`: the owner is immutable (like a user's plane) — changing `tenantId` is rejected with
  `ValidationException`; the code/scope/permission replacement is unchanged, re-validated against the
  existing owner.
- `list(UUID tenantId)`: `null` → all; reserved platform id → global only (`TENANT_ID.isNull()`);
  otherwise → global **or** that tenant (`TENANT_ID.isNull().or(TENANT_ID.eq(id))`). Order by
  `(tenant_id nulls first, code)` so the global catalog reads first.
- `delete(id)`: unchanged shape (deletes `role_permissions`, `user_roles`, then the role), so a role
  of any owner can be removed.

## PermissionService

- `create`: same ownership normalization; reject the reserved platform id; `(code, tenant_id)`
  conflict → `ConflictException`; keep the `hasAccessLevel` validation.
- `delete(id)`: as today (delete `role_permissions` then the permission).
- `list(UUID tenantId)`: same filter as `RoleService.list`.

## UserService.replaceRoles

- Resolve the role within the user's **plane** instead of by code alone:
  - platform user (`tenantId == null`): global roles only, `scope = PLATFORM` (existing rule).
  - tenant user: global `TENANT` roles **or** roles owned by that tenant. Reject a global `PLATFORM`
    role (existing) and a role owned by another tenant (new,
    `ValidationException("Role belongs to another tenant: " + code)`).
- Keep the existing `onConflictDoNothing` insert and the `USER_ROLES.TENANT_ID` value.

## TenantService.delete

- In the same transaction, before deleting the tenant: delete `role_permissions` for the tenant's
  owned roles, delete the tenant's owned `roles`, delete the tenant's owned `permissions`. Without
  this the `fk_roles_tenant` / `fk_permissions_tenant` FKs would block the delete.

## Tenant-plane service rules

The services stay plane-agnostic where possible; the **owner is a parameter**, so the platform plane
passes the value from the request and the tenant plane passes the caller's tenant:

- `RoleService.create(RoleRequest, UUID owner, Set<String> actorPermissions)` — `owner` is the row's
  `tenant_id` (`null` on the platform plane when the request omits it; the caller's tenant on the
  tenant plane, and a `tenantId` in the body there is ignored/rejected).
- **Escalation guard (tenant plane only)**: every requested permission code must be in
  `actorPermissions` — otherwise `ValidationException("Cannot grant a permission you do not hold: …")`.
  The wildcard is never grantable. On the platform plane `actorPermissions` is the platform admin's
  set (which contains `*`), so the check is a no-op there.
- **Ownership guard**: `update`/`delete` take the caller's owner; a row owned by a different owner is
  a `NotFoundException` (a tenant cannot see another tenant's row), a **global** row addressed from the
  tenant plane is an `AccessDeniedException` (it is visible, just immutable to a tenant).
- `RoleService.list(owner)` / `PermissionService.list(owner)` reuse the same owner-aware filter
  (phase 4 passes the platform plane's `?tenantId=` or the caller's tenant).
- `TenantService.delete` also removes the tenant's owned `role_permissions`, `roles` and `permissions`
  (phase 2's FKs would otherwise block it), before the existing `user_roles` + tenant deletes.

## TenantService

- `create(TenantRequest)`: in the **same transaction** as the tenant insert, seed the tenant's admin
  role — code `admin` (a fixed constant; `PermissionCatalog` gains `TENANT_ADMIN_ROLE = "admin"` next
  to `PLATFORM_ADMIN_ROLE`), `scope = TENANT`, `tenant_id = <new tenant>`, granted the **read/write
  level only** of the global `TENANT`-scope catalog: derive the codes from `PermissionCatalog.PERMISSIONS`
  by taking every `TENANT`-scope entry whose code ends in the read/write level
  (`Access.READ_WRITE.suffix()`) — that is `tenant:user:read-write`, `tenant:role:read-write` and
  `tenant:permission:read-write` today, and a future `TENANT`-scope resource joins automatically.
  **Never the `:read-only` code**: a read/write grant already satisfies every read check
  (`PermissionCatalog.acceptedCodes(resource, READ_ONLY)` is why granting both would be redundant).
  Idempotent: a retried create or a second bootstrap must not duplicate it (`onConflictDoNothing` on
  `(code, tenant_id)` plus the `role_permissions` conflict target). Factor it as a reusable
  `ensureTenantAdminRole(tx, tenantId, now)` so the bootstrap can call it for tenants that already exist.
- `update(...)`: nothing to do for the role — the code is the fixed `admin`, never derived from the
  slug, so a rename leaves it alone.
- `delete(...)`: as before, plus the tenant's owned roles/permissions (see below). The seeded admin
  role is deleted with the tenant.

## Tenant admin role protection

- `RoleService.delete`/`update` must refuse the **seeded admin roles** on both planes, by a predicate
  over `(code, tenant_id)`: `platform-admin` when the row is global, `admin` when the row is
  tenant-owned. No schema change is needed — the code is stable by construction because it cannot be
  renamed.
- Today `platform-admin` is deletable by anyone holding `platform:role:read-write`, which locks the
  platform out; this closes it.
- A tenant admin may still create *other* roles and edit their grants freely; only the seeded
  administrative role is immutable, so a tenant cannot lose its last admin by accident.

## Tenant-plane user rules (UserService)

`UserService` is already parameterized by `tenantId`; the tenant plane just passes the **caller's**
tenant and never a request value:

- `list(callerTenant)` and `create(request, callerTenant)` (the request's `tenantId` is **forced** to
  the caller's), plus `update(id)` / `delete(id)` / `resetPassword(id)`: each must first confirm the
  target user belongs to the caller's tenant — otherwise `NotFoundException` (never a hint that
  another tenant's user exists).
- `replaceRoles` keeps the "own tenant or global `TENANT`" rule and adds the **escalation** rule: the
  caller may only assign roles whose permission set it holds (phase 3 / `plan.md` section C). A tenant
  admin therefore cannot mint a peer with more power than itself.
- `POST` still creates the Supabase Auth identity through the service-role client (a tenant admin
  never sees the key); `resetPassword` keeps the "not your own account — use `/me/password`" rule.

## Shared ownership normalization

A small helper keeps the rule in one place (e.g. `Owner` in `…/admin/identity/` or a static method on
each service): `null`/absent → global; the reserved platform id → `ValidationException`; anything else
→ that tenant id (existence checked by the FK, and by a tenant lookup where a clear `404` is wanted).

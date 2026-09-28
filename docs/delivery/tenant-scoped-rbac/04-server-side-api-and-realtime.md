# Phase 4 — Server-side API

> **Executed 2026-09-27.** Done as specified: the platform-plane handlers take a `CallerScope` plus an
> optional `?tenantId=` (parsed with the new `Ids.optionalUuid`), and the three tenant handlers exist and
> are registered in `AdminModule`. One deviation: instead of a separate `CallerTenant` type,
> `PermissionGuard` owns `callerScope(ctx)` — resolving the caller's tenant from their `users` row and
> their permissions **in that tenant's context**, cached per request — and `callerTenantScope(ctx)`,
> which refuses a platform caller on a tenant route (`403`). Keeping it in the guard also preserves the
> preset-attribute short-circuit the existing `PermissionGuardTest` relies on.

**Scope** — expose the owner on the existing platform-plane endpoints **and** add the tenant
self-service plane (routes, a tenant-aware guard, and caller-tenant resolution).

**Artifacts**

| File | Change |
| --- | --- |
| `…/admin/role/RoleHandler.java` | `GET /api/v1/roles?tenantId=` → `service.list(tenantId)` (parse via `Ids.uuid`, absent = `null`) |
| `…/admin/permission/PermissionHandler.java` | `GET /api/v1/permissions?tenantId=` → `service.list(tenantId)` |
| `…/admin/user/TenantUserHandler.java` (new) | tenant-plane user routes (`/api/v1/tenant/users`, incl. `PUT …/roles` and `PUT …/password`) |
| `…/admin/role/TenantRoleHandler.java` (new) | tenant-plane role routes (`/api/v1/tenant/roles`) |
| `…/admin/permission/TenantPermissionHandler.java` (new) | tenant-plane permission routes (`/api/v1/tenant/permissions`) |
| `…/admin/auth/PermissionGuard.java` | resolve effective permissions in the **caller's tenant context** — today `requireAny` calls `resolver.resolve(sub, null)`, which would `403` every tenant user whose role comes from their tenant |
| `…/admin/AdminModule.java` | bind + register the two new handlers |
| DTOs | `tenantId` serialized automatically (Jackson record component; `null` = global) |

**Dependencies** — phase 3 (services accept the filter and return the field).

**Verification** — `AdminIntegrationTest` (Docker/CI) exercises the routes; the JSON assertions in
phase 7 pin `"tenantId":null` for global rows and a tenant id for an owned row.

## Route behaviour

- `POST /api/v1/roles`, `PATCH /api/v1/roles/{id}`, `POST /api/v1/permissions`,
  `DELETE /…/{id}` — unchanged signatures; the owner travels in the request body (`RoleRequest` /
  `PermissionRequest` gained `tenantId`).
- `GET /api/v1/roles`, `GET /api/v1/permissions` — optional `?tenantId=` query parameter, read with
  `ctx.queryParam("tenantId")` and parsed with the existing `Ids.uuid` helper, exactly like
  `GET /api/v1/users`:
  ```java
  String tenantId = ctx.queryParam("tenantId");
  ctx.json(service.list(tenantId == null ? null : Ids.uuid(tenantId)));
  ```
- Guards on the platform plane stay `guard.requireRead(ctx, PLATFORM_ROLE)` / `requireWrite(...)`.

## Tenant self-service plane

- New routes (table in `plan.md`): `/api/v1/tenant/users`, `/api/v1/tenant/roles` and
  `/api/v1/tenant/permissions` — users list/create/update/delete + `PUT …/roles` + `PUT …/password`,
  roles and permissions list/create/update/delete — guarded by `requireRead`/`requireWrite` with the
  catalog resources `TENANT_USER`, `TENANT_ROLE` and `TENANT_PERMISSION`.
- New handler next to the existing ones: `…/admin/user/TenantUserHandler.java`, plus the
  `TenantRoleHandler` / `TenantPermissionHandler` above. They mirror their platform-plane counterparts
  but take the tenant from the caller and call the service with it.
- **The tenant is the caller's, never a parameter.** The handler resolves it through
  `CallerTenant.of(principal(ctx).subject())`; the path carries no `tenantId`, and one supplied in a
  tenant-plane body is rejected (`422`).
- **The guard must be tenant-aware.** `PermissionGuard.requireAny` currently resolves with
  `resolver.resolve(sub, null)`; a tenant user's permissions come from `user_roles` rows carrying
  their tenant, so a tenant-plane check must resolve with the caller's tenant. Resolve that tenant
  once per request and cache it alongside the existing `PERMISSIONS_ATTRIBUTE`, so a check costs no
  extra query per call site.
- **Escalation**: the handler passes the caller's resolved permission set into the service, which
  refuses to grant a permission the caller does not hold, and never the wildcard (phase 3).
- A platform user (no tenant) hitting any `/api/v1/tenant/*` route is refused (`403`).

## Realtime

Not involved — the admin console has no Realtime channels, and nothing here broadcasts.

## Errors

No handler change: `ValidationException` → `422`, `ConflictException` → `409`, `NotFoundException` →
`404`, `AccessDeniedException` → `403`, all through the single `ProblemDetailMapper`.

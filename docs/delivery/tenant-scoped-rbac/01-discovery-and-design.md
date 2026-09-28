# Phase 1 — Discovery & design

**Scope** — fix the ownership model, its invariants, visibility and contract changes. No code in
this phase; the decisions here are confirmed before phase 2.

**Artifacts** — this document (+ the open questions in `plan.md`).

**Dependencies** — none.

**Verification** — the user confirms the open questions in `plan.md`; phases 2–8 execute against
that agreed model.

## Model

Two orthogonal axes on `roles` and `permissions`:

| Axis | Column | Values | Meaning |
| --- | --- | --- | --- |
| **What it is about** | `scope` | `PLATFORM` \| `TENANT` | cross-tenant capability vs. within-one-customer capability (unchanged) |
| **Who defines it** | `tenant_id` | `NULL` \| tenant id | global/platform-defined vs. owned by one tenant (**new**) |

Invariants:

1. `tenant_id IS NULL` → the global catalog (seeded at bootstrap). `tenant_id = <id>` → owned by that
   tenant.
2. **A tenant-owned row must be `TENANT` scope:** `CHECK (tenant_id IS NULL OR scope = 'TENANT')`.
   A tenant must not own a cross-tenant (`platform:*`) capability.
3. `code` is unique **within an owner**: `UNIQUE (code, tenant_id)` for tenant rows, plus a partial
   unique index `(code) WHERE tenant_id IS NULL` for the global pool. (A plain
   `UNIQUE (code, tenant_id)` would allow duplicate global codes: PostgreSQL treats `NULL`s as
   distinct in unique indexes.)
4. A role may only be assigned in its own plane: `user_roles` rows for a tenant-owned role must carry
   that same tenant; a global role follows `scope` (existing rule). Enforced in the service, not by a
   `CHECK` (cross-table).

## The per-tenant admin role

Mirroring `platform-admin`, each tenant gets a seeded administrative role, created **with the tenant**
(same transaction), idempotently:

| Plane | Role code | `tenant_id` | `scope` | Granted |
| --- | --- | --- | --- | --- |
| platform | `platform-admin` | `NULL` | `PLATFORM` | `*` |
| any tenant | `admin` | that tenant | `TENANT` | the **read/write level only**: `tenant:user:read-write`, `tenant:role:read-write`, `tenant:permission:read-write` |

- **Grants are the read/write level only.** A read/write grant already satisfies read, so a
  `:read-only` code would be redundant. The **catalog** still seeds both levels per resource — this rule
  is about the admin role's grants, not the catalog.
- It is a **tenant-owned** row, so only that tenant's users can hold it and other tenants never see it.
- Its grants are **global** catalog permissions (owned by no one) — the "global rows are usable
  everywhere" rule — not tenant-owned permissions.
- The platform admin assigns it to the tenant's **first** user; that user is then the **tenant admin**
  and can create further users, roles and permissions inside the tenant.
- The code is the **fixed `admin`** for every tenant (`code` is unique *per owner*, so `tenant_id`
  separates them), so a tenant rename never touches the role. The seeded admin roles are **protected**
  from rename/delete on both planes.

## Visibility

| Plane | `GET /roles`, `GET /permissions` with | Returns |
| --- | --- | --- |
| platform | no `tenantId` | everything (global + all tenants') — the platform overview |
| platform | `tenantId=<reserved platform id>` | global only (`tenant_id IS NULL`) |
| platform | `tenantId=<tenant id>` | global + that tenant's own rows |
| tenant self-service | (no parameter — the caller's tenant is implicit) | global + the caller's own rows |

A tenant may only **mutate its own** rows; a global row is read-only to it (`403` on
`PATCH`/`DELETE`). `PermissionResolver` (effective permissions) needs **no rule change**: it already
filters `user_roles.tenant_id IS NULL OR user_roles.tenant_id = :tenantContext`, and invariant 4 makes
a tenant-owned role reachable only from its own tenant's `user_roles` row.

## Contracts

- **Wire additions** (additive, so an older client is unaffected):
  - `RoleDto` / `PermissionDto`: `tenantId` (nullable, `null` = global).
  - `RoleRequest` / `PermissionRequest`: `tenantId` (nullable; omitted/`null` = create a global row).
  - `GET /api/v1/roles?tenantId=`, `GET /api/v1/permissions?tenantId=` — optional filter
    (mirrors `GET /api/v1/users?tenantId=`).
- **Tenant self-service routes** (new): `/api/v1/tenant/users`, `/api/v1/tenant/roles` and
  `/api/v1/tenant/permissions` (see `plan.md`), guarded by `tenant:user:*`, `tenant:role:*` and
  `tenant:permission:*`. Every operation is confined to the caller's tenant — including the Auth
  identity a new tenant user needs, created through the same service-role Supabase client the platform
  plane uses.
- **Tenant context is not in the token**: `Principal` carries only `sub`, so the caller's tenant is
  resolved from `users.tenant_id`. A caller with no tenant (a platform user) is refused on the tenant
  plane, and the path carries no `tenantId`, so a tenant admin cannot address another tenant.
- **Escalation guardrail (tenant plane)**: a tenant admin may only grant permissions it **already
  holds**, and never the wildcard `*` (§9.5 "grant only what you hold").
- **Platform plane unchanged**: all platform-plane routes keep their `platform:role` /
  `platform:permission` guards, and it remains the only plane that creates or edits global rows.
- **Errors** — unchanged mapper: unknown/other-tenant role or permission → `ValidationException`
  (`422`); a duplicate `(code, owner)` → `ConflictException` (`409`); missing row → `404`.

## Acceptance criteria

1. Creating a role with `tenantId` omitted stores `tenant_id = NULL`; the `platform-admin` seed and
   the whole seeded permission catalog stay global.
2. Two tenants can each define a role with the same `code` and different grants; each tenant's list
   shows only the global roles plus its own.
3. `POST /api/v1/roles` with a `tenantId` that is the reserved platform id is rejected (`422`) — the
   platform plane is addressed by omitting the field, and no row may be owned by it.
4. A tenant-owned role cannot be granted a permission owned by another tenant, and cannot be granted
   a `PLATFORM`-scope permission (`422`).
5. `PUT /api/v1/users/{id}/roles` rejects a role owned by another tenant (`422`), and a tenant user
   still cannot receive a global `PLATFORM` role (existing rule preserved).
6. Deleting a tenant removes its owned roles/permissions (and their `role_permissions`) before the
   tenant row, so the FK no longer blocks the delete; a tenant that still has users is still refused
   (`409`).
7. A platform user's effective permissions (`GET /api/v1/me`) are unchanged.

### Tenant self-service

8. A tenant user holding `tenant:role:read-only` gets `200` on `GET /api/v1/tenant/roles` and `403`
   on `POST`/`PATCH`/`DELETE`; with `tenant:role:read-write` those succeed.
9. `GET /api/v1/tenant/roles` returns the global catalog **plus the caller's own** roles — never
   another tenant's, even when the caller guesses an id (the tenant is not a parameter).
10. A tenant admin creating a role with a permission it does not itself hold is rejected (`422`); the
    wildcard `*` is rejected (`422`).
11. A tenant admin gets `403` mutating a **global** role/permission, and `404` on another tenant's id
    (a row it cannot see).
12. A platform user (no tenant) calling any `/api/v1/tenant/*` route is refused (`403`).
13. The same role `code` can exist in two tenants; each tenant sees only its own plus the global set.

### Tenant admin role and tenant user management

14. Creating a tenant seeds exactly one `admin` role owned by it whose grants are **exactly**
    `tenant:user:read-write`, `tenant:role:read-write` and `tenant:permission:read-write` — **no
    `:read-only` code** and no `PLATFORM` permission; a retried create or a second bootstrap does not
    duplicate it.
15. The platform admin can assign that `admin` role to the tenant's first user; that user's `GET /me`
    shows the tenant permissions, and their `tenantId` is the tenant.
16. As that tenant admin, `POST /api/v1/tenant/users` creates a user **in their tenant** (whatever
    `tenantId` the body claims), the user appears in `GET /api/v1/tenant/users`, and their roles can be
    replaced with own-tenant or global `TENANT` roles only.
17. That tenant admin cannot read, edit, delete or password-reset a user of another tenant (`404`),
    and cannot assign a `platform:*`-scope role or another tenant's role (`422`).
18. The seeded `platform-admin` and each tenant's `admin` cannot be renamed or deleted from **either**
    plane (rejected), while every non-seeded role stays fully editable.

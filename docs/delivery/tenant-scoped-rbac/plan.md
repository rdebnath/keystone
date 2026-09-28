# Delivery Plan — Tenant-scoped Roles & Permissions (+ tenant self-service)

**Feature slug:** `tenant-scoped-rbac`
**Level:** Platform-level — touches `platform/keystone-admin` (schema, domain, API),
`platform/keystone-admin-ui` (Flutter) and `docs/`.

## Sizing decision

**Produce a plan.** The change adds an owner column (and constraints/indexes) to the two core RBAC
tables, rewrites every role/permission lookup (`code` stops being globally unique), adds a whole
second **tenant self-service** admin plane (tenant context + tenant-aware guard + new routes),
changes the REST payload shape, and is security-relevant (it decides who may grant what to whom).

## Summary

Today `roles` and `permissions` have **no `tenant_id`**: every role and permission is global, and
`roles.code` / `permissions.code` are **globally unique**. `roles.scope` (`PLATFORM`|`TENANT`) says
what a role is *about*, not who owns it — so it is impossible to have two tenants with a role called
`manager` with different grants.

Three changes ship together.

### A. Optional ownership on roles and permissions

| `tenant_id` | Meaning | Visible to | Assignable to |
| --- | --- | --- | --- |
| `NULL` | **global** — the platform-defined catalog, seeded at bootstrap | the platform plane **and every tenant** | per `scope` (unchanged: a `PLATFORM` role never goes to a tenant user) |
| a tenant id | **owned by that tenant** | that tenant **and** the platform plane | that tenant's users only |

`scope` (what the row is about) and ownership (who defines it) stay **orthogonal**, with one
guardrail: **a tenant-owned row must be `TENANT` scope** — a tenant must never own a cross-tenant
`platform:*` capability.

| id | code | scope | tenant_id | Meaning |
| --- | --- | --- | --- | --- |
| … | `platform-admin` | `PLATFORM` | `NULL` | global; wildcard; the platform plane |
| … | `tenant-admin` | `TENANT` | `NULL` | global catalog role — any tenant may assign it |
| … | `manager` | `TENANT` | `acme-id` | Acme's own role — invisible to Globex |
| … | `manager` | `TENANT` | `globex-id` | Globex's own role — same code, different owner |

### B. A per-tenant admin role (code `admin`)

Just as `platform-admin` is the platform's administrative role, **every tenant gets its own admin
role**, seeded when the tenant is created:

| Plane | Seeded role | Owned by | Scope | Granted |
| --- | --- | --- | --- | --- |
| platform | `platform-admin` | global (`tenant_id IS NULL`) | `PLATFORM` | `*` (see open question 4) |
| any tenant | `admin` | that tenant | `TENANT` | the **read/write level only**: `tenant:user:read-write`, `tenant:role:read-write`, `tenant:permission:read-write` |

The code is the **fixed `admin`** for every tenant: `code` is unique *per owner*, so `tenant_id` tells
the tenants apart, and a tenant rename never touches the role.

**Read/write only — never the `:read-only` code.** A `:read-write` grant already satisfies every read
check (the guard accepts `:read-only` *or* `:read-write`), so granting the read-only code alongside it
is redundant. The tenant set is derived by filtering the catalog (`scope == TENANT`, level ==
read/write), so a future `TENANT`-scope resource extends the seeded admin role automatically.

Note this is about what the **admin roles are granted** — the **catalog** keeps seeding both levels per
resource, because an operator composing some other role may well want a read-only one.

A holder of that `admin` role is a **tenant admin**: they create and manage **users, roles and
permissions inside their own tenant**, and nothing outside it. The platform admin still creates the
tenant and its **first** tenant admin (assigning the seeded role to that first user); after that the
tenant runs itself.

### C. Tenant self-service routes

A second admin plane so a tenant admin can manage **its own** users, roles and permissions:

```
GET    /api/v1/tenant/users                 tenant:user:read-only
POST   /api/v1/tenant/users                 tenant:user:read-write
PATCH  /api/v1/tenant/users/{id}            tenant:user:read-write
DELETE /api/v1/tenant/users/{id}            tenant:user:read-write
PUT    /api/v1/tenant/users/{id}/roles      tenant:user:read-write
PUT    /api/v1/tenant/users/{id}/password   tenant:user:read-write

GET    /api/v1/tenant/roles                 tenant:role:read-only
POST   /api/v1/tenant/roles                 tenant:role:read-write
PATCH  /api/v1/tenant/roles/{id}            tenant:role:read-write
DELETE /api/v1/tenant/roles/{id}            tenant:role:read-write

GET    /api/v1/tenant/permissions           tenant:permission:read-only
POST   /api/v1/tenant/permissions           tenant:permission:read-write
DELETE /api/v1/tenant/permissions/{id}      tenant:permission:read-write
```

The **tenant is never taken from the request** — it comes from the authenticated caller
(`users.tenant_id`), so a tenant admin cannot reach another tenant (no cross-tenant IDOR). A platform
user (or any caller with no tenant) is refused on these routes. A tenant sees the global catalog plus
its own rows and may only mutate **its own**; global rows are read-only to it. Guardrails:

- **grant only what you hold** — a tenant admin may not grant a role or permission it does not itself
  hold, and never the wildcard `*`;
- **no self-lockout** — the seeded admin roles (`platform-admin`, and each tenant's `admin`) cannot be
  renamed or deleted through these routes.

The platform plane (`/api/v1/roles`, `/api/v1/permissions`, `/api/v1/users`) keeps working exactly as
today and remains the only plane that creates or edits global rows.

## Phase list

1. Discovery & design — `01-discovery-and-design.md`
2. Database changes — `02-database-changes.md`
3. Domain & application services — `03-domain-and-application-services.md`
4. Server-side API (platform plane **and** tenant self-service) — `04-server-side-api-and-realtime.md`
5. Frontend / UI (Flutter) — `05-frontend-ui-flutter.md`
6. Security & observability — `06-security-and-observability.md`
7. Testing — `07-testing.md`
8. Delivery — `08-delivery.md`

Realtime is not involved (the admin console has no Realtime channels today).

## Resolved decisions (from the user, 2026-09-27)

| # | Decision |
| --- | --- |
| 1 | **Both** roles and permissions gain optional tenant ownership. |
| 2 | **Tenant self-service routes are in scope** (not deferred to a later feature). |
| 3 | `tenant_id IS NULL` = **global**: applicable to the platform plane **and all tenants**, still subject to `scope`. A `PLATFORM`-scope row is never assignable to a tenant user (unchanged rule). |
| 4 | Deleting a tenant explicitly cleans up its owned roles/permissions (and their grants) before the tenant row; a tenant that still has users is still refused (`409`). |
| 5 | **Indexes are part of the change** — every new access path gets an index justified by the query it serves (phase 2). |
| 6 | **Every tenant gets a seeded `admin` role** (like `platform-admin`), owned by that tenant, granted the **read/write** `TENANT`-scope codes. **A tenant admin creates tenant-specific users and tenant-specific roles** — so the tenant-plane *users* routes are in scope too. |
| 7 | The tenant admin role's **code is the fixed `admin`**, not `<slug>-admin` (`code` is unique per owner, so `tenant_id` disambiguates) — so a tenant rename never has to touch the role. |
| 8 | The **seeded admin roles are protected**: `platform-admin` and each tenant's `admin` cannot be renamed or deleted from either plane (closes the live lockout gap). |
| 9 | **The tenant console is built**, reusing `platform/keystone-admin-ui` as a library (option b) so nothing is duplicated: the same shell/screens driven by a console abstraction, with the hosting app routing tenant users into it. |

## Open questions (remaining — minor defaults)

1. **`user_roles` primary key / scope `CHECK`** — `docs/ARCHITECTURE.md` §9.4 claims a
   `(user_id, role_id, tenant_id)` PK and a `user_roles` scope `CHECK` that the changelog never
   created (the real PK is `(user_id, role_id)`). Kept as-is, and the **doc corrected**; raise it
   separately if you want the schema aligned instead.
2. **A global row from the tenant plane is read-only** — `PATCH`/`DELETE` of a global role/permission
   by a tenant. *(default: `403` — the tenant can already see the row, so a `404` would mislead)*
3. **Which tenant users land in the tenant console** — a tenant user holding none of
   `tenant:user|role|permission:<level>` keeps going to the app UI (inventory) exactly as today, so an
   ordinary inventory user is unaffected; only a tenant admin sees the console. *(default: as
   described — say if you would rather every tenant user land in the console even when it is empty)*
4. **`platform-admin`'s grant: keep the wildcard `*`?** The tenant `admin` role is now read/write-only
   by construction. `platform-admin` holds `*`, which contains no `:read-only` code at all — so it does
   not break the read/write-only rule, but it *does* make the platform admin's access open-ended.
   Replacing `*` with the four explicit `platform:<resource>:read-write` codes would make its grant
   symmetric with the tenant role and auditable, **but** the platform admin would then lose access to
   anything added to the catalog later (a new `platform` resource, or an app's `inventory:*` codes)
   until its role is edited by hand — the wildcard is exactly what makes it future-proof, and
   `docs/ARCHITECTURE.md` §9.3 documents it as held only by `platform-admin`.
   **RESOLVED (user, 2026-09-27): keep `*`.**

## Confirmation state

- [x] Design questions answered (see *Resolved decisions*) — 2026-09-27.
- [x] Tenant admin role + tenant-plane **users** routes scoped in — 2026-09-27.
- [x] **Plan confirmed** — fixed `admin` role code, seeded admin roles protected, tenant console built in
      the shared UI library, `platform-admin` keeps the wildcard, admin grants read/write-only
      (2026-09-27).
- [x] Phases 2, 3, 4, 5, 6, 8 executed; phase 7 largely (schema, tenant-plane HTTP and console-routing
      tests).
- [ ] Nothing outstanding except optional service-level slices and the manual walk-throughs.

## Execution summary (2026-09-27)

**Phase 2 — database.** `0003-tenant-scoped-rbac.xml` (3 changesets) + master include: owner columns with
FKs, per-owner uniqueness, the `TENANT`-scope `CHECK`s and the five indexes. Offline jOOQ codegen
regenerated `roles.tenant_id` / `permissions.tenant_id`.

*Deviation:* the global `UNIQUE (code)` could not be dropped by its PostgreSQL-generated name (`DROP
CONSTRAINT roles_code_key`) because the H2-backed offline `DDLDatabase` names it differently and fails
hard. `DROP CONSTRAINT IF EXISTS` works in both: it drops it on PostgreSQL and is a harmless no-op in the
codegen renderer. The partial unique index is accepted by the renderer, so no fallback was needed.

**Phase 3 — domain & services.** `PermissionCatalog` gained `TENANT_ADMIN_ROLE = "admin"`,
`tenantAdminGrants()` (derived: `TENANT` scope + read/write level) and `isSeededAdminRole(code, owner)`;
new `identity.CallerScope` (plane + tenant + permissions, with `requireGrantable`/`holds`), `identity.Owners`,
and a shared `role.RoleSeeder` (`ensurePlatformAdminRole`, `ensureTenantAdminRole`) used by both the
bootstrap and tenant creation. `RoleService`/`PermissionService`/`UserService` are owner-aware;
`TenantService.create` seeds the tenant's `admin` role in the same transaction and `delete` cleans up the
tenant's owned rows; `BootstrapRunner` delegates role seeding, is scoped to the global partition, and
re-assures every existing tenant of its admin role.

**Phase 4 — API.** Platform-plane handlers take a `CallerScope` and an optional `?tenantId=`; new
`TenantRoleHandler`, `TenantPermissionHandler`, `TenantUserHandler`; `PermissionGuard` owns
`callerScope(ctx)` (tenant-aware resolution, cached per request) and `callerTenantScope(ctx)` (refuses the
platform plane).

*Deviation from the plan:* no separate `CallerTenant` class — `PermissionGuard.callerScope` is the single
seam, which keeps the existing preset-attribute short-circuit its HTTP test relies on.

*Two bugs found by the new tests, both fixed:* the escalation check used literal set membership, so an
admin holding `:read-write` could not grant or assign `:read-only` — `CallerScope.holds` now encodes the
same "write implies read" implication the guard uses, and both `requireGrantable` and role assignment use
it.

**Phase 6 — security.** Reviewed; no new logging, metrics or configuration. Additionally protects the
wildcard permission from deletion (deleting it would strip the platform admin until the next bootstrap).

**Phase 7 — testing.** `TenantScopedRbacSchemaTest` (5) pins the `0003` invariants against real
PostgreSQL; `TenantSelfServiceIntegrationTest` (4) walks the tenant plane over HTTP, including that
another tenant's rows are invisible and that the seeded/global roles are immutable.

**Verification:** `mvn -pl platform/keystone-admin -am test` → **73 tests, 0 failures, 0 skipped**
(including the Docker-backed Testcontainers suites).

**Not verified:** the Flutter client (no toolchain here) and the manual walk-throughs in the phase docs.

## Companion change (planned separately)

The request also asked for the **bootstrap admin username/email** to come only from yaml instead of
being hardcoded in Java. That is a small, low-risk config change, so per the delivery skill it is
planned **without** the phase machinery — see `docs/delivery/bootstrap-admin-identity/`.

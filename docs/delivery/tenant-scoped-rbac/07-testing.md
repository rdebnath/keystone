# Phase 7 — Testing

> **Partially executed 2026-09-27.** Written and green against real PostgreSQL (Testcontainers):
> `TenantScopedRbacSchemaTest` (5 — the `0003` schema invariants and indexes) and
> `TenantSelfServiceIntegrationTest` (4 — the tenant plane over HTTP: the seeded `admin` role holds
> read/write only, a tenant admin runs its own tenant, another tenant's rows are invisible, escalation and
> the wildcard are refused, a global role is read-only, tenant shadowing of a catalog code is `409`, and
> the platform plane is refused on tenant routes). The existing suites were extended rather than
> duplicated: `PermissionGuardTest` now constructs the guard with its new `DataAccess` collaborator and
> `BootstrapRunnerTest` with the new `RoleSeeder`.
>
> **Remaining:** the dedicated `RoleServiceTest` / `PermissionServiceTest` / `TenantUserServiceTest` slices
> (their rules are currently covered through the integration tests instead) and every Flutter test, which
> needs a Flutter toolchain (see phase 5).

**Scope** — prove the ownership rules, the per-owner uniqueness and the filter, at unit, slice and
integration level, and keep the existing suites green.

**Artifacts**

| Test | Kind | Covers |
| --- | --- | --- |
| `RoleServiceTest` (new, Testcontainers) | slice | create with/without owner; reserved platform id rejected; duplicate `(code, owner)` → `409`; the same `code` in two tenants; `list(null/reserved/tenant)`; grant of a foreign-tenant permission → `422`; owner immutable on update |
| `PermissionServiceTest` (new, Testcontainers) | slice | same ownership/`(code, owner)` rules; `list` filter |
| `UserServiceTest` (extend) | slice | `replaceRoles` accepts global-`TENANT` and own-tenant roles; rejects another tenant's role (`422`); still rejects a global `PLATFORM` role for a tenant user |
| `TenantService` assertions (extend `AdminIntegrationTest` or a service test) | slice | deleting a tenant removes its owned roles/permissions/grants and still refuses when it has users (`409`) |
| `AdminIntegrationTest` (extend) | integration | HTTP: `GET /api/v1/roles?tenantId=…` scoping; `POST` with `tenantId` returns it in the body; `"tenantId":null` for the seeded catalog; two tenants with the same role code; `PUT /users/{id}/roles` cross-tenant rejection |
| `TenantRoleServiceTest` / `TenantPermissionServiceTest` (new, Testcontainers) | slice | owner-scoped create/list/update/delete; escalation refusal (`422`); global row immutable (`403`); another tenant's id → `404` |
| `PermissionGuardTest` (extend) | HTTP | tenant-aware resolution: a tenant user whose grant lives in their tenant passes the guard; a platform user is refused on `/api/v1/tenant/*` |
| `TenantServiceTest` (new/extend, Testcontainers) | slice | creating a tenant seeds exactly one `admin` role whose grants are **exactly** the three read/write `tenant:*` codes (no `:read-only`, no `PLATFORM`); a retried create is idempotent; a slug change leaves the role alone; deleting the tenant removes the role |
| `RoleServiceTest` (extend) | slice | `platform-admin` and `admin` refuse rename/delete (the pre-existing lockout gap) |
| `TenantUserServiceTest` (new, Testcontainers) | slice | tenant-plane create/list/update/delete/reset confined to the caller's tenant; another tenant's user → `404`; a role the caller does not hold → `422` |
| `TenantApiIntegrationTest` (extend) | integration | the whole tenant-admin story over HTTP: a `admin` holder creates a user, assigns an own-tenant role, resets that user's password, and cannot touch another tenant |
| index assertions (phase 2) | slice | `EXPLAIN` on the container DB shows each new index is chosen (and any unused one is dropped) |
| `PermissionCatalogTest` | unit | unchanged — the seeded catalog stays global (`TENANT_ID` null) |
| `models_test.dart` (extend) | Flutter unit | `Role`/`Permission` parse `tenantId` (present, absent → null) |
| `roles_screen`/`permissions_screen` widget tests (new/extend) | Flutter widget | owner selector filters the list; create sends the chosen owner |
| `user_editor_test.dart` (extend) | Flutter widget | the picker only offers assignable roles |

**Dependencies** — phases 2–5.

**Verification**

- `mvn -pl platform/keystone-admin test` green (Docker required for the Testcontainers slices and
  `AdminIntegrationTest`; they are `disabledWithoutDocker = true`, so a Docker-less run must be
  reported as *skipped*, not green).
- `mvn -q -DskipTests compile` clean repo-wide; `apps/inventory/server` tests still green
  (`BootstrapToolTest` seeds the catalog and must be unaffected).
- `dart format --set-exit-if-changed lib test`, `flutter analyze` clean, `flutter test` green in
  `platform/keystone-admin-ui`.

## Regression focus

- The bootstrap still seeds exactly the flat global catalog and the global `platform-admin` role.
- `GET /me` permissions for the bootstrap admin are unchanged (wildcard).
- `GET /api/v1/roles` **without** a parameter keeps returning everything (existing clients/tests
  unaffected).

# Delivery Plan — Permission Access Levels (read/write · read-only)

**Feature slug:** `permission-access-levels`
**Level:** Platform-level — touches `platform/keystone-admin` (backend), `platform/keystone-admin-ui`
(Flutter) and `docs/`.

## Sizing decision

**Produce a plan.** This changes an authorization contract (the permission catalog and every
guarded route), touches seeded catalog data, application services, all four admin REST handlers, the
Flutter admin UI and the architecture docs, and it changes what existing roles can do. It is not a
single-file tweak.

## Summary

Today each resource is split into four action-granular codes (`create`, `read`, `update`, `delete`,
plus `platform:user:assign-role`) — 30 seeded codes. The target is **two access levels per
resource**:

| Access level | Code | Grants |
| --- | --- | --- |
| **read/write** | `<resource>:read-write` | read + create + update + delete (and role assignment) |
| **read-only** | `<resource>:read-only` | read only |

`write` **implies** `read`: a caller holding only `platform:tenant:read-only` is rejected with `403`
on every mutation. The wildcard `*` still grants everything and is still assigned only to the
`platform-admin` role.

| Resource (catalog constant) | read-only | read/write | read/write covers |
| --- | --- | --- | --- |
| platform tenants (`PLATFORM_TENANT`) | `platform:tenant:read-only` | `platform:tenant:read-write` | create, read, update, delete tenant |
| platform roles (`PLATFORM_ROLE`) | `platform:role:read-only` | `platform:role:read-write` | create, read, update, delete role |
| platform permissions (`PLATFORM_PERMISSION`) | `platform:permission:read-only` | `platform:permission:read-write` | create, read, delete permission |
| platform users (`PLATFORM_USER`) | `platform:user:read-only` | `platform:user:read-write` | create, read, delete user **and** assign roles |
| tenant roles (`TENANT_ROLE`) | `tenant:role:read-only` | `tenant:role:read-write` | create, read, update, delete role |
| tenant permissions (`TENANT_PERMISSION`) | `tenant:permission:read-only` | `tenant:permission:read-write` | create, read, delete permission |
| tenant users (`TENANT_USER`) | `tenant:user:read-only` | `tenant:user:read-write` | create, read, delete user **and** assign roles |

7 resources × 2 levels = **14 codes + `*`** (down from 30).

**Not released yet** — fresh development: no back-compatibility obligation, no legacy codes to carry
over and no data cleanup. A dev/demo database that predates the two levels can simply be reset with
`SchemaTool reset`.

## Phase list

1. Discovery & design — `01-discovery-and-design.md`
2. Database changes — **skipped.** No DDL/schema change: permission rows are seeded at bootstrap
   (application-side, phase 3). Keeping the changelog DDL-only keeps the offline jOOQ codegen
   deterministic (`platform/keystone-admin/pom.xml` renders the changelog to DDL through jOOQ's
   `DDLDatabase`).
3. Domain & application services — `03-domain-and-application-services.md`
4. Server-side API & realtime — `04-server-side-api-and-realtime.md`
5. Frontend / UI (Flutter) — `05-frontend-ui-flutter.md`
6. Security & observability — `06-security-and-observability.md`
7. Testing — `07-testing.md`
8. Delivery — `08-delivery.md`

## Resolved decisions (proposed — confirm or change)

1. **Two codes per resource, level carried by the code suffix** (`:read-only`, `:read-write`). No
   schema change and no REST contract change: roles keep `permissions: string[]`
   (`RoleDto`/`RoleRequest`) and `/api/v1/me` keeps `permissions: string[]` (`MeDto`).
2. **`write` implies `read`** — the read check accepts `<resource>:read` **or** `<resource>:write`.
3. **Guard API** — `PermissionGuard.requireRead(ctx, resource)` / `requireWrite(ctx, resource)`,
   with resource names as constants on `PermissionCatalog` (beside the existing `WILDCARD` /
   `PLATFORM_ADMIN_ROLE`), plus a pure `grants(permissions, acceptedCodes)` helper so the decision
   logic is unit-testable without a database.
4. **New `Access` enum** (`READ_ONLY`, `READ_WRITE`) in `platform.admin.identity`, sibling of
   `Scope`, owning the two level names and their code suffixes (and the same
   `ValidationException`-style validation message as `Scope`).
5. **`assign-role` folds into `:read-write`** — assigning roles is a user mutation, so
   `platform:user:read-write` covers `PUT /api/v1/users/{id}/roles`.
6. **No legacy or upgrade path at all** (user decision, 2026-09-27) — this is fresh development on an
   unreleased platform, so there are no replaced codes to carry over or clean up: the catalog simply
   seeds the two levels per resource, and no prune/migration code exists for old codes.
7. **API-created permissions must use one of the two levels** — `PermissionService.create` validates
   that the code ends in `read-only` or `read-write`, so "only two types" cannot be bypassed through
   `POST /api/v1/permissions`.
8. **The catalog keeps both levels per resource (14 rows + `*`) and `platform-admin` keeps the `*`
   grant** — confirmed by the user (2026-09-27): read/write implies read, but the read-only rows stay
   so other roles can be granted read-only. The `*` row is displayed as read/write in the admin UI
   (it grants everything) rather than as an unknown level.

## Decisions taken (were open questions)

1. **Code suffix** — **`:read-only` / `:read-write`** (user decision; the literal level names).
   Note this diverges from the existing `inventory:item:write` example, which `docs/ARCHITECTURE.md`
   §9.3 is updated to match (Phase 8).
2. **Existing role grants** — **nothing to do** (fresh development, unreleased platform): no replaced
   codes exist and no cleanup code was written.
3. **Guard signature** — `requireRead` / `requireWrite(ctx, resourceConstant)` (default kept; no
   objection raised).
4. **`assign-role` granularity** — folded into `platform:user:read-write` (default kept).
5. **Permissions screen UX** — Resource + Type dropdown composing the code (default kept).

## Confirmation state

- [x] Plan reviewed — design confirmed with two amendments (user): codes are **`:read-only` /
      `:read-write`**, and there is **no migration or cleanup of any kind** ("remember this is not
      released yet").
- [x] **Phase 3 & 4 executed** (2026-09-27) — see the execution summary below.
- [x] **Phases 5–8 executed** (2026-09-27) — the feature is delivered; see the summary below.
- [ ] Nothing outstanding except a Docker/CI run of `AdminIntegrationTest`.

## Execution summary

- **Phase 3** — `Access` added (`READ_ONLY("read-only")`, `READ_WRITE("read-write")`, `suffix()`,
  `suffixes()`); `PermissionCatalog` rewritten (7 resource constants, `RESOURCES`, `PERMISSIONS =
  seeds()` → 14 codes + `*`, `code()`, `acceptedCodes()`, `hasAccessLevel()`); `PermissionGuard` now
  exposes `requireRead` / `requireWrite` and the pure `grants(...)` helper (the old `require(ctx,
  code)` is removed); `BootstrapRunner` is unchanged (its seed loop already inserts the catalog);
  `PermissionService` rejects a code that does not end in a level.
- **Follow-up (2026-09-27)** — on the user's instruction the bootstrap's legacy-code prune was removed
  entirely: this is fresh development, so nothing is carried over or cleaned up. `PermissionCatalog`
  has no legacy list, `BootstrapRunner` has no prune method, and `PermissionCatalogTest` now pins the
  exact fresh catalog (14 level codes + `*`).
- **Phase 4** — all 15 guarded call sites in `TenantHandler`, `RoleHandler`, `PermissionHandler` and
  `UserHandler` switched to `requireRead` / `requireWrite` with the catalog resource constants
  (`PUT /users/{id}/roles` now needs `platform:user:read-write`).
- **Phase 3 and 4 were delivered together**: the guard's string-code API, its 15 call sites and the
  catalog it checks against are one atomic change — an intermediate state would guard routes with
  codes the catalog no longer seeds.
- **Phase 5** — `PermissionAccess` (`read-only` / `read-write`) + derived `Permission.access` added to
  `keystone-admin-ui`; the permissions dialog is now Resource + Type (scope derived, live code
  preview, level shown in the list); freezed output regenerated; `dart format`, `flutter analyze`
  clean, `flutter test` 9/9.
- **Phase 6** — deny path, level order, wildcard, fail-closed and the absence of any cleanup/grant
  derivation path reviewed; no new logging, metrics or configuration.
- **Phase 7** — `PermissionCatalogTest` (6), `PermissionGuardTest` (5 HTTP cases), `AccessTest` (2),
  `AdminIntegrationTest` extended with the catalog assertion.
- **Phase 8** — `CHANGELOG.md` (Unreleased → Changed) and `docs/ARCHITECTURE.md` §9.3 updated;
  `README.md` verified as needing no change.
- **Verification** — `mvn -q -DskipTests compile` clean repo-wide; `mvn -pl platform/keystone-admin
  test` → **45 tests, 0 failures, 1 skipped** (`AdminIntegrationTest`, `disabledWithoutDocker` — no
  Docker here, so its new catalog assertion still needs a Docker/CI run); `flutter analyze` clean and
  `flutter test` 9/9 in `platform/keystone-admin-ui`.

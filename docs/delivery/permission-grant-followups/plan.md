# Delivery Plan — The permission-grant follow-ups: an `access` filter, and a global role's catalogue

**Feature slug:** `permission-grant-followups`
**Level:** **Platform-level** — `platform/keystone-admin` (a new list filter on two routes; a tightened grant
rule), `platform/keystone-admin-ui` (the filter control, and the picker's seed follows the new rule),
`docs/` and `CHANGELOG.md`. No DDL, no new endpoint, no realtime change.

## Sizing decision

**Planned, then executed in one pass** — the two code items were already agreed in writing as the follow-ups of
`docs/delivery/role-permission-picker/08-delivery.md`, and the instruction was "implement point 3", so this file
is the plan *and* the execution log rather than a stop-and-confirm cycle. Not a single-file tweak either: it
changes a **list contract** (a new validated query parameter on both permission routes) and a **grant rule**
that the console then has to mirror.

## What was asked (verbatim from the follow-up list)

1. **An access-level filter** (`read-only` / `read-write`) on the permission list routes — "the one filter that
   would always be available in the picker, and it would serve the Permissions screen too".
2. **A global role may hold a tenant-owned `TENANT`-scope permission** — accepted by the backend
   (`ownerFilter(null)` was `noCondition`) and meaningless to the other tenants sharing the role.
3. **The manual click-through** against a live backend (no dev credentials in this environment).

## What shipped

### 1. `access` — a filter on the code's last segment

| Layer | Change |
| --- | --- |
| `identity/Access` | `from(String)` / `optional(String)` — parses the **code suffix** (`read-only`, `read-write`), not the enum name, and refuses anything else with a `422`. |
| `permission/PermissionService` | `list(…, Access access, …)` + `accessFilter(access)`: absent → no condition, otherwise `PERMISSIONS.CODE.endsWith(":" + suffix)` — a suffix match on the code, so the wildcard (which carries no level) belongs to neither set. |
| both permission handlers | read `?access=` and pass it. |
| `core/lists.dart` | `ListQueryLocation` reads `access` from the URL like `scope`. |
| `models/ListQuery` | an `access` field carried by every mutator (`withSearch`, `withScope`, …), a `withAccess(...)` that returns to page 1, and the parameter in `toQueryParameters()`. |
| `features/admin/owner_filter.dart` | `AccessFilter` — *All levels* / *read-only* / *read/write*. |
| Permissions screen + role picker | the control, on both planes: the level is a property of the **code**, not of the owner, and every row shows it (§1.6). |

### 2. A role may hold the catalogue plus its **own** owner's permissions

`RoleService.grantPermissions` no longer decides "may this caller see it" (`ownerFilter`) but "may this **role**
hold it" (`grantableTo(owner)`): `owner == null` → `tenant_id IS NULL`, otherwise
`tenant_id IS NULL OR tenant_id = owner`. A global role therefore holds the catalogue only — its grants are
handed to every tenant that holds it, so a tenant-owned code there would leak one tenant's permission into
everybody else's roles. The refusal reads
`Unknown permission: <code> (a global role may hold the global catalog only)`.

The console follows, in three places: `ListQuery.permissionsFor` seeds a role with **no** owner from the
reserved platform tenant (`Tenant.platformId`, mirrored from `PlatformSchema.PLATFORM_TENANT_ID`) instead of
from *every* owner; the picker's owner filter is **removed** (with the seed pinned there is nothing left to
narrow, and the control could only have offered choices the role cannot hold); and `PermissionSelection.retaining`
keeps a pick for a global role only when the permission is global.

### 3. The click-through, honestly

The environment has **no** live backend credentials (no Supabase URL / service-role key), so a browser session
against a real deployment is still not possible — that follow-up stays open. What *is* possible here, and was
done instead, is stronger for the parts that matter: **Docker is available**, so the two **Testcontainers
integration suites** now walk the new behaviour over real PostgreSQL and the real Javalin stack — the access
filter on both planes, the `422` for an unknown level, and a tenant-owned permission granted to its own
tenant's role **and refused to a global one**. That is exactly the API contract the console's picker calls.

## Phase list

1. Discovery & design — `01-discovery-and-design.md`
2. Database changes — **skipped.** No DDL: the filter is a `WHERE` on an existing column.
3. Domain & application services — `03-domain-and-application-services.md`
4. Server-side API & realtime — `04-server-side-api-and-realtime.md`
5. Frontend / UI (Flutter) — `05-frontend-ui-flutter.md`
6. Security & observability — `06-security-and-observability.md`
7. Testing — `07-testing.md`
8. Delivery — `08-delivery.md`

## Decisions

1. **The filter's value is the code suffix** (`access=read-only`), not the enum name: that is what a permission
   code ends in, what the console displays, and what its own `PermissionAccess.suffix` already mirrors.
2. **The filter is a suffix match on `code`**, not a new column: a level is derivable from the code, and adding a
   column would be a schema change for information already in the row. A consequence worth stating: the
   wildcard `*` carries no level, so it is in neither level's set.
3. **A level filter on both planes.** It is a property of the code, so a tenant console gets it too.
4. **The grant rule is about the *role's owner*, not the caller's visibility.** Reusing `ownerFilter` there was
   the defect: it answers "what may this caller see", and for a platform caller that answer is "everything".
5. **The picker loses its owner filter** as a *consequence* of decision 4 — recorded because it reverses an
   earlier decision (`role-permission-picker` decision 2), which was correct under the old rule.
6. **The reserved platform-tenant id lives on the `Tenant` model** (`Tenant.platformId`), beside the
   `isPlatform` flag it describes, so `ListQuery` can use it without importing `core/` — which would invert the
   layering (`core/permissions.dart` imports the models).
7. **No new guideline rule.** `docs/UX_GUIDELINES.md` §1.6 already says to filter on what the row shows, and
   §1.17 already says a picker's seed must make every offered choice valid; this delivery is an instance of both.
   `docs/CODING_GUIDELINES_BACKEND.md` §8 gained `access` in its example filter list.

## Execution log

| Phase | State | Date |
| --- | --- | --- |
| 1 — Discovery & design | **done** — the three items restated, the contract and the rule written down, the supersession of the picker's owner filter recognised. | 2026-09-28 |
| 3 — Domain & application services | **done** — `Access.from`/`optional`, `PermissionService.accessFilter` + `list(…)`, `RoleService.grantableTo`; console-side `ListQuery.access`/`withAccess`, `Tenant.platformId`, `PermissionSelection.retaining`. | 2026-09-28 |
| 4 — Server-side API | **done** — `?access=` on `GET /api/v1/permissions` and `GET /api/v1/tenant/permissions`; no DTO, guard, realtime or database change. | 2026-09-28 |
| 5 — Frontend / UI | **done** — `AccessFilter` on the Permissions screen and in the picker; the picker's owner filter removed, its seed pinned to the catalogue for a global role. | 2026-09-28 |
| 6 — Security & observability | **done** — the grant guard reviewed (it narrows a leak, not a permission); no new logging, metric or realtime. | 2026-09-28 |
| 7 — Testing | **done** — backend **103** tests, 0 failures, 0 skipped (`keystone-admin`, real PostgreSQL via Testcontainers); console **138** tests, `flutter analyze` clean. | 2026-09-28 |
| 8 — Delivery | **done** — CHANGELOG entry, backend §8 filter example, supersession recorded in the previous delivery's follow-up list. | 2026-09-28 |

## Open questions

1. **Deploy order:** server first is recommended — the console's `access` parameter is silently ignored by an
   older backend (Javalin ignores an unknown query parameter), so a client-first release is degraded rather
   than broken. See `08-delivery.md`.
2. **A level filter for the *roles* list?** A role has no level of its own; its grants do. Out of scope, and it
   would be a filter whose effect the row does not show (§1.6).
3. **Global roles that already hold a tenant-owned permission** (possible before this guard): an *update* now
   refuses to carry such a grant forward, so the role must be edited to drop it. No row is changed by this
   delivery, and no data cleanup was requested.
4. **The manual click-through** against a live backend remains open (no dev credentials here); the integration
   suites are the substitute, and `08-delivery.md` records the exact commands and results.


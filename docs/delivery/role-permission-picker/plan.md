# Delivery Plan — Grant a role its permissions from the catalogue, not by typing codes

**Feature slug:** `role-permission-picker`
**Level:** **Platform-level** — `platform/keystone-admin-ui` (the console both planes share, so the platform
plane and every tenant console change together), `docs/UX_GUIDELINES.md` (one new normative rule) and
`CHANGELOG.md`. **No** backend, database, REST-contract, hosting-app or deployment change.

## Sizing decision

**Produce a plan.** Not a single-file tweak:

- It replaces an input with a **server-backed picker**, which is a new pattern in the console (a picker
  that is itself a paged list) and needs a shared, reusable implementation rather than a local hack.
- The offered set must **mirror an authorization rule** — what the backend will accept for the role's
  owner + scope (`RoleService.grantPermissions`, `CallerScope.requireGrantable`). Getting that wrong turns
  a picker into a source of `422`s, so the rule is designed (and written down) before any code.
- It adds a **normative UX rule**: `docs/UX_GUIDELINES.md` §1.13 today reads "a picker MUST NOT be fed by a
  paged list". A picker over a large set *is* a paged list, so the guideline needs the missing rule rather
  than a silent violation of the one it has.
- The selection has to survive paging, searching and a change of the role's owner/scope — small, but it is
  state design, not a widget swap.

## Problem

The **Roles** screen's create dialog asks for permissions as free text:

```
Permissions (comma-separated)
tenant:user:read-only, tenant:role:read-write
```

That is the *only* place a permission can be chosen, and it asks the user to recall three things the
console already knows and the server already exposes:

1. **the exact code** — `tenant:role:read-write`, spelled and hyphenated correctly;
2. **the access level** — a code without a level is a `422` (`PermissionCatalog.hasAccessLevel`);
3. **what is grantable at all** — the code has to exist in the catalogue the role's *owner* can use, and its
   `scope` has to equal the role's `scope` (`RoleService.grantPermissions`), or the create is refused with
   "Unknown permission: …" / "Permission scope mismatch for role …".

Meanwhile the **Permissions** screen next door lists exactly those codes, and has since `search-and-paging`:
server-side search, owner and scope filters, sorting, paging and totals. The user can *see* the catalogue
but cannot *pick* from it — they read a code off one screen and retype it into the other.

## What changes, in one page

```
┌ Create role ───────────────────────────────────────────────────────────────┐
│ Code       [ tenant-auditor                    ]                           │
│ Owner      [ Global (platform-defined)      ▾ ]                            │
│ Scope      [ TENANT                         ▾ ]                            │
│ Permissions   2 selected · tenant:user:read-only,                          │
│                 tenant:role:read-only                                      │
│            [ Choose permissions ]                                          │
└────────────────────────────────────────────────────────────────────────────┘
             │ opens
             ▼
┌ Grant permissions ─────────────────────────────────────────────────────────┐
│ [ Search permission code      ]  [ Owner ▾ ]  [ Sort ▾ ↑ ]                 │
│ 1–25 of 43 · Page 1 of 2                                                   │
│ [x] tenant:permission:read-only   TENANT · read-only · Global              │
│ [ ] tenant:role:read-only         TENANT · read-only · Global              │
│ [x] tenant:user:read-only         TENANT · read-only · Global              │
│ …                                                                          │
│                                    [ First < ]  Page 1 of 2  [ > Last ]    │
├────────────────────────────────────────────────────────────────────────────┤
│ 2 selected: tenant:permission:read-only x  tenant:user:read-only x         │
│                                        [ Clear ]  [ Cancel ]  [ Apply ]    │
└────────────────────────────────────────────────────────────────────────────┘
```

- The permissions field becomes a **selection summary** (count + the codes) and a *Choose permissions*
  button. Nothing is typed.
- The picker is a **dialog that is a list**: the console's own `SearchField`, `ListToolbar`, `SortSelect`,
  `PagedListView` and `PaginationBar` — so search is server-side and covers the whole catalogue, the result
  set is described (`1–25 of 43 · Page 1 of 2`), the empty and error states are the shared ones, and the
  list degrades exactly like the Permissions screen does.
- The picker **pins the query to what the backend will accept**, derived from the role's owner and scope —
  so no offered row can be refused:

| Role being created | picker `tenantId` | picker `scope` | owner filter shown? |
| --- | --- | --- | --- |
| Platform plane · global role (owner `null`) · `PLATFORM` | `null` (every `PLATFORM` row is global anyway) | `PLATFORM` | no — nothing to narrow |
| Platform plane · global role · `TENANT` | `null` (the global catalogue **plus** each tenant's rows — all grantable, per `ownerFilter(null)`) | `TENANT` | **yes** — narrows to the global catalogue or one tenant |
| Platform plane · tenant-owned role (owner `T`) · `TENANT` | `T` (global + `T`, exactly `ownerFilter(T)`) | `TENANT` | no — the owner is already fixed |
| Tenant plane (owner is the caller's tenant, never named) | dropped by `ConsoleScope.permissions` | `TENANT` | no — the route has no owner to name |

- **Selection survives paging, searching and filtering**: it is a map keyed by code, so a code picked on
  page 1 stays picked after a search, and the summary re-shows every code before Create.
- The **rows are the catalogue rows** — code, `scope`, level and owner — rendered by the same widget the
  Permissions screen uses, so the two screens cannot drift.
- On the tenant plane a row the caller **may not grant** (`CallerScope.requireGrantable`: never the
  wildcard, and never a code the caller does not hold — with write implying read) is **disabled and says
  why**, instead of being offered and then refused by the server.
- Changing the role's owner or scope **drops the selections that no longer match** and says so, rather than
  letting a user submit a pair the backend will reject.
- **Roles gain *Edit***: the same dialog, prefilled and reusing the same picker, sending
  `PATCH /api/v1/roles/{id}` (`UpdateRoleRequest`) — so a role's grants can be corrected, not only set once.
  The owner, and the scope of a tenant-owned role, are shown read-only (both are immutable server-side), and
  the seeded `platform-admin` / tenant `admin` roles have no *Edit* action at all.

Reused as-is, with **no backend work**: `GET /api/v1/permissions` and `GET /api/v1/tenant/permissions` are
already paged, searchable (`q`), filterable (`tenantId`, `scope`), sortable (`code`/`createdAt`/`updatedAt`)
and totalled — the exact contract the picker needs, already proven by `AdminIntegrationTest`
(`/api/v1/permissions?scope=TENANT`, `?q=platform%3Atenant`) and `TenantSelfServiceIntegrationTest`.

## Phase list

1. Discovery & design — `01-discovery-and-design.md`
2. Database changes — **skipped.** No DDL, no Liquibase changeset, no jOOQ codegen churn: the picker reads
   the existing `permissions` table through the existing paged route.
3. Client-side rules — `03-domain-and-application-services.md`
4. Server-side API & realtime — **skipped.** No route, DTO, guard or realtime-message change; the picker
   calls the two list routes the Permissions screen already calls.
5. Frontend / UI (Flutter) — `05-frontend-ui-flutter.md`
6. Security & observability — `06-security-and-observability.md`
7. Testing — `07-testing.md`
8. Delivery — `08-delivery.md`

## Decisions

1. **The picker is a paged list — not a dropdown, and not an `/options` set.** A dropdown cannot hold a
   catalogue that grows with every tenant that defines a permission, and `OptionList` is capped at 500 rows:
   the case §1.13 itself says should become type-to-search. Making the picker a *list* gives search,
   filters, sorting, paging and totals from the shared widgets, and the complete set stays reachable because
   the user **narrows** instead of scrolling. The selection accumulates across pages, so "every choice is
   reachable" is preserved by construction rather than by one big response.
2. **The picker's scope is pinned, not filterable.** The create *permission* dialog already derives `scope`
   from the resource instead of letting the user compose an invalid `scope` + `code` pair
   (`core/permissions.dart`); this is that rule one level up. A filter whose only effect is to make the
   eventual Create fail is worse than no filter (`docs/UX_GUIDELINES.md` §1.6, "filter on what the row
   shows"). The scope is therefore *stated* ("TENANT — only TENANT-scope permissions can be granted") and
   only the owner filter is interactive, in the one case where several owners are genuinely grantable.
3. **The selection is a `Map<String, Permission>` (code → row), not a `Set<String>`.** Holding the row is
   what lets an owner/scope change drop exactly the selections that became invalid, and lets the summary
   show a code's scope and owner without another request.
4. **The picker's list state is dialog-local, not in the URL.** §1.12 is about *screens*: the URL is what
   survives a refresh and can be pasted to a colleague. A modal inside an unsaved form has no such contract,
   and writing a second `page`/`q`/`sort` into the same query string would corrupt the screen's own state.
5. **One row widget renders a permission**, shared by the Permissions screen and the picker (selectable
   when the picker passes a selection), so "what a permission looks like" is stated once.
6. **No new provider, no new endpoint.** The picker uses the existing `permissionsPageProvider(ListQuery)`
   family, which is already console-aware (it drops `tenantId` on the tenant plane). No `permissions/options`
   route and no `OptionList<Permission>` is introduced.
7. **Roles gain *edit* as well as create** (confirmed 2026-09-28). The same dialog serves both, because the
   picker's value is greatest where a grant set is *fixed*: without an edit path a mistyped or missing grant
   is unrecoverable from the console. Edit sends `PATCH /api/v1/roles/{id}` with a new
   `UpdateRoleRequest(code, scope, permissions)` — the owner is **immutable** (the service ignores it and a
   tenant-owned role must stay `TENANT` scope, `RoleService.requireOwnableScope`), so those two fields are
   stated read-only while editing. Creating still uses `CreateRoleRequest`.
8. **The seeded admin roles are not editable from the console.** `PermissionCatalog.isSeededAdminRole` marks
   the global `platform-admin` and each tenant's `admin` immutable — "without this a single role-write holder
   could delete the only role that grants them back in" — so `Role.isSeededAdmin` mirrors that check and the
   row's *Edit* action is not rendered, instead of letting the user write a change the backend will refuse.
9. **No authorization, logging, metric, realtime or configuration change.** The guards on both ends stay as
   they are — create and edit are both behind the plane's role write grant, and the catalogue behind the
   permission read grant; a caller who may not read the catalogue gets the shared error panel with a Retry,
   not a bypass.

## Open questions

1. **An access-level filter (`read-only` / `read-write`) — include it now, or record it as a follow-up?**
   *Proposed: follow-up.* It is the one filter that would always be available in the picker (the scope is
   pinned, the owner filter applies to a single case), and the level is already shown on every row. It needs
   a backend parameter (`access=read-only|read-write`) on both permission list routes plus service,
   validation and integration tests — turning a frontend-only delivery into a contract change. The picker is
   built so the control is a one-line addition later.
2. ~~**Should this delivery also add *edit role* to the screen?**~~ **Resolved (2026-09-28): yes, in scope.**
   The same dialog serves create and edit (`UpdateRoleRequest` + `ApiClient.updateRole` +
   `ConsoleScope.updateRole`), the row gains an *Edit* action gated on the same write grant, and the seeded
   admin roles have no *Edit* action at all (decision 8). The owner and, for a tenant-owned role, the scope
   are read-only: both are immutable server-side.
3. **On the tenant plane, disable the rows the caller cannot grant, or let the server's `422` explain?**
   *Proposed: disable, with the reason on the row.* "Cannot grant a permission you do not hold" is a correct
   refusal but a poor experience when the console could have greyed the row out. Implemented as the
   `Me.canGrant` mirror in phase 3 — the same "UX, not security" mirror `Me.allowsResource` already is.
4. **A global (`tenant_id IS NULL`) role may hold a *tenant-owned* `TENANT`-scope permission** — the backend
   accepts it (`ownerFilter(null)` is `noCondition`), and it is meaningless to the other tenants sharing that
   global role. *Proposed: unchanged here, recorded as a backend follow-up.* The picker mirrors what the
   backend accepts — it must never hide a row the server would take — and the owner filter in that case is
   how an admin stays on the global catalogue.
5. **`docs/UX_GUIDELINES.md`: add §1.17 ("a picker over a set too large for one control is a paged list")
   plus the §2 mapping row?** *Proposed: yes*, because §1.13 as written forbids what this delivery does, and
   the written rule is what makes the next app build it the same way.

## Confirmation state

- [x] **Plan reviewed and approved (2026-09-28)** — open question 2 answered **yes** (edit role is in scope);
      questions 1, 3, 4 and 5 accepted as recommended (access-level filter deferred to a follow-up; rows the
      caller may not grant are disabled with the reason; the global-role/tenant-permission wart is recorded
      as a backend follow-up; `docs/UX_GUIDELINES.md` §1.17 is written).
- [x] Phase 1 — Discovery & design (2026-09-28)
- [x] Phase 3 — Client-side rules (2026-09-28)
- [x] Phase 5 — Frontend / UI (Flutter) (2026-09-28)
- [x] Phase 6 — Security & observability (2026-09-28)
- [x] Phase 7 — Testing (2026-09-28)
- [x] Phase 8 — Delivery (2026-09-28)

### Execution log

| Phase | State | Date |
| --- | --- | --- |
| 1 — Discovery & design | **done** — 11 acceptance criteria, the route contract, the grantability rule read from the backend, the §1.17 draft. Docs only. | 2026-09-28 |
| 3 — Client-side rules | **done** — `ListQuery.permissionsFor`, `PermissionSelection`, `Me.canGrant`, `Role.isSeededAdmin`, `UpdateRoleRequest` + `ApiClient.updateRole`/`ConsoleScope.updateRole`. | 2026-09-28 |
| 5 — Frontend / UI | **done** — `permission_picker.dart`, `permission_row.dart`, the create/edit role dialog, the row's *Edit* action, the Permissions screen switched to the shared row. | 2026-09-28 |
| 6 — Security & observability | **done** — guardrails unchanged; the picker affordance gated on the permission read grant; non-grantable rows disabled. No logging/metric/realtime change. | 2026-09-28 |
| 7 — Testing | **done** — 3 new Flutter suites (10 + 4 + 19) plus 7 additions; **136 passed** (was 96), `flutter analyze` clean in the package and in the hosting app. | 2026-09-28 |
| 8 — Delivery | **done** — `CHANGELOG.md` entry, `docs/UX_GUIDELINES.md` §1.17 + §2 mapping; client-only deploy, no migration. `docs/ARCHITECTURE.md` reviewed and left unchanged (§9.5 documents the guardrails, not the console's affordances). | 2026-09-28 |

### Deviations recorded after execution

1. **One dialog-precondition was tightened beyond the plan** (phase 5, deviation 2): choosing a tenant *owner*
   now sets the role's scope to `TENANT` and disables the scope control, because `RoleService.requireOwnableScope`
   refuses a tenant-owned `PLATFORM` role. The picker made the invalid pair reachable (a picker seeded with
   `PLATFORM` for a role that can only be `TENANT`), so it was closed in the same delivery.
2. **`PermissionSelection` stores `code`/`scope`/`tenantId` records, not `Permission` rows** (phase 3, deviation 1):
   an edit starts from a `Role`, which carries codes only.
3. **`dart format` was not used as a gate.** The installed SDK's formatter restyles every file in the repository
   (new "tall" style, 80 columns, vs the committed 120-column style), so its unrelated rewrites were reverted and
   the new code written in the repository's existing style. See `05-frontend-ui-flutter.md` → *Formatter note*.
4. **Manual click-through was not performed** (no dev credentials in this environment); recorded as follow-up 3 in
   `08-delivery.md`.



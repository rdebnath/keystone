# Delivery Plan — Server-side search, filtering & paging (+ a common UX guideline)

**Feature slug:** `search-and-paging`
**Level:** **Platform-level** — touches `platform/keystone-common`, `platform/keystone-data` and
`platform/keystone-web` (new shared list primitives), `platform/keystone-admin` (REST contract +
services for `tenants`/`users`/`roles`/`permissions` on **both** admin planes),
`platform/keystone-admin-ui` (Flutter console), `docs/` (a new UX guideline) and
`docs/ARCHITECTURE.md`.

## Sizing decision

**Produce a plan.** The change spans every layer:

- **New shared platform primitives** (`keystone-common`, `keystone-data`, `keystone-web`) so the rule
  is written once instead of four times.
- **A REST contract change**: every list route stops returning a bare JSON array and returns a typed
  **page envelope**; the four list services change their return type, and three new `…/options` routes
  appear (pickers cannot use a paged endpoint).
- **Both admin planes**: the platform plane and the tenant self-service plane (the tables are shared,
  but the tenant plane has its own handlers and must page/search its own users, roles and permissions).
- **Flutter**: four screens, four providers, a new family key type, shared search/pagination widgets,
  and updated tests.
- **A new normative UX guideline** (`docs/UX_GUIDELINES.md`) plus contract text in the two coding
  guidelines.
- **Database**: the default `ORDER BY` of the paged queries needs supporting indexes.

## Problem

Today all four list endpoints return **everything**: `TenantService.list`, `UserService.list`,
`RoleService.list` and `PermissionService.list` are `selectFrom(...).fetch()` with no `LIMIT`/`OFFSET`,
and the console renders the whole array. Filtering exists (the platform `tenantId` filter) but it is a
query parameter on a full scan, and there is **no search at all**.

The consequence is what the request calls out: a search that filtered the loaded array would search
**one page** of data. Search must therefore be a server-side `WHERE` on the same query the page window
is taken from, and the client must be told the **total** so it can show and navigate pages.

## Shape of the change

### 1. One list contract, defined once

| Parameter | Type | Default | Rule |
| --- | --- | --- | --- |
| `page` | int ≥ 0 | `0` | 0-based (`offset = page × size`); the UI labels it `page + 1` |
| `size` | 1…100 | `25` | outside the range → `422` |
| `sort` | resource key | resource default | not in the resource's whitelist → `422` naming the allowed keys |
| `order` | `asc` \| `desc` | `asc` | case-insensitive; anything else → `422` |
| `q` | string ≤ 100 | absent | case-insensitive **contains** over the resource's searchable columns |
| resource filters | | | `tenantId` (users/roles/permissions), see below |

```json
{
  "items": [ { "…": "one DTO per element, unchanged shape" } ],
  "page": 0,
  "size": 25,
  "totalElements": 142,
  "totalPages": 6,
  "hasNext": true,
  "hasPrevious": false
}
```

### 2. Per resource

| Resource | List routes (platform · tenant) | Search columns (`q`) | Filters | Sort keys | Default order |
| --- | --- | --- | --- | --- | --- |
| tenants | `GET /api/v1/tenants` | `name`, `slug`, `country` | — | `name`, `slug`, `country`, `createdAt`, `updatedAt` | synthetic platform row first, then `name ASC` |
| users | `GET /api/v1/users` · `GET /api/v1/tenant/users` | `username`, `email`, `phone_number` | `tenantId` (platform only) | `username`, `email`, `createdAt`, `updatedAt` | `username ASC` |
| roles | `GET /api/v1/roles` · `GET /api/v1/tenant/roles` | `code` | `tenantId` (platform only), `scope` | `code`, `createdAt`, `updatedAt` | `tenant_id ASC NULLS FIRST, code ASC` |
| permissions | `GET /api/v1/permissions` · `GET /api/v1/tenant/permissions` | `code` | `tenantId` (platform only), `scope` | `code`, `createdAt`, `updatedAt` | `tenant_id ASC NULLS FIRST, code ASC` |

`scope` on roles/permissions is **optional** (open question 2): it filters a column the console already
shows, but it was not requested.

**Ordering must be total.** A page window is only stable if the `ORDER BY` is deterministic, so every
sort key gets a tiebreaker (`code`, `username`, `name`, `id`) — a partial order would let a row appear
on two pages or none.

### 3. Pickers: `/options` (why a page cannot feed a dropdown)

Four console controls are fed by a *complete* list, not a table, and a paged endpoint would silently
truncate them: the owner filter (`roles`/`permissions` screens, `owner_filter.dart`), the tenant filter
(`users_screen.dart`), the tenant pickers in the user/role/permission dialogs, and the role checklist in
the user editor (`user_editor.dart:626` watches `rolesProvider(null)`). So the contract gets a second,
explicitly **unpaged** shape for reference data:

| New route | Plane | Guard | Returns |
| --- | --- | --- | --- |
| `GET /api/v1/tenants/options` | platform | `platform:tenant:read-only` | synthetic platform row + every tenant |
| `GET /api/v1/roles/options[?tenantId=]` | platform | `platform:role:read-only` | every visible role (`RoleDto`) |
| `GET /api/v1/tenant/roles/options` | tenant | `tenant:role:read-only` | the caller's global + own roles |

```json
{ "items": [ { "…": "TenantDto / RoleDto" } ], "truncated": false }
```

`truncated` is honest about the cap (`500`) instead of silently dropping rows. The guideline states the
rule: *a paged list serves tables; `/options` serves pickers, is never paged, and is capped.* No route
conflict exists — `tenants` and `roles` have no `GET …/{id}`, so `/tenants/options` cannot be captured by
a path-parameter route (and a test asserts it).

### 4. The synthetic platform tenant row

`GET /api/v1/tenants` returns the synthetic `Keystone` row (`PlatformSchema.PLATFORM_TENANT_ID`) ahead
of the persisted tenants today, and `docs/ARCHITECTURE.md` §9.2 documents that. It is **not a row in the
table**, so paging it needs a rule (open question 1). Recommended: it stays a **pinned first row that
participates in search** — the logical list is `[platform] + tenants`, and the service maps the
requested window onto the real rows around it (a pure, unit-tested helper). When `q` is present and does
not match the synthetic row's name/slug, it is excluded: a search result may never contain a row that
does not match the term.

### 5. Where the shared code lives (so the rule is not re-implemented)

| Module | New artifact | Why there |
| --- | --- | --- |
| `keystone-common` | `common/query/PageRequest` (record: `page`, `size`, `sort`, `order`; `defaults()`, `offset()`; canonical constructor validates → `422`, `size ≤ 100`, `page ≤ 10 000`) | framework-free wire vocabulary every app and the platform share |
| `keystone-common` | `common/query/Page` (record: `items`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`; `Page.of(items, request, total)` computes the arithmetic) | one envelope, one place that does the maths |
| `keystone-common` | `common/query/OptionList` (record: `items`, `truncated`) | the unpaged reference-data envelope for pickers |
| `keystone-common` | `common/query/SortOrder` (`ASC`/`DESC`, `parse(String)`) | no string literals for direction |
| `keystone-common` | `common/query/SearchTerm.normalize(String)` (trim, blank → `null`, length cap) | the search rule, testable without HTTP |
| `keystone-data` | `Search.containsIgnoreCase(term, fields…)` → jOOQ `Condition` (`noCondition()` for a blank term; `LIKE … ESCAPE '\'` OR'ed over the fields, with `%`, `_`, `\` escaped) + `Search.matches(term, values…)` for rows that are not in the database | jOOQ-specific, so it belongs with the jOOQ infrastructure (`Field.likeIgnoreCase(String, char)` verified present in jOOQ 3.21.8) |
| `keystone-web` | `QueryParams.page(Context)`, `QueryParams.search(Context)`, `QueryParams.order(Context, SortOrder)` | HTTP parsing stays in the web layer; a handler lists the params in one line |

Deliberately **not** added: a generic "run this select paged" helper over jOOQ's generic types. Each
service keeps its explicit `fetchCount(...)` + `.limit(…).offset(…)` (guideline §7: write the SQL you
need); only the escaping, the offset and the envelope arithmetic are shared.

## Non-goals

- **No offset-free/keyset paging.** `page`/`size` is what the guideline already names and what a console
  needs (jump to page, totals). Keyset paging is a later optimisation for very deep pages.
- **No fuzzy/trigram search.** `q` is a case-insensitive substring match; typo tolerance and ranking are
  not requested.
- **No saved searches, no per-user list preferences, no column chooser.**
- **No realtime.** The admin console is request/response only (as in
  `docs/delivery/tenant-country-user-phone`), so no Supabase Realtime channel or payload version changes.
- **No write-route changes.** `POST`/`PATCH`/`PUT`/`DELETE` bodies and responses are untouched.
- **No new permission codes.** Paging/search is a read of the same resources, behind the same guards.
- **The `apps/inventory` `GET /api/v1/items` list is out of scope** — it is not one of the four named
  modules; the shared primitives make it a later one-line adoption.

## Open questions (need a decision before phase 3)

| # | Question | Recommended answer |
| --- | --- | --- |
| 1 | How does the synthetic platform tenant row behave in a paged list? | Pinned **first**, consumes the first slot of page 0, counts in `totalElements`, and is **covered by `q`** (omitted when the term does not match). Alternative: drop it from the list entirely and expose it only through `/tenants/options` + the by-id lookup — cleaner numbers, but it changes the documented "returned first" contract and the drill-down UX. |
| 2 | Add a `scope` (`PLATFORM`/`TENANT`) filter to roles/permissions? | **Yes** — it is a column the screens display, and it is the same `WHERE` shape as `tenantId`. Strike it if you want the change strictly smaller. |
| 3 | Is the list state (search, page, filter) kept in the URL so a list is deep-linkable and refresh-safe? | **Yes** via `go_router` query parameters (`admin_shell` already owns the routes). It is the difference between "a filter you can lose" and a console you can share. Moderate extra work in phase 5; strike it to keep phase 5 smaller. |
| 4 | Is the 500-row cap on `/options` acceptable for the tenant pickers? | **Yes for now**, with the truncation surfaced and a documented follow-up: once the tenant count approaches the cap, the pickers become type-ahead (Material 3 `SearchAnchor` against `/options?q=`). |

## Phases

| # | Phase | File | Applies |
| --- | --- | --- | --- |
| 1 | Discovery & design (incl. the UX guideline) | `01-discovery-and-design.md` | yes |
| 2 | Database changes | `02-database-changes.md` | yes (indexes only) |
| 3 | Domain & application services | `03-domain-and-application-services.md` | yes |
| 4 | Server-side API & realtime | `04-server-side-api-and-realtime.md` | yes (contract + routes; no realtime) |
| 5 | Frontend / UI (Flutter) | `05-frontend-ui-flutter.md` | yes |
| 6 | Security & observability | `06-security-and-observability.md` | yes |
| 7 | Testing | `07-testing.md` | yes |
| 8 | Delivery | `08-delivery.md` | yes |

## Confirmation state

- [x] Plan presented — 2026-09-28 (plan written; no code before confirmation).
- [x] **Question 1 confirmed** (2026-09-28): the synthetic platform tenant row stays a **pinned,
  searchable first row** — it takes the first slot of page 0, counts in `totalElements`, is covered by `q`,
  and is omitted when the term cannot match it.
- [x] **Question 2 confirmed** (2026-09-28): roles and permissions get the **`scope`**
  (`PLATFORM`/`TENANT`) filter.
- [x] **Question 3 confirmed** (2026-09-28): list state (search, filters, `page`, `size`, `sort`) lives in
  the **URL query string**, so a filtered list can be refreshed, bookmarked and shared.
- [x] **Question 4 confirmed** (2026-09-28): the picker lists (`/tenants/options`, `/roles/options`) keep a
  **500-row ceiling** with `"truncated": true` beyond it; type-to-search pickers stay a follow-up.
- [x] **Execution started** — phase 1 (2026-09-28). All four open questions are answered, so the plan as
  written is what gets built.

## Execution log

| Phase | State | Date |
| --- | --- | --- |
| 1 — Discovery & design | **done** — `docs/UX_GUIDELINES.md` created; backend §8 *List endpoints* + §13, frontend §7.3 + §14, `ARCHITECTURE.md` §5.1/§9.2 and `AGENTS.md` updated. Docs-only; see `01-discovery-and-design.md`. | 2026-09-28 |
| 2 — Database | **done** — `0005-paging-indexes.xml` (3 indexes, guarded, idempotent) + master include; `PagingIndexesSchemaTest` asserts them on a real PostgreSQL. One deviation: a third index (`idx_users_username`). | 2026-09-28 |
| 3 — Domain & application services | **done** — `common/query` (`PageRequest`, `Page`, `OptionList`, `SortOrder`, `SearchTerm`), `data/Search`, `web/QueryParams`, `Scope.optional`, and all four services return `Page<Dto>`; `PlatformRowWindow` for the pinned tenant row. | 2026-09-28 |
| 4 — Server-side API | **done** — 7 list routes paged/searched/filtered, 3 unpaged `/options` routes, `scope` filter; no new Guice bindings, no realtime change. | 2026-09-28 |
| 5 — Frontend / UI (Flutter) | **done** — `ListQuery` + `Paged`/`OptionList`, paged providers, the shared list widgets, five screens, URL state. | 2026-09-28 |
| 6 — Security & observability | **done** (review) — guards unchanged and verified, caps and escaping proven, no new logging/metrics/realtime. | 2026-09-28 |
| 7 — Testing | **done** — Java: 5 new unit suites + `PlatformRowWindowTest` + `SearchTest` + `QueryParamsTest` + `PagingIndexesSchemaTest` + a new integration test (admin **99**, was 88). Flutter: 3 new suites (**94**, was 66). All green. | 2026-09-28 |
| 8 — Delivery | **done** — `CHANGELOG.md` (breaking envelope noted), the four guideline/architecture updates, deploy order and five follow-ups. | 2026-09-28 |

### Open question 4 in plain terms

Some console controls are **dropdowns** — the *Tenant* filter on the Users screen, the *Owner* filter on
the Roles and Permissions screens, the tenant picker in the "Add user / role / permission" dialogs, and the
role checklist in the user editor. A dropdown has to show **all** the choices, so it cannot be fed by a
paged list (a dropdown built from page 1 would only ever offer the first 25 tenants — the same problem the
request is about, in another form).

So these get their own unpaged routes: `/tenants/options` and `/roles/options`, which return the whole set
in one response. But "the whole set" has no natural end, and one un-bounded response is a risk for the
server, the browser and the dropdown itself. `OptionList` therefore carries a ceiling (a maximum number of
rows `/options` will return) plus a `"truncated": true` flag when there were more rows than the ceiling —
so the UI can say "showing the first N" instead of silently hiding tenants.

`500` is the proposed ceiling. It is far more than any dropdown can usefully display, it bounds one
response to a few hundred kilobytes, and the recorded follow-up is to turn the pickers into
**type-to-search** (query `/options?q=…`) once the tenant count approaches it — at which point the ceiling
stops mattering, because the user narrows the list before it is ever returned.

**What the decision changes:** only this number. A lower cap (100) fails sooner but is still safe; a higher
cap (1000) is safe today but the picker should then be type-ahead; no cap at all is the one option not
recommended, because it makes the response size depend on customer growth.

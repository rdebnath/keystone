# Phase 3 — Domain & application services

## Scope

The shared query vocabulary (so four services do not each invent it) and the four list services becoming
**paged + searchable** while keeping every existing visibility rule (`tenantFilter`, `ownerFilter`,
`effectiveOwner`, `requireGrantable`) exactly as it is.

## Artifacts — platform libraries

| Artifact | Shape |
| --- | --- |
| `keystone-common` `common/query/PageRequest.java` | `record PageRequest(int page, int size, String sort, SortOrder order)`; `DEFAULT_SIZE = 25`, `MAX_SIZE = 100`, `MAX_PAGE = 10_000`; `defaults()`, `offset() = page * size`; the canonical constructor trims a blank `sort` to `null`, defaults a `null` order to `ASC`, and throws `ValidationException` (→ `422`) for `page < 0`, `page > MAX_PAGE`, `size < 1`, `size > MAX_SIZE`, or a malformed `sort`. |
| `keystone-common` `common/query/Page.java` | `record Page<T>(List<T> items, int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious)` + `static <T> Page<T> of(List<T> items, PageRequest request, long totalElements)` — the **only** place that computes `totalPages`/`hasNext`/`hasPrevious`. They are components (not derived accessors) so the envelope Jackson writes is explicit and clients never re-derive it. |
| `keystone-common` `common/query/OptionList.java` | `record OptionList<T>(List<T> items, boolean truncated)` + `MAX_OPTIONS = 500`, for the `/options` routes (phase 4). |
| `keystone-common` `common/query/SortOrder.java` | `enum SortOrder { ASC, DESC }` + `parse(String)` (case-insensitive; blank → `ASC`; anything else → `ValidationException`). |
| `keystone-common` `common/query/SearchTerm.java` | `static String normalize(String raw)` — trim, blank → `null`, longer than `MAX_LENGTH = 100` → `ValidationException`. The search *rule* in one place, testable without HTTP. |
| `keystone-data` `Search.java` | `@SafeVarargs static Condition containsIgnoreCase(String term, Field<?>... fields)` — `DSL.noCondition()` for a `null` term, else the OR of `field.likeIgnoreCase("%" + escaped + "%", '\\')`, escaping `\`, `%` and `_`; plus `static boolean matches(String term, String... values)` for the one row that is **not** in the database (`null`-safe, `Locale.ROOT`, blank term matches). |
| `keystone-web` `QueryParams.java` | `static PageRequest page(Context ctx)`, `static String search(Context ctx)` (via `SearchTerm.normalize`), `static SortOrder order(Context ctx, SortOrder fallback)` — HTTP parsing stays in the web layer and a handler reads its params in one line. |
| `keystone-admin` `identity/Scope.java` | one addition: `static Scope optional(String value)` — absent/blank → `null`, present → the existing `from(value)`. Mirrors `Ids.optionalUuid` so the `scope` filter is parsed the same way as `tenantId`. |

`Field.likeIgnoreCase(String, char)` was **verified present** in the pinned jOOQ **3.21.8**
(`javap` on `~/.m2/repository/org/jooq/jooq/3.21.8/jooq-3.21.8.jar`), so no custom `ESCAPE` glue is needed.

## Artifacts — `keystone-admin` services

All four `list(...)` methods gain `(String search, PageRequest page)` and return `Page<Dto>`; each body
becomes the same three steps: build `Condition where`, `fetchCount(where)` for the total, then
`selectFrom(...).where(where).orderBy(orderOf(page)).limit(page.size()).offset(page.offset())`. Sort keys
resolve through a private `switch` (the whitelist); **an unknown key throws `ValidationException` naming
the allowed keys**, so a sort key can never reach SQL as arbitrary text.

| Service | Change |
| --- | --- |
| `tenant/TenantService` | `Page<TenantDto> list(PageRequest page, String search)` and `OptionList<TenantDto> options()`. Search = `Search.containsIgnoreCase(search, TENANTS.NAME, TENANTS.SLUG, TENANTS.COUNTRY)`. The synthetic row keeps its place through the new pure helper `tenant/PlatformRowWindow` (below). Sort keys `name` (default), `slug`, `country`, `createdAt`, `updatedAt`, tiebreaker `id`. |
| `user/UserService` | `Page<UserDto> list(CallerScope caller, UUID filter, String search, PageRequest page)`. `where = tenantFilter(visibleTenant(caller, filter)) AND Search.containsIgnoreCase(search, USERS.USERNAME, USERS.EMAIL, USERS.PHONE_NUMBER)`; the roles map is narrowed to the page's user ids (`USER_ROLES.USER_ID.in(ids)`, joined to `ROLES` for the codes) and skipped when the page is empty. Sort keys `username` (default), `email`, `createdAt`, `updatedAt`, tiebreaker `id`. |
| `role/RoleService` | `Page<RoleDto> list(CallerScope caller, UUID filter, Scope scope, String search, PageRequest page)` and `OptionList<RoleDto> options(CallerScope caller, UUID filter)` (same `effectiveOwner` visibility, default order, capped at `MAX_OPTIONS`). The `permissionsByRole` lookup is narrowed to the page's role ids. Search = `code`; a present `scope` is an equality filter on `ROLES.SCOPE`; sort keys `code` (default), `createdAt`, `updatedAt`. |
| `permission/PermissionService` | `Page<PermissionDto> list(CallerScope caller, UUID filter, Scope scope, String search, PageRequest page)` — search = `code`, `scope` filter, the same sort keys. No `options()`: nothing lists permissions in a picker (the create-role dialog takes codes as text). |

### `tenant/PlatformRowWindow` (the only non-obvious piece)

```java
record PlatformRowWindow(int realOffset, int realLimit, boolean includesPlatformRow) {
    static PlatformRowWindow of(PageRequest page, boolean platformMatches, long realTotal);
}
```

`platformMatches = Search.matches(search, PLATFORM_TENANT_NAME, RESERVED_SLUG)`. When it is `false` the
window is the plain `(offset, size, false)`. When it is `true` the logical list is `[platform] + tenants`,
so page 1 takes `size - 1` real rows from offset `0`, and page *p* > 1 takes `size` rows from offset
`p * size - 1`. `TenantService` then builds the envelope with
`total = realTotal + (platformMatches ? 1 : 0)` and prepends the existing `platformTenant()` DTO when
`includesPlatformRow`. Pure, no database, fully unit-testable (phase 7) — which is exactly why it is a
separate record rather than inline arithmetic.

## Decisions

- **`Page`/`PageRequest`/`OptionList` live in `keystone-common`, not in `keystone-admin`** — the app
  modules own lists too (`apps/inventory` `GET /api/v1/items`), and the guideline must apply to them.
- **No generic "paged fetch" over jOOQ's generic types**: each service keeps its explicit count + window
  queries (guideline §7 — write the SQL you need). Only escaping, `offset()` and the envelope arithmetic
  are shared.
- **`scope` is a filter, not a sort key**, and only roles/permissions carry it (open question 2).
- **The default order stays what the console shows today** (`username`, `name`, global-first then `code`),
  so paging changes *how much* is returned, never *what order* — a paged list that reorders its first page
  would be a surprising regression.
- **`list()` keeps its `CallerScope`-derived visibility untouched**: paging narrows the rows of the same
  visible set, never widens it (phase 6 re-checks this).
- **Search terms are never persisted or echoed into logs** — only into the response (`totalElements`), the
  detail of which is a phase 6 concern.

## Dependencies

- Phase 1 (the contract) and phase 2 (the indexes the default orders need).

## Execution (2026-09-28)

Delivered as designed, with the shared vocabulary as promised:

| Artifact | Notes |
| --- | --- |
| `keystone-common` `common/query/` | `PageRequest`, `Page`, `OptionList`, `SortOrder`, `SearchTerm` — records, no framework dependencies |
| `keystone-data` `Search` | `containsIgnoreCase(term, fields…)` (blank term → `DSL.noCondition()`, escaped `ILIKE … ESCAPE '\'`, fields OR'ed) and `matches(term, values…)` for the one row that is not in the database |
| `keystone-web` `QueryParams` | `page(Context)`, `search(Context)`, `order(Context, fallback)` — one line per handler |
| `keystone-admin` `identity/Scope` | `optional(String)` added (absent → `null`, present → validated), mirroring `Ids.optionalUuid` |
| `tenant/TenantService` | `Page<TenantDto> list(PageRequest, String)` + `OptionList<TenantDto> options()` + `tenant/PlatformRowWindow` |
| `user/UserService` | `Page<UserDto> list(caller, filter, search, page)`; roles loaded per **page** of ids |
| `role/RoleService` | `Page<RoleDto> list(caller, filter, scope, search, page)` + `OptionList<RoleDto> options(caller, filter)`; grants loaded per page |
| `permission/PermissionService` | `Page<PermissionDto> list(caller, filter, scope, search, page)` |

**Notes from the implementation**

- **`Search.containsIgnoreCase` renders `ILIKE`** on PostgreSQL (asserted in `SearchTest`), not
  `LIKE … IGNORE CASE`; the explicit `ESCAPE '\'` is what keeps `%`, `_` and `\` literal.
- **A field named `DSL` cannot coexist with `org.jooq.impl.DSL`** — the first attempt at `SearchTest` named
  its `DSLContext` field `DSL` and javac resolved `DSL.using(...)` to the field. Renamed to `CTX`.
- **`Page`/`OptionList` copy their items** (`List.copyOf`) so a caller cannot mutate an envelope it was
  handed; asserted in their unit tests.
- **Tenant roles/permissions keep the default composite order** (`tenant_id NULLS FIRST, code`); a named
  sort key replaces it and adds `id` as a tiebreaker, because a partially ordered window can drop or repeat
  rows.
- **`visibleTenant` / `effectiveOwner` / `requireGrantable` are untouched**: every page is taken from the
  same `WHERE` the unpaged list used.

## Dependencies

- Phase 1 (the contract) and phase 2 (the indexes the default orders need).

## Verification

- New unit tests, all green: `PageRequestTest` (7), `PageTest` (6), `OptionListTest` (4),
  `SearchTermTest` (4), `SortOrderTest` (4), `SearchTest` (6), `PlatformRowWindowTest` (7) — plus
  `QueryParamsTest` (3) in `keystone-web`. `keystone-common` 32 tests, `keystone-data` 11, `keystone-web`
  10, `keystone-admin` **99** (was 88).
- `mvn test` (whole reactor) → **BUILD SUCCESS**, so the whole API surface compiles and the existing
  integration suites still pass against the new signatures.


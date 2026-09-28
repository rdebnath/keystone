# Phase 7 — Testing

## Scope

Prove the two things the request is about: **search finds rows that are not on the current page**, and
**paging stays consistent with the search/filter**. Plus the arithmetic, the escaping, the validation and
the updated console fixtures.

## Java — unit (no database)

| Test | Covers |
| --- | --- |
| `keystone-common` `common/query/PageRequestTest` | defaults; `offset()`; `page < 0`, `size < 1`, `size > 100`, `page > 10 000`, malformed `sort` → `ValidationException`; blank `sort` → `null`; absent `order` → `ASC` |
| `keystone-common` `common/query/PageTest` | `of(...)` for: empty result, `total < size`, `total == size`, `total` an exact multiple of `size`, `total > size` → asserts `totalPages`, `hasNext`, `hasPrevious` at the first/middle/last page |
| `keystone-common` `common/query/SortOrderTest` | `asc`/`ASC`/`desc`, blank → `ASC`, garbage → `ValidationException` |
| `keystone-common` `common/query/SearchTermTest` | blank → `null`, trimming, 100-char boundary, 101 chars → `ValidationException` |
| `keystone-common` `common/query/OptionListTest` | `truncated` for `size < / == / > MAX_OPTIONS` |
| `keystone-data` `data/SearchTest` | blank/`null` term → `DSL.noCondition()`; `%`, `_`, `\` each escaped (assert on the **rendered** SQL via `DSL.using(SQLDialect.POSTGRES).render(condition)`, the idiom `JooqDataAccessTest` already uses); `matches(term, values…)` including `null` values and `Locale.ROOT` case folding |
| `keystone-web` `web/QueryParamsTest` | query params → `PageRequest`/search/order through a real embedded Javalin route (the `ContextPathTest` harness), including absent params and the `422` paths |
| `keystone-admin` `tenant/PlatformRowWindowTest` | `size = 1` on page 0 → platform only; the `size - 1` first page; offset `p * size - 1` on later pages; `platformMatches = false`; a page past the end → an empty window; `realTotal = 0` |
| `keystone-admin` `identity/ScopeTest` (extended) | `optional(null)`/`optional(" ")` → `null`; `optional("tenant")` → `TENANT`; garbage → `ValidationException` |

## Java — slice/integration (Testcontainers PostgreSQL, real Guice injector, embedded Javalin)

Extended in place rather than re-harnessed: `AdminIntegrationTest` (platform plane) and
`TenantSelfServiceIntegrationTest` (tenant plane) already bootstrap a tenant, a tenant admin and users.

| Assertion | Why it matters |
| --- | --- |
| Envelope shape on all seven list routes (`items`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`) | the contract the Flutter model parses |
| Seed 5 tenants + 30 users, then `q` for a row that sorts past page 1 → found on `page=0` with `totalElements=1` | **the defect being fixed**: search is not limited to the loaded page |
| `page=1&size=10` returns rows 11–20 of the same ordered set, with no repeat and no gap vs `page=0` | a total, stable `ORDER BY` |
| `size=101`, `size=0`, `page=-1`, `sort=nope`, `order=sideways`, `q` of 101 chars → `422`; `page=99` (past the end) → `200` with `items: []` | validation and the "stale page is not an error" rule |
| `q=%` and `q=_` → `0` results (not every row) | wildcard escaping |
| platform tenant: first item of `page=0`, counted in `totalElements`, present when `q=key`, absent when `q` cannot match it | the open-question-1 rule |
| `GET /api/v1/tenant/users?q=<a platform user's username>` → `0` results, and `?tenantId=` → `422` | no cross-tenant visibility through search, tenant still not caller-chosen |
| `GET /api/v1/tenants/options`, `/roles/options`, `/tenant/roles/options` → `200` with `items`; each `401` without a token and `403` without the read code; `/tenants/options` is not parsed as an id | the picker contract and the route-ordering risk |
| a `scope=TENANT` filter excludes `PLATFORM` rows and vice versa (if open question 2 is confirmed) | the new filter |

## Flutter

| Test | Covers |
| --- | --- |
| `test/paged_test.dart` (**new**) | `Paged<T>`/`OptionList<T>` parsing (present/absent/null fields, int → num, empty `items`) |
| `test/list_query_test.dart` (**new**) | `toQueryParameters()` omits blank `q` and a `null` `sort`; equality of two identical queries (the `family` cache key); `copyWith(page: 0)` on a filter change |
| `test/lists_widget_test.dart` (**new**) | `PagedListView`: error → retry, "nothing yet" vs "nothing matches '<term>'" + *Clear search*, keeps rows while refreshing; `PaginationBar`: page label, disabled ends, page-size change; `ListToolbar` summary text |
| `test/search_field_test.dart` (**new**) | 300 ms debounce fires once for a burst of keystrokes; the clear button empties the field and reports `''`; an external value re-syncs the controller |
| `test/user_editor_test.dart`, `test/admin_shell_test.dart`, `test/models_test.dart`, `test/console_test.dart` (updated) | provider overrides move to `tenantOptionsProvider`/`roleOptionsProvider`/`usersPageProvider`, fixtures become envelopes, and the role checklist reads options |

## Execution (2026-09-28)

Delivered as listed above, with one addition and one correction.

| Suite | New/changed | Result |
| --- | --- | --- |
| `keystone-common` `common/query` | `PageRequestTest` (7), `PageTest` (6), `OptionListTest` (4), `SearchTermTest` (4), `SortOrderTest` (4) — all new | module total **32**, 0 failures |
| `keystone-data` `SearchTest` | new (6) — escaping asserted on the **rendered SQL** | module total **11**, 0 failures |
| `keystone-web` `QueryParamsTest` | new (3) — through a real embedded Javalin route | module total **10**, 0 failures |
| `keystone-admin` `PlatformRowWindowTest` | new (7) — the pinned-row arithmetic | |
| `keystone-admin` `PagingIndexesSchemaTest` | new (3) — the `0005` indexes exist, composite leading column, migration run twice | |
| `keystone-admin` `AdminIntegrationTest` | one new test: envelope, page windows, off-page search, pinned row, wildcard escaping, `422`s, stale page, `/options`, `scope` filter | module total **99** (was 88), 0 failures |
| `keystone-admin-ui` | `envelopes_test` (7), `list_query_test` (8), `lists_test` (13) new; `console_test` and `user_editor_test` updated | **94** tests (was 66), all passed |

**Correction to the plan:** `SearchTest` asserts on **`ILIKE`**, which is what jOOQ renders for a
case-insensitive match on PostgreSQL — not `LIKE … IGNORE CASE`. The first version of the test asserted the
latter and failed, which is exactly what the rendered-SQL assertion is there to catch.

**What is not covered:** the search is a leading-wildcard `ILIKE`, so it is a sequential scan on these
catalog-sized tables; that is an accepted, documented trade-off (phase 2) rather than something a test can
prove. And the console has no widget test that drives a *screen* (with its router and URL) end to end — the
screens' behaviour is covered by `lists_test`, `list_query_test` and the provider-level `console_test`,
which is where the logic lives.

## Commands and results

```bash
mvn test                                              # whole reactor (Docker 29.8.1 present)
# → BUILD SUCCESS: common 32 · web 10 · data 11 · admin 99 · inventory-server 15, 0 failures

cd platform/keystone-admin-ui
flutter analyze && flutter test                        # → No issues found · 94 tests passed

cd apps/inventory/frontend
flutter analyze                                        # → No issues found
```


## Dependencies

- Phases 3, 4 and 5 (there is nothing to test before they exist).

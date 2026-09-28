# Phase 5 — Frontend / UI (Flutter)

## Scope

`platform/keystone-admin-ui` (the console both planes share) is reworked so every list screen is
server-paged and searchable, and so the UX guideline of phase 1 is enforced by **shared widgets** rather
than by four separate implementations. `apps/inventory/frontend` only hosts the console, so it has no
screen changes of its own.

## Artifacts — models (`lib/src/models`)

| Artifact | Change |
| --- | --- |
| `models/models.dart` | New `@freezed abstract class ListQuery` — the list state and the provider family key: `String? tenantId`, `@Default('') String search`, `@Default(0) int page`, `@Default(25) int size`, `String? sort`, `@Default('asc') String order`. Freezed gives value equality, which is what makes an `autoDispose.family` cache per query (and not per widget rebuild). Plus `ListQuery.initial({String? tenantId})` and `Map<String, String> toQueryParameters()` that omits `q` when blank and `sort`/`order` when `sort == null` (so the server's default order applies instead of being echoed). |
| `models/envelopes.dart` | **New**, hand-written immutable generics: `Paged<T>` (`items`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`) and `OptionList<T>` (`items`, `truncated`). |

`Paged<T>`/`OptionList<T>` are **not** freezed: `json_serializable` generates field-level code for
concrete classes, so a generic envelope would need four near-identical `TenantPage`/`UserPage`/`RolePage`/
`PermissionPage` copies that drift apart. A hand-written generic envelope with a shared parser keeps the
"no `Map` outlives the client call" rule (`docs/CODING_GUIDELINES_FRONTEND.md` §14) while staying DRY.
This is recorded as a decision, and the frontend guideline gains the same sentence.

## Artifacts — client and providers (`lib/src/core`)

`api_client.dart` — the four list calls return envelopes and take the query:

```dart
Future<Paged<Tenant>> tenants(ListQuery query) async {
  final res = await dio.get<Map<String, dynamic>>(
    '$basePath/tenants',
    queryParameters: query.toQueryParameters(),
  );
  return _paged(res.data, Tenant.fromJson);        // the existing _list<T> helper's sibling
}
```

| Method | Returns |
| --- | --- |
| `tenants(ListQuery)` | `Paged<Tenant>` |
| `tenantOptions()` | `OptionList<Tenant>` (calls `/tenants/options`) |
| `roles(ListQuery)` / `roleOptions({String? tenantId})` | `Paged<Role>` / `OptionList<Role>` |
| `permissions(ListQuery)` | `Paged<Permission>` |
| `users(ListQuery)` | `Paged<User>` |

`console.dart` (`ConsoleScope`) passes the query through and keeps its invariant that a tenant console
never sends a tenant parameter: `users(query)` calls the API with `query.copyWith(tenantId: null)` when
`!isPlatformPlane`, and `roleOptions` is likewise plane-aware.

`providers.dart`:

| Provider | Change |
| --- | --- |
| `tenantsProvider` | **replaced** by `tenantsPageProvider = FutureProvider.autoDispose.family<Paged<Tenant>, ListQuery>` and, for pickers, `tenantOptionsProvider = FutureProvider<OptionList<Tenant>>`. |
| `rolesProvider` / `permissionsProvider` / `usersProvider` | **replaced** by `rolesPageProvider` / `permissionsPageProvider` / `usersPageProvider`, all `autoDispose.family<…, ListQuery>` keyed by the query, delegating to `consoleProvider`. |
| `roleOptionsProvider` | **new** `autoDispose.family<OptionList<Role>, String?>` (the role checklist in the user editor). |
| `tenantByIdProvider` | unchanged in shape, now sourced from `tenantOptionsProvider` — a tenant that is not on the currently displayed page must still resolve (the deep link `/tenants/{id}` no longer has a full list to search). |

`admin_shell.dart` sign-out invalidation switches to the new provider names (family invalidation still
drops every instance).

## Artifacts — shared widgets (`lib/src/core/lists.dart`)

The guideline's list rules, once, as widgets a screen opts into:

| Widget | Responsibility (maps to `docs/UX_GUIDELINES.md` §1) |
| --- | --- |
| `SearchField` | Debounced (300 ms) search box with a labelled `InputDecoration`, `Icons.search` prefix and a tooltip'd clear button; owns its controller but re-syncs from the incoming `value`, so an external *Clear search* empties the field too. (Rules 3, 4, 11) |
| `ListToolbar` | Lays out `SearchField`, the screen's filters, `SortSelect` and `ListSummary` ("1–25 of 142") the same way on every screen. (Rules 2, 6, 11) |
| `PagedListView<T>` | Renders `AsyncValue<Paged<T>>`: error → `MessagePanel` with *Retry* against the same query; `totalElements == 0` with an empty search → "nothing yet" panel; `totalElements == 0` with a search → "nothing matches '<term>'" panel with *Clear search*; otherwise the rows plus `PaginationBar`. While refreshing it keeps the previous rows and shows a `LinearProgressIndicator` above them. (Rules 7, 8, 9) |
| `PaginationBar` | First/previous/next/last (disabled at the ends, never hidden), "Page 2 of 6", and the page-size selector (25/50/100). (Rules 6, 11) |
| `SortSelect` | Dropdown of the resource's sort keys plus an asc/desc toggle — the console renders `ListTile` rows, not a table, so a dropdown is the honest control. |

## Artifacts — screens

| Screen | Change |
| --- | --- |
| `tenants_screen.dart` | `ListQuery` in state, `ListToolbar` (search only) + `PagedListView<Tenant>`; create/edit/delete invalidate `tenantsPageProvider` **and** `tenantOptionsProvider`. |
| `users_screen.dart` | Tenant filter fed by `tenantOptionsProvider` + search; the list moves into the paged `UserList`. |
| `user_editor.dart` | `UserList` takes a `ListQuery` (search/page) and renders through `PagedListView`; `tenant_users_screen.dart` passes a query with only the search/page set; `_RoleChecklist` reads `roleOptionsProvider` instead of `rolesProvider(null)`, and shows the plane's roles incl. the `/options` truncation hint when `truncated`. User create/edit/delete/reset invalidate `usersPageProvider`. |
| `roles_screen.dart` / `permissions_screen.dart` | `OwnerFilter` fed by `tenantOptionsProvider`, a `scope` filter (open question 2), search and `SortSelect`; create invalidates the matching paged provider. |
| `owner_filter.dart` | Source changed to `tenantOptionsProvider`; while loading it offers only *All owners* (the graceful degradation the roles screen already relies on when the caller cannot read tenants). |

**Every screen resets `page` to 0 on a new search or filter** (rule 4) — the one bug this whole phase
exists to avoid is a filter change that leaves the user on page 7 of a 2-page result set.

## List state in the URL (open question 3)

When confirmed, the four screens read their initial `ListQuery` from
`GoRouterState.of(context).uri.queryParameters` and write changes back with `context.replace(...)`
(debounced together with the search), behind one small helper in `lists.dart` so no screen builds a URL by
hand. Refresh, back/forward and a shared link then preserve `q`, `page`, `size`, `sort` and the filters.

## Decisions

- **The filter/page state lives in `ListQuery` (freezed), not in six separate `setState` fields** — one
  value is the provider key, the URL payload and the "is this a different query?" check.
- **Providers stay `FutureProvider.autoDispose.family`** as the file already uses (rather than moving to
  `Notifier`/`AsyncNotifier`): the change is about the query, and a `family` keyed by `ListQuery` is the
  same shape the codebase already relies on for `autoDispose` caching.
- **Debounce lives in `SearchField`**, not in each screen, so a new list screen cannot forget it.
- **The console keeps its `ListTile` rows** (no data-table rewrite) — phase 5 is about search/paging, and a
  table would silently change every screen's look and drag in responsive/overflow work.
- **`Paged<T>`'s totals are trusted as sent**; the UI never recomputes `totalPages`, so client and server
  cannot disagree about the last page.

## Dependencies

- Phases 3 and 4 (the contract the client consumes), phase 1 (the UX rules).

## Execution (2026-09-28)

| Artifact | Delivered |
| --- | --- |
| `models/list_query.dart` | `ListQuery` (freezed) with `toQueryParameters()`, `withSearch`/`withTenant`/`withScope`/`withSort`/`withSize` (each resetting the page) plus `onPage` and `withoutTenant` (which keep it). Generated `list_query.freezed.dart` / `.g.dart` written by `build_runner`. |
| `models/envelopes.dart` | `Paged<T>` and `OptionList<T>` — hand-written immutable generics, with `isBeyondEnd`, `rangeLabel` and `pageLabel`. |
| `core/api_client.dart` / `console.dart` | The four list calls return `Paged<T>` and take a `ListQuery`; `tenantOptions()` / `roleOptions()` read `/options`; `ConsoleScope` drops a tenant a tenant-plane route cannot name — **without** resetting the page (`withoutTenant`). |
| `core/providers.dart` | `tenantsPageProvider`, `rolesPageProvider`, `permissionsPageProvider`, `usersPageProvider` (`autoDispose.family` keyed by `ListQuery`), `tenantOptionsProvider`, `roleOptionsProvider`, and `tenantByIdProvider` now sourced from the options list so a deep link resolves off-page. |
| `core/lists.dart` (new) | `ListQueryLocation`, `SearchField` (300 ms debounce, labelled, clearable), `ListToolbar`, `SortOption`/`SortSelect`, `PaginationBar`, `PagedListView` (two empty states, stale-page recovery, retry, keeps the previous page for the same result set). |
| Screens | `tenants_screen`, `users_screen`, `tenant_users_screen`, `roles_screen`, `permissions_screen` and `UserList`/`_RoleChecklist` in `user_editor.dart`; every list is now `ListToolbar` + `PagedListView`, with the URL written on each change. |
| `owner_filter.dart` | `OwnerFilter` re-sourced from the options list, joined by a new `TenantFilter` (users) and `ScopeFilter` (roles/permissions). |
| `admin_shell.dart` | Sign-out invalidates the six new providers instead of the four old ones. |
| `keystone_admin_ui.dart` | Exports `lists.dart`, `envelopes.dart` and `list_query.dart`. |

**Deviations and decisions beyond the plan**

- **Toolbar alignment fixed after review (the reported defect).** The first cut centred the toolbar's
  children (`WrapCrossAlignment.center`) while they have different heights — a filter with helper text is
  taller than a plain field, and the sort direction toggle was its own 48 px control beside the field — so
  the tenant/owner filter and the sort control appeared out of line, on every list screen. Three changes,
  all in the shared widgets, so every screen is fixed at once:
  `ListToolbar` aligns by the top edge (`WrapCrossAlignment.start`); `SearchField`'s label always floats
  (`FloatingLabelBehavior.always`), matching the dropdowns instead of sitting inline until the first
  keystroke; and `SortSelect`'s direction toggle is now the sort field's `suffixIcon` with tight
  constraints, so it cannot change the field's height. The rule was missing from the guideline, so it was
  **added as `docs/UX_GUIDELINES.md` §1.16** ("a row of controls shares one line") together with its
  mapping in §2.
- **`ScopeFilter` lives in `owner_filter.dart`** rather than in each screen: roles and permissions need the
  same control, and the file is already the home of the platform plane's list filters.
- **The tenant drill-down writes its URL without the tenant**, because the tenant is already the path
  (`/tenants/{id}`); it is re-applied to every query from the path parameter, never typed by the user.
- **The screens read the URL once, in `didChangeDependencies`**, not on every build: the URL is an input at
  first paint and an output afterwards, which is what keeps a debounced search from fighting the router.
- **`PagedListView` only carries the previous page over when the *result set* is unchanged** (same search,
  tenant, scope, sort) — paging keeps the rows visible, a new filter shows a spinner rather than rows that
  answer a different question.
- **`Tenant`'s country is still displayed and now sortable**; the tenants screen gained a sort control but
  deliberately not a country filter (not requested).

## Verification

- `cd platform/keystone-admin-ui && flutter analyze` → **No issues found**; `flutter test` → **96 tests
  passed** (was 66 before the delivery): `envelopes_test` (7), `list_query_test` (8), `lists_test` (15) new,
  plus the updated `console_test` and `user_editor_test` fixtures (which now assert the paging parameters,
  the options route, and that a tenant-plane query keeps its page when the unnameable tenant is dropped).
  `lists_test`'s `should_line_up_controls_of_different_heights` is the regression guard for the toolbar
  alignment, and it also documents that a wide surface is the case where alignment is visible (the toolbar
  wraps into stacked rows on a narrow one, by design).
- `cd apps/inventory/frontend && flutter analyze` → **No issues found**.
- `build_runner` regenerated the freezed/json code for `ListQuery`; the generated files are committed, as the
  package already does for its models.


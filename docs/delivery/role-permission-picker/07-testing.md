# Phase 7 — Testing

**Scope** — unit tests for the three client rules (phase 3) and widget tests for the picker and the create-role
dialog (phase 5). No backend test is added: the routes the picker calls are already covered on both planes.

**Artifacts**

| File | Change |
| --- | --- |
| `test/permission_picker_test.dart` **(new)** | The picker end to end through the create-role dialog, against a recording `dio` adapter (the pattern `user_editor_test.dart` established). |
| `test/permission_row_test.dart` **(new)** | One permission renders identically in both modes, and a non-grantable code renders disabled. |
| `test/permission_selection_test.dart` **(new)** | `PermissionSelection.toggle` / `retaining` / `codes`. |
| `test/models_test.dart` | `Me.canGrant` cases. |
| `test/list_query_test.dart` | `ListQuery.permissionsFor` seeds owner + scope and resets to page 1. |

**Dependencies** — phases 3 and 5 (the tests are written against them).

**Verification** — `flutter test` green, `flutter analyze` clean, `dart format --set-exit-if-changed` clean.
Baseline before this delivery: **96 tests** (recorded by `search-and-paging` phase 5) — the count after is
reported per file, and no existing test is weakened or deleted.

## Acceptance criteria → tests

| # | Criterion (phase 1) | Test |
| --- | --- | --- |
| A1 | No typed permissions | `permission_picker_test` → `should_not_offer_a_permission_text_field` (the old label is gone; the summary appears after a pick) |
| A2 | Server-side search over the whole catalogue | `should_search_the_catalogue_on_the_server` — entering text issues `q=…&page=0`, and the rendered rows are exactly the adapter's answer for that term (a locally-filtered list would show different rows) |
| A3 | Server-side sort and paging | `should_page_and_sort_by_request` — `sort=code&order=desc`, `page=1`, `size=50` appear in the request |
| A4 | Result set described from the server's totals | `should_describe_the_result_set_from_the_server_totals` — `1–25 of 43 · Page 1 of 2` from the envelope |
| A5 | Only grantable rows offered (query pinned) | `should_seed_the_picker_with_the_roles_owner_and_scope` — a global `TENANT` role asks `scope=TENANT` with no `tenantId`; a tenant-owned role asks `tenantId=<owner>` |
| A6 | Selection survives paging/search | `should_keep_a_pick_made_on_another_page` — pick on page 0, page to 1, pick, Apply → the created role carries both codes, sorted |
| A7 | Owner/scope change prunes | `should_drop_a_pick_the_new_scope_cannot_use` — the notice is shown and only the still-valid code is sent |
| A8 | Non-grantable rows disabled | `should_disable_a_permission_the_caller_may_not_grant` (picker, tenant plane) + `permission_row_test` → `should_render_a_row_the_caller_may_not_grant_as_disabled` |
| A9 | Shared empty/error/retry states | `should_show_the_servers_detail_with_a_retry` (an erroring adapter) and `should_show_the_empty_catalogue_state` |
| A10 | Tenant plane sends no tenant | `should_send_no_tenant_from_a_tenant_console` |
| A11 | One row widget for both screens | `permission_row_test` → `should_render_the_code_scope_level_and_owner` |

## Rules under test (unit)

- `PermissionSelection.toggle` adds and removes by **code**, matches the row it kept, and `codes` is sorted
  (deterministic request bodies).
- `PermissionSelection.retaining` keeps a pick when the owner is unchanged, when the new owner is `null`
  (a global role may hold any owner's row), when the pick is global (grantable to every owner) and when the
  scope matches exactly; drops it otherwise; and returns the dropped codes for the dialog's notice.
- `Me.canGrant` — the platform plane is unrestricted; a tenant caller cannot grant the wildcard, can grant a
  code it holds, can grant the `read-only` sibling of a held `read-write` code, and cannot grant a code with
  an unknown level.
- `ListQuery.permissionsFor` sets `tenantId` + `scope`, starts at page 0 with size 25 and no `sort`, and its
  `toQueryParameters()` omits `tenantId` when the owner is null.

## Notes

- The widget tests drive the **real** `RolesScreen` → *Add role* → *Choose permissions*, so they cover the
  wiring as well as the dialog; the adapter answers `/api/v1/roles` and `/api/v1/tenants/options` too.
- Error responses use the RFC 9457 body shape `{"detail": "…"}` that `apiErrorMessage` reads, so the tests
  assert the message a user would actually see.
- Everything runs without a backend or Docker; the picker's requests are asserted, not the database.

## Execution record (2026-09-28)

**Suites added (40 tests; the package went from 96 to 136):**

| File | Tests | What it covers |
| --- | --- | --- |
| `test/permission_selection_test.dart` **(new)** | 10 | Picking/unpicking by code, immutable values, code ordering, `remove`, `ofRole`, and `retaining` for every owner/scope case the grant rule has (own owner, global row, global role, another tenant's row, another scope). |
| `test/permission_row_test.dart` **(new)** | 4 | The catalogue row's code/scope/level/owner, the tenant-owned icon and label, the checkbox mode, and the disabled row (asserting `onChanged == null` **and** `enabled == false` — the defect noted in phase 5). |
| `test/permission_picker_test.dart` **(new)** | 19 | The picker's own rules (seeding, server-side search, paging, size, sort, the conditional owner filter, the stated scope, keeping picks across pages, chip removal, disabled rows + no tenant parameter on the tenant plane, the server's error + Retry, the empty state) and the screen's wiring (no text field; create; prune-on-scope-change; edit; no *Edit* for a seeded role; the withheld picker without the read grant). |
| `test/models_test.dart` | +6 | `Me.canGrant` — platform exempt, a held code, write-implies-read (and not the reverse), the wildcard, a code with no level — plus `isSeededAdminRole` both ways. |
| `test/list_query_test.dart` | +1 | `ListQuery.permissionsFor` seeds owner + scope, stays on page 1 with the server's order, and drops no parameters. |

The picker tests run against a **fake backend** (`_CatalogueAdapter`) that filters, orders and pages the
catalogue exactly as `PermissionService` does — filtering by `scope`, the `tenantId` owner rule (reserved id →
global only, a tenant id → global + that tenant), a case-insensitive `q`, and a page window with the totals it
computed. That is what makes A2 and A5 real assertions: a row that sorts onto page 2
(`tenant:user:read-only`, 41st of 43) appears only after a search the *server* performed, and every requested
query is asserted verbatim.

**Two test-only findings**

1. `CheckboxListTile` fires its tap handler from `onChanged`, not from `enabled` — so the first cut of a
   disabled row still toggled. Fixed in `permission_row.dart` (pass `onChanged: null` as well) and guarded by
   `permission_row_test`.
2. `ListView.builder` only builds the rows in view, so a test may not assert on an arbitrary row of a page; the
   paging and cross-page-selection tests assert the *window* (the summary, the first row of the new page, the
   absence of the old page's first row) instead of a row that may be off-screen.

**Verification**

```
cd platform/keystone-admin-ui && flutter analyze   →  No issues found!   (1.5s)
                                  flutter test      →  136 passed
cd apps/inventory/frontend      && flutter analyze   →  No issues found!
```

(Flutter 3.47.5 / Dart 3.13.4 at `/Users/rajeshdebnath/manual-install/flutter/bin`.) No test was removed or
weakened: the 96 pre-existing tests all still pass, and `dart format` was deliberately not used as a gate — see
the formatter note in `05-frontend-ui-flutter.md`.

# Keystone — UX Guidelines

> **Status:** Accepted · **Last updated:** 2026-09-28
>
> The UX counterpart to `docs/ARCHITECTURE.md` and the two coding guidelines. The coding
> guidelines say *how* to build; this document says what the **user** must experience. The rules
> here are **normative** in the same spirit: a screen that deviates is either fixed or carries a
> reviewed justification, stated in a comment or the pull request.
>
> Applies to the platform admin console (`platform/keystone-admin-ui`), the tenant
> self-service console it shares, and every application's own UI (`apps/<app>/frontend`).

## How to read this document

- Each section covers one **user interaction pattern** and applies to every screen of that kind —
  not to one app or one screen.
- **MUST** is required; **SHOULD** needs a written reason to deviate.
- Where a rule is enforced by shared code, that code is named. Those widgets and endpoints exist
  so no screen has to re-implement a rule — or forget it.

## 1. List screens: search, filtering, sorting & paging

A *list screen* is any screen that shows a collection of records: tenants, users, roles,
permissions, items, orders. Its rules are the most-often-broken part of a console, because a list
that works perfectly with twelve rows behaves surprisingly with twelve hundred.

### 1.1 The server owns the collection

A list MUST be fetched one page at a time from a **paged, searchable, filterable** endpoint
(contract: `docs/CODING_GUIDELINES_BACKEND.md` §8). The client MUST NOT download a whole
collection and filter it in memory: with more rows than fit on a page, an in-memory filter searches
only the page the user happens to be looking at.

### 1.2 Search covers the whole collection, never just the page

The search box sends the term to the server, and the server searches **every row the caller may
see**. The result language says so — *"12 results for 'acme'"* — never *"3 of 25 rows match"*.

### 1.3 Search says what it searches

The field names the searched columns (`Search name, slug or country`,
`Search username or email`), because they differ per resource. A bare "Search" box leaves the user
guessing whether it matches an id, a label or a phone number.

### 1.4 Typing is debounced, and a new query starts at page 1

Search input is debounced (300 ms recommended) so a pause — not every keystroke — is a request.
**Any** new search or filter returns to the **first page**: leaving the user on page 7 of a
now-2-page result set is a defect, not a preference.

### 1.5 Filters compose with search

Changing a filter MUST NOT clear the search term, and typing a term MUST NOT reset a filter. Each
has its own deliberate *Clear*. Multiple filters combine with AND, and the screen shows which ones
are active.

### 1.6 Filter on what the row shows

Offer a filter only for values the row already displays (tenant/owner, scope, status, type). A
filter whose effect the user cannot see in the rows is worse than no filter at all.

### 1.7 The result set is always described

Every list shows the visible range and the total (`1–25 of 142`), the current page (`Page 2 of 6`),
and a page-size control (25 / 50 / 100). Numbers come from the server's totals, never from
`items.length` alone — otherwise "last page" is a guess.

### 1.8 Two empty states, never one

- **Nothing exists yet** → the resource's empty state with the **create** action
  (`No tenants yet.` + *Add tenant*).
- **Nothing matches** → the search echoed back with a **Clear search** action
  (`Nothing matches "acme".`), and no create action.

A blank panel, and the same generic "No data" for both cases, are both defects: the user cannot tell
"there is nothing here" from "my filter hid everything".

### 1.9 Loading never blanks the screen

While the next page or a new query loads, the previous rows stay visible with a **secondary**
indicator (a thin progress bar, a subtle opacity), instead of replacing the list with a centred
spinner. The user keeps their context and the layout does not jump.

### 1.10 Errors keep the query

A failed page shows the error where the list was, with a **Retry** action that repeats the **same**
query (same term, filters, sort and page). Losing the user's search term to a network blip is a
defect; make them re-type it and they will stop searching.

### 1.11 Sorting is explicit and honest

Where a list is sortable, the control offers the keys the **server** accepts, and the screen shows
which key and direction are active. The default order is the server's; the client never re-sorts a
page, because a client-side sort reorders only the rows it holds.

### 1.12 List state lives in the URL

The search term, filters, page, size and sort are query parameters. Refreshing keeps the list as it
was, back/forward moves through the queries the user ran, and a filtered list can be linked to a
colleague. A console where a refresh throws the filter away is a console that is used in tabs.

### 1.13 Pickers use `/options`, never a page

A dropdown, select or checklist that must show **all** choices is fed by the resource's unpaged
`/options` endpoint (`OptionList<T>`: the complete set, capped, with `truncated` surfaced — never
silently cut off). A paged list MUST NOT feed a picker: it would silently offer only the first page.
When the number of options approaches the cap, the control becomes **type-to-search** instead of a
longer dropdown.

### 1.14 Accessible by default

The search field has a visible label (a placeholder and an icon are not a label), the clear
affordance has a tooltip, the row actions have accessible names, pagination controls are reachable
by keyboard and are **disabled** (never hidden) at the ends, and the result summary is readable text
rather than an image or an aria-hidden ornament.

### 1.15 One visual language for loading, empty and error

Loading, empty and error states are rendered through the shared `MessagePanel` (icon + message +
optional action). A screen MUST NOT hand-roll its own empty-state layout, colour or wording; the
console's states must look like one product, and a new screen must get them for free.

### 1.16 A row of controls shares one line

Controls in a toolbar are aligned by their **top edge**, never by their centres. They are not all the same
height — a filter carrying helper text is taller than one without, and a control with a trailing toggle is
taller than a plain field — so centring them puts one control's label between two lines of its neighbour,
which reads as a broken toolbar (it is the easiest layout bug to ship and the most visible).

- A control that *belongs to* a field is rendered **inside** that field's decoration, so it cannot shift
  the field's height: a direction toggle beside a sort key is a `suffixIcon`, a clear action is the search
  box's own action.
- A **search box's label always floats**, like the dropdowns beside it; a label that begins inside the box
  and moves up on the first keystroke leaves an empty toolbar looking misaligned.
- Helper text hangs **below its own field** and never becomes the reason a control moves.

### 1.17 A picker over a set too large for one control is a paged list

A picker that can show its whole set compactly — a dropdown, a short checkbox list — MUST be fed by the
resource's unpaged `/options` route (§1.13). A picker whose set can outgrow one control (a permission
catalogue that every tenant may add to, a product list) MUST NOT fall back to a **text box** — asking a user
to recall and spell an exact code — and MUST NOT present a truncated `/options` response as if it were
complete. It is instead a **browsable picker**: a list with its own server-side search, filters, sort and
paging (§1.1–1.7 apply to it unchanged), whose **selection is accumulated across pages**, so a choice made on
one page is never lost and every row stays reachable.

- The picker MUST be **seeded with the query that makes every offered choice valid** — the same scope, owner
  or other constraint the user is choosing within — and MUST NOT offer a choice the server will reject. Where
  that constraint is fixed by another control, it is **stated as text** rather than shown as a filter that
  could only produce failures (§1.6).
- A choice the caller may not make (a grant they do not hold, a value their plane cannot set) is rendered
  **disabled, with the reason on it**, never silently selectable and then refused on save.
- The picker's own state is a modal's, not a screen's: it is not written to the URL (§1.12 is about screens),
  and a second `page`/`q`/`sort` set in the same query string would corrupt the screen's own list state.
- The chosen set MUST be reviewable before it is submitted (a count and a removable chip per choice), because
  a selection gathered across pages cannot be verified by looking at one page.

## 2. Where these rules live in the code

| Rule | Enforced by |
| --- | --- |
| 1.1, 1.2, 1.4, 1.6, 1.7 (server-side paging, search, filtering, totals) | `PageRequest` / `Page` / `SearchTerm` / `SortOrder` / `OptionList` in `platform/keystone-common`, `Search` in `platform/keystone-data`, `QueryParams` in `platform/keystone-web` |
| 1.2, 1.3, 1.4, 1.5, 1.7, 1.8, 1.9, 1.10, 1.11, 1.14, 1.15 (list UX) | `SearchField`, `ListToolbar`, `PagedListView`, `PaginationBar`, `SortSelect` and `MessagePanel` in `platform/keystone-admin-ui/lib/src/core/` |
| 1.16 (one row of controls shares one line) | `ListToolbar` (`WrapCrossAlignment.start`), `SearchField` (always-floating label), `SortSelect` (the direction toggle is a tightly-constrained `suffixIcon`); guarded by `lists_test.dart` |
| 1.12 (list state in the URL) | the console's `go_router` routes |
| 1.13 (complete sets for pickers) | the resources' `/options` routes + `OptionList<T>` |
| 1.17 (a large picker is a paged list) | `showPermissionPicker` + `PermissionRow` in `platform/keystone-admin-ui/lib/src/features/admin/` — a `Dialog` built from `SearchField`/`ListToolbar`/`SortSelect`/`PagedListView`, seeded by `ListQuery.permissionsFor(...)` and pruned by `PermissionSelection` |

A new list screen is expected to be built **from** these pieces. If a piece is missing, add it
there — in the platform library — so the next app inherits the rule instead of re-learning it.

## 3. Related documents

- `docs/ARCHITECTURE.md` — system design, the two admin planes, authorization, data flows.
- `docs/CODING_GUIDELINES_BACKEND.md` §8 — the REST list contract (§8 *List endpoints*) these
  screens rely on.
- `docs/CODING_GUIDELINES_FRONTEND.md` §7.3 — how to build a list screen with the shared widgets.
- `docs/delivery/search-and-paging/` — the delivery that introduced §1, with the reasoning and the
  open questions behind each decision.

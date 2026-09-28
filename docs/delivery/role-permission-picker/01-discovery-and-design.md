# Phase 1 — Discovery & design

**Scope** — confirm the requirement and its acceptance criteria, fix the contract the picker uses, write the
rules down (query-from-role, selection, grantability) and draft the one normative UX rule this delivery adds.
No code in this phase.

**Artifacts** — this file; the plan's *Decisions* and *Open questions* sections; the `docs/UX_GUIDELINES.md`
§1.17 draft below (applied in phase 8, so the guideline lands with the code that proves it).

**Dependencies** — none. Everything below was read from the repository: `roles_screen.dart` (the typed
field), `permissions_screen.dart` + `core/lists.dart` (the list kit to reuse), `RoleService` /
`PermissionService` / `CallerScope` (what is grantable), `AdminIntegrationTest` / `TenantSelfServiceIntegrationTest`
(the route contract), `docs/UX_GUIDELINES.md` §1.

**Verification** — each acceptance criterion is mapped to a test in `07-testing.md`; each contract claim is
cited to the file and line it was read from.

## The request, restated

> In Roles, let the permissions be selectable through various filters, sorting, paging etc. instead of hand
> typing the permissions.

The one place permissions are chosen today is the create-role dialog's free-text field
(`roles_screen.dart`, `_CreateRoleDialogState._permissions`, label `Permissions (comma-separated)`, hint
`tenant:user:read-only, tenant:role:read-write`). It must become a **picker over the permission catalogue**
with server-side search, filtering, sorting and paging — the same list rules every other collection in the
console follows.

## Acceptance criteria

| # | Criterion |
| --- | --- |
| A1 | The create-role dialog has no permission text field: permissions are chosen from a list, and the dialog shows the count and the chosen codes. |
| A2 | The picker searches **the whole catalogue** server-side (`q=`, debounced 300 ms), never the rows it happens to hold. |
| A3 | The picker sorts server-side (`sort` + `order` on the resource's whitelisted keys, with the direction toggle inside the sort field) and pages server-side (`page`, `size` 25/50/100, disabled-not-hidden at the ends). |
| A4 | The result set is always described (`1–25 of 43 · Page 1 of 2`) from the **server's** totals. |
| A5 | Every offered row is one the backend would accept for this role's owner + scope — verified against `RoleService.grantPermissions` (`Unknown permission` / `Permission scope mismatch`). |
| A6 | A selection made on one page survives paging, a new search, a filter change and reopening the picker. |
| A7 | Changing the role's owner or scope drops the selections that no longer match, and says so. |
| A8 | On the tenant plane, a code the caller cannot grant (`CallerScope.requireGrantable`) is disabled with the reason, not silently selectable. |
| A9 | The picker's empty state, error state and retry are the shared ones (`MessagePanel`, `PagedListView`), not hand-rolled. |
| A10 | Both planes work, and a tenant console still sends no tenant parameter (its route derives the tenant from the caller). |
| A11 | The permission rows look the same in the picker and on the Permissions screen (one widget). |

## The contract (already in place — nothing to build)

| Route | Plane | `q` | filters | `sort` | default order |
| --- | --- | --- | --- | --- | --- |
| `GET /api/v1/permissions` | platform | `code` contains, ci | `tenantId` (null = every owner · reserved platform id = global catalogue only · tenant id = global + that tenant), `scope` (`PLATFORM`\|`TENANT`) | `code` · `createdAt` · `updatedAt` | `tenant_id ASC NULLS FIRST, code ASC, id ASC` |
| `GET /api/v1/tenant/permissions` | tenant | as above | `scope` only — a `tenantId` is **refused** (`PermissionService.effectiveOwner`) and the tenant comes from the caller's own row | as above | as above |

Both return the page envelope `{items, page, size, totalElements, totalPages, hasNext, hasPrevious}`
(`Paged<T>`) and both are gated by the *permission* resource's read grant
(`guard.requireRead(ctx, PLATFORM_PERMISSION | TENANT_PERMISSION)`), which is **not** the role resource the
screen is gated on — see phase 6 (`06-security-and-observability.md`).


## The grantability rule (read from the backend, mirrored by the picker)

`RoleService.grantPermissions(tx, roleId, owner, scope, codes)` accepts a code only when **both** hold:

1. **owner** — the permission's `tenant_id` satisfies `ownerFilter(owner, PERMISSIONS.TENANT_ID)`:
   `owner == null` → no restriction; the reserved platform id → `tenant_id IS NULL`; a tenant id →
   `tenant_id IS NULL OR tenant_id = <that tenant>`; and
2. **scope** — `permission.scope == role.scope`, otherwise `ValidationException("Permission scope mismatch
   for role …")`; a code not visible to the owner at all is `ValidationException("Unknown permission: …")`.

A code carried by both a global and a tenant row resolves to the **global** row (the catalogue is canonical).

`CallerScope.requireGrantable(codes)` adds the escalation guardrail *on top*: a tenant caller may not grant
the wildcard, and may not grant a code it does not hold — where "holds" follows the guard's implication
(a `…:read-write` grant covers `…:read-only` of the same resource). The platform plane is exempt.

So the picker's offered set is **the list route's visibility rule with `scope` and `tenantId` pinned to the
role's own values** — exactly the table in `plan.md`, and exactly what `permissionsPageProvider` already asks
for. Nothing new is needed from the server to enforce it: the rules are restated here because the *pins* are
what keep the picker honest.

## The normative UX rule this delivery adds (draft for §1.17)

> ### 1.17 A picker over a set too large for one control is a paged list
>
> A picker that can show its whole set compactly (a dropdown, a checkbox list) MUST be fed by the
> resource's unpaged `/options` route (§1.13). A picker whose set can outgrow one control — a permission
> catalogue, a product list — MUST NOT fall back to a text box, and MUST NOT present a truncated `/options`
> response as complete. It is instead a **browsable picker**: a list with its own server-side search,
> filters, sort and paging (§1.1–1.7 apply to it unchanged), whose **selection is accumulated across
> pages**, so a choice made on another page is never lost and every row remains reachable.
>
> Such a picker MUST also be **seeded with the query that makes every offered choice valid** — the same
> scope/owner the caller is choosing within — and MUST NOT offer a choice the server will reject. Where the
> offered set is fixed by another control (a scope derived from the resource), that control is stated as
> text rather than shown as a filter that could only produce failures (§1.6).

## Decisions taken here

1. Browsable picker over a per-permission query (open question 1: the access-level filter is a follow-up).
2. Pin `scope`; `owner` is a filter only for a global role on the platform plane (`plan.md`, the table).
3. Selection = `Map<String, Permission>`; a `Set<String>` cannot answer "is this row still valid?".
4. The picker is a dialog; its list state is local, not in the URL (§1.12 is about screens).
5. The permission **row** is shared with the Permissions screen; the picker renders it with a checkbox.
6. Disable-not-hide for a code the caller may not grant (open question 3).
7. No backend change; `/options` is not extended to permissions — a 500-row cap cannot serve a catalogue that
   grows with every tenant, and the paged list *is* the better control.

## Open questions carried into confirmation

See `plan.md` → *Open questions* 1–5. None blocks phase 3 or 5 as designed; question 1 (access filter) and
question 2 (edit role) would add work; question 5 is the guideline text above.

## Verification

- Every acceptance criterion has a named test in `07-testing.md` (A1–A11 → the table there).
- Each contract claim above was read from the file named beside it, and the two integration tests named in
  `plan.md` already assert the route's `q`/`scope`/`tenantId` behaviour on both planes — so no new backend
  test is required for the requests the picker makes.


## Execution record (2026-09-28)

- **Confirmed before coding:** open question 2 answered *yes* (edit role is in scope); 1, 3, 4 and 5 accepted
  as recommended. `plan.md` records the confirmation.
- **Contract re-verified against the code**, as written above: `RoleHandler`/`TenantRoleHandler` read
  `tenantId`/`scope`/`q`/`page` through `QueryParams`, `PermissionService.list` filters with
  `ownerFilter(...) ∧ scopeFilter(scope) ∧ Search.containsIgnoreCase(q, CODE)` and returns `Page<PermissionDto>`,
  and both permission routes are behind the permission resource's read grant. No backend change was needed, so
  phases 2 and 4 stayed skipped.
- **A5's rule was confirmed twice during phase 7** — `AdminIntegrationTest`/`TenantSelfServiceIntegrationTest`
  already cover the route behaviour, and the new Flutter tests assert the picker's *requests* against a
  fake backend that answers exactly as the server does.
- **§1.17 landed as drafted** (`docs/UX_GUIDELINES.md` §1.17 + a §2 mapping row), with one sentence added that
  the draft did not have: the chosen set must be reviewable before it is submitted (the chips) — the plan's
  phase 5 made that a requirement, so the guideline says it too.

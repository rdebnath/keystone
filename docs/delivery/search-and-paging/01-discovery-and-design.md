# Phase 1 — Discovery & design

## Scope

Turn the request ("search and paging in tenant, users, roles, permission — on the backend, and make it
a common guideline for UX") into (a) one normative **list contract** and (b) the **UX guideline** that
makes it repeatable. No code in this phase: the two documents below *are* the deliverable, and phases
2–8 implement them.

## Artifacts

| Artifact | Change |
| --- | --- |
| `docs/UX_GUIDELINES.md` | **New.** The common UX guideline. Its first section is normative for list screens (below); later UX rules get their own sections, so this doc is the home for UX rules rather than a third subsection hidden in the frontend guidelines. |
| `docs/CODING_GUIDELINES_BACKEND.md` | §8: the one-line *"Paginate list endpoints (`page`, `size`, `sort`) and return a typed page wrapper"* (line 414) is expanded into the normative REST list contract, as a new *List endpoints* subsection of §8; §13 gains the envelope as a typed record. |
| `docs/CODING_GUIDELINES_FRONTEND.md` | New subsection (§7.3 *List screens*) pointing at the shared widgets and at `docs/UX_GUIDELINES.md` §1; §14 gains `Paged<T>`. |
| `AGENTS.md` | "Read first" gains `docs/UX_GUIDELINES.md` (the guideline is only useful if it is read). |
| `docs/ARCHITECTURE.md` | §5.1 (REST data flow) and §9.2 (*"`GET /api/v1/tenants` returns it first"*) updated to the paged contract. |
| `docs/delivery/search-and-paging/` | This plan and its phase files. |

## The UX guideline (`docs/UX_GUIDELINES.md` §1, drafted here)

Normative rules for every list screen (platform console, tenant console, and every app's own console):

1. **Every list is server-paged.** No screen ever loads a whole collection to filter it client-side.
   Search and filtering are query parameters; the client renders what the server returned **plus** the
   server's totals.
2. **Search covers the whole collection, not the page.** The result panel says so explicitly:
   *"12 results for 'ac'"*, never *"3 of 25 rows match"*.
3. **Search is explicit about what it searches.** The placeholder names the fields (`Search name, slug
   or country`), because the columns differ per resource.
4. **Typing is debounced (300 ms) and resets to page 1.** A new search/filter always returns to the
   first page; keeping page 5 of the old result set is the classic empty-list bug.
5. **Filter changes reset the page and keep the search term.** Search + filter compose; neither silently
   clears the other (a *Clear* affordance clears each one deliberately).
6. **State is visible and recoverable.** Result count (`1–25 of 142`), current page (`Page 2 of 6`),
   page size (`25 / 50 / 100`) are always shown; navigation is first/previous/next/last.
7. **Two distinct empty states.** *Nothing exists yet* (with the create action) vs *nothing matches*
   (with the term echoed and a **Clear search** action). Never one generic "No data".
8. **Data stays on screen while the next page loads.** The list keeps the previous rows and shows a
   secondary progress indicator instead of blanking to a spinner (no layout jump, no lost context).
9. **Errors keep the query.** The error panel offers *Retry* against the same query, reusing the console's
   existing `MessagePanel`.
10. **List state lives in the URL** (open question 3): search, filters, page and sort are query
    parameters, so a filtered list can be refreshed, bookmarked and shared.
11. **Accessible by default**: the search field has a label, the clear icon a tooltip, and pagination
    controls are keyboard-reachable with disabled (not hidden) ends.
12. **Pickers use `/options`, never a paged list** (open question 4): reference data for a dropdown is
    fetched from the resource's unpaged `/options` endpoint, which is capped and reports `truncated`.

## Dependencies

None. Phases 2–8 implement this contract.

## Verification

- Review: the guideline is normative, testable and free of implementation detail (it says *what* a list
  screen must do, not which widget does it).
- Cross-check every rule against the request: backend search + filtering (rule 1–5, contract), paging
  across pages (rule 2, 6, 8), and reuse by the four modules (rules are resource-agnostic).
- Each later phase states which rules it satisfies, so the guideline is not "written and forgotten".

## Execution (2026-09-28)

Phase 1 delivered, and it is the only phase with no code in it. All four open questions were answered
before a file was written (1: pinned, searchable platform row; 2: `scope` filter; 3: list state in the URL;
4: a 500-row `/options` ceiling), so these documents are the **confirmed** contract, not a proposal.

| Artifact | What changed |
| --- | --- |
| `docs/UX_GUIDELINES.md` | **Created**: purpose and how to read it, **§1 with fifteen numbered rules** for list screens (1.1 the server owns the collection … 1.15 one visual language for loading/empty/error), §2 mapping each rule to the code that enforces it, §3 related documents. |
| `docs/CODING_GUIDELINES_BACKEND.md` | §8's one-line paging bullet (line 414) now points at a new **List endpoints (search, filtering & paging)** subsection: the parameter table (`page` 0…10000, `size` 1…100, `sort`, `order`, `q` ≤ 100 chars, resource filters), the envelope JSON, and the rules — shared vocabulary (`common.query`, `keystone-data.Search`, `keystone-web.QueryParams`), bound `LIKE` with escaped wildcards, **total** ordering, a stale page is `200` + empty `items`, the sort whitelist lives in the service, paging never widens visibility, `/options` for pickers, the envelope is a typed record, and search terms are not loggable PII. §13 gained the *list envelopes are records too* rule. |
| `docs/CODING_GUIDELINES_FRONTEND.md` | New **§7.3 List screens**: consume the envelope, one `ListQuery` per screen, `autoDispose.family` keyed by the query, build from the shared widgets, never filter or re-sort a fetched page, pickers use `/options`, list state in the URL. §14 gained the `Paged<T>`/`OptionList<T>` rule, including why they are hand-written generics rather than four generated copies. |
| `docs/ARCHITECTURE.md` | §5.1 gained a fifth data-flow step for server-side paged/searched reads and `/options`; §9.2 now describes the synthetic `Keystone` tenant as the **pinned first row of the paged list** — counted in `totalElements`, searchable by `q`, and absent when the term does not match it. |
| `AGENTS.md` | "Read first" gained item 4 (the UX guideline) with the feature-delivery skill moved to 5, so the rule is read before screens are built. |
| `docs/delivery/search-and-paging/plan.md` | Confirmation state records the four confirmed answers and the start of execution; the shared-code table names `common/query` + `OptionList` and the `PageRequest` caps. |

**Verification (docs-only).** Every claim was checked against the repository *before* it was written:

- the four list services really do `selectFrom(...).fetch()` with no `limit`/`offset`
  (`TenantService.list`, `UserService.list`, `RoleService.list`, `PermissionService.list`);
- `docs/CODING_GUIDELINES_BACKEND.md` line 414 really is the paging one-liner, and §13 (not §14) is the
  JSON-and-typed-payloads section — the plan files were corrected accordingly;
- the `/options` need is real: `user_editor.dart:626` watches `rolesProvider(null)` for the role checklist,
  and `owner_filter.dart` / `users_screen.dart` / the three dialogs need *every* tenant;
- the docs' cross-references (`UX_GUIDELINES.md` §1.13, backend §8/§13, frontend §7.3/§14) were re-grepped
  after the edits, so no section points at a heading that does not exist.

No code, build or test was touched, so there was nothing to run in this phase.

## Follow-ups

- Phase 8 re-reads these documents against the shipped code, so nothing here ends up "documented but not
  implemented" (the one risk of a contract-first phase).
- `docs/UX_GUIDELINES.md` deliberately holds only §1 today; future UX rules (forms, dialogs, error copy)
  get their own sections rather than growing §1.

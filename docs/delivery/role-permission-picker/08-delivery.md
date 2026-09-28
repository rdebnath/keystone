# Phase 8 — Delivery

**Scope** — write down what shipped, where, and in what order it can be deployed; record the follow-ups.

**Artifacts**

| Artifact | Change |
| --- | --- |
| `docs/UX_GUIDELINES.md` | New **§1.17 "A picker over a set too large for one control is a paged list"** (draft in `01-discovery-and-design.md`) + a row in §2 *Where these rules live in the code* naming `SearchField`/`ListToolbar`/`SortSelect`/`PagedListView` as the enforcement, so the next app inherits the rule instead of re-learning it. |
| `CHANGELOG.md` | An entry under `[Unreleased]` → `### Changed`, stating what the user gets, that it is **console-only (no API change)**, and naming the mirrored rules (`RoleService.grantPermissions`, `CallerScope.requireGrantable`). |
| `docs/ARCHITECTURE.md` | **Reviewed.** §9.5 already states "grant only what you hold"; this delivery is the console expressing it, so a one-line cross-reference is added **only** if that section enumerates the console's affordances. Otherwise unchanged. |
| `docs/CODING_GUIDELINES_*.md` | **Unchanged.** No new backend pattern; the frontend rules (§7.3 list screens, §14 typed models) already cover everything used here. |

**Dependencies** — phases 3, 5, 6, 7 complete and green.

**Verification** — `CHANGELOG.md` entry reviewed against the diff; the §1.17 text matches what the code does
(the picker's selection survives paging, its query is seeded, no filter can produce an invalid choice); the
guideline's §2 table names only widgets that exist.

## Deployment

This is a **client-only** change — `platform/keystone-admin-ui` is a Flutter package published into the
hosting app(s):

1. `flutter pub get` in `apps/inventory/frontend` (no dependency change, so nothing to resolve beyond the
   path package);
2. `flutter build web` + deploy to Firebase Hosting (or an app-store build for iOS/Android).

No Liquibase changeset, no jOOQ regeneration, no migration, no new environment variable, no
server-before-client (or client-before-server) ordering constraint: the picker calls routes that already
exist and already behave as it assumes. A console built from this change against an older backend (or the
other way round) is unaffected, because no wire contract moved.

## Follow-ups (recorded, not blockers)

1. **Access-level filter** (`read-only` / `read-write`) on the permission list routes — makes the picker's one
   always-available filter possible, and would serve the Permissions screen too (open question 1).
2. **Edit role** in the console: `ApiClient.updateRole` + a row-menu action + a prefilled dialog, plugging
   into the picker that already accepts a pre-selection (open question 2).
3. **A global role holding a tenant-owned `TENANT`-scope permission** — accepted by the backend, meaningless
   to the other tenants sharing the role; worth a backend guard or a catalogue rule (open question 4).
4. **Manual click-through** against a live backend (dev credentials required) — recorded honestly if it could
   not be performed, as previous deliveries have done.
5. **`/permissions/options`** stays unbuilt on purpose: a 500-row capped set cannot serve a catalogue that
   grows with every tenant, and §1.17 now says what to build instead.

## Execution record (2026-09-28)

**Shipped**

| Artifact | Change |
| --- | --- |
| `docs/UX_GUIDELINES.md` | **§1.17** ("a picker over a set too large for one control is a paged list") + a §2 mapping row naming `showPermissionPicker`/`PermissionRow`, `ListQuery.permissionsFor` and `PermissionSelection` as the enforcement. |
| `CHANGELOG.md` | An entry under `[Unreleased]` → `### Changed` describing the picker, the edit path, the mirrored rules, and that it is a **console-only** change. |
| `docs/ARCHITECTURE.md` | **Unchanged, as decided in the plan.** §9.5 *Delegated administration* enumerates the guardrails ("grant only what you hold", write implies read, the seeded roles are immutable) but not the console's affordances, and this delivery adds no new rule to it — the picker mirrors what was already documented. |
| `docs/CODING_GUIDELINES_BACKEND.md` / `_FRONTEND.md` | **Unchanged.** No new backend pattern; the frontend rules already cover everything used (§7.3 list screens, §14 typed models). |

**Deployment** — client-only, and no ordering constraint:

```
cd apps/inventory/frontend && flutter pub get && flutter build web   # then: firebase deploy --only hosting
```

No Liquibase changeset, no jOOQ regeneration, no migration, no environment variable, no server-before-client
(or client-before-server) step: the picker calls two routes that already existed with parameters the backend
already accepted, and `PATCH /api/v1/roles/{id}` was already served. A console built from this change against an
older backend — or the reverse — is unaffected, because no wire contract moved.

**Follow-ups (recorded, not blockers)**

1. ~~**Access-level filter** (`read-only` / `read-write`) on the permission list routes.~~
   **Delivered** (2026-09-28) by `docs/delivery/permission-grant-followups/`: `?access=` on both permissions
   list routes + an `AccessFilter` on the Permissions screen and in the picker.
2. ~~**A global role holding a tenant-owned `TENANT`-scope permission** is still accepted by the backend.~~
   **Closed** (2026-09-28) by the same delivery: `RoleService.grantableTo` restricts a role to the catalogue
   plus its own owner's rows, so a global role holds the catalogue only — and the picker's seed follows.
   (This supersedes the `ownerFilter(null)` reasoning in `plan.md`'s owner/scope table and in decision 2.)
3. **Manual click-through** against a live backend (dev credentials required) — **not performed**, as the
   environment has none. The console was verified by `flutter analyze`, 136 widget/unit tests and review.
4. **Repository-wide `dart format`** — this SDK's formatter restyles every file (see the note in
   `05-frontend-ui-flutter.md`), so the repository is not `dart format`-clean under it. Worth a formatting-only
   change of its own, so future deliveries stop having to revert formatter churn.
5. **`/permissions/options` stays unbuilt on purpose**: a 500-row capped set cannot serve a catalogue that grows
   with every tenant, and §1.17 now says what to build instead.

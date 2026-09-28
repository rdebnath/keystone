# Phase 8 — Delivery

## Scope

Land the documentation the guideline depends on, record the one breaking contract change, and state the
deployment order and the follow-ups that were deliberately not delivered.

## Artifacts

| Artifact | Change |
| --- | --- |
| `CHANGELOG.md` | New entry: **breaking** — list endpoints return a page envelope; search/paging parameters; three `/options` routes; the two indexes. Names every affected route so a consumer can migrate. |
| `docs/UX_GUIDELINES.md` | **New**, created in phase 1, finalised here with the rules as implemented (including the `/options` rule and its cap) and a pointer to the widgets that enforce them. |
| `docs/CODING_GUIDELINES_BACKEND.md` | §8 line 414 replaced by the full list contract (parameters, envelope, caps, sort whitelist, searchable-columns rule, stable ordering, `/options`, `422` semantics) as §8's *List endpoints* subsection; §13 gains "the page envelope is a typed record, never a `Map`". |
| `docs/CODING_GUIDELINES_FRONTEND.md` | New §7.3 *List screens* (shared widgets, debounce, page reset, `ListQuery`, no client-side filtering of a server list) and §14's note on `Paged<T>`/`OptionList<T>`; both link `docs/UX_GUIDELINES.md` §1. |
| `docs/ARCHITECTURE.md` | §5.1 REST data flow and §9.2 (*"`GET /api/v1/tenants` returns it first"*) restated for the paged contract; the platform tenant is described as a pinned, searchable first row. |
| `AGENTS.md` | "Read first" gains `docs/UX_GUIDELINES.md` (between the frontend guidelines and the feature-delivery skill), so the next agent reads the rule instead of re-inventing it. |

## Deployment notes

- **Migration**: nothing to run by hand — `0005-paging-indexes.xml` is applied by `AdminMigrationRunner`
  at service start (Liquibase), and it is additive and idempotent. The changelog is also included in the
  app's master changelog path (`scripts/migrate-schema.sh` applies the same set).
- **Order: server before (or with) the console.** The console is served from Firebase Hosting while the
  API runs on Cloud Run, so they deploy separately; a new console against an old server would fail to parse
  a JSON array into `Paged<T>`, and an old console against a new server would fail to parse the object into
  a `List`. Deploy the API first, then the web build (or accept a short window of broken lists).
- **Rollback**: revert the Cloud Run revision and the Firebase release. The new indexes are additive and
  harmless to the previous revision, so no schema rollback is needed. No data was migrated.
- **No downtime requirement**: paging changes queries, not the schema's shape; the two `CREATE INDEX`
  statements are non-concurrent (small tables), so they finish in milliseconds.

## Follow-ups (recorded, not delivered)

1. **`pg_trgm` GIN indexes** on the searched columns once a table exceeds ~10k rows per tenant — the
   trigger and the reason for deferring it are in phase 2 and in the guideline.
2. **Type-ahead pickers** (`SearchAnchor` against `/options?q=`) once the tenant count approaches the
   500-row `MAX_OPTIONS` cap, so a dropdown never becomes an unusable list.
3. **Drop the now-redundant `idx_users_tenant`** (superseded by `idx_users_tenant_username`) in a later,
   reviewed changeset — `0003` is applied and must not be edited.
4. **Adopt the contract in `apps/inventory`** (`GET /api/v1/items` → `Page<ItemDto>`), now a one-line
   change thanks to `keystone-common`.
5. **Keyset (seek) paging** if a console ever needs deep pages without `OFFSET` cost.

## Dependencies

- Phases 1–7 (this phase only records what they did).

## Execution (2026-09-28)

| Artifact | Change |
| --- | --- |
| `CHANGELOG.md` | New `[Unreleased] → Added` entry: what the lists now take and return, the **Breaking** envelope change, the three `/options` routes and their cap, the shared vocabulary, the `0005` indexes, the console's new controls, and the new `docs/UX_GUIDELINES.md`. |
| `docs/UX_GUIDELINES.md` | Created in phase 1 (the rules); **unchanged since**, because the shipped code follows them — the phase 8 cross-check found no rule that was documented but not implemented, and no implemented rule that was missing from §1. |
| `docs/CODING_GUIDELINES_BACKEND.md` | §8's new *List endpoints* subsection (parameters, envelope, caps, whitelist, escaping, ordering, `/options`, PII) and §13's *list envelopes are records too*. |
| `docs/CODING_GUIDELINES_FRONTEND.md` | §7.3 *List screens*, and §14's `Paged<T>`/`OptionList<T>` rule. |
| `docs/ARCHITECTURE.md` | §5.1's fifth data-flow step, and §9.2's description of the synthetic tenant as the pinned first row of the *paged* list. |
| `AGENTS.md` | "Read first" now lists the UX guideline as item 4 (the skill moved to 5). |

## Deployment notes (as planned)

- **Migration**: `0005-paging-indexes.xml` is applied by `AdminMigrationRunner` at service start (also
  included by `scripts/migrate-schema.sh`). Additive and idempotent — proven by the schema test running
  `migrate()` twice.
- **Order: API first, then the web build.** The console is served from Firebase Hosting while the API runs
  on Cloud Run, so they deploy separately; a new console against an old API would fail to parse a JSON array
  into `Paged<T>`, and an old console against a new API would fail to parse the object into a `List`.
- **Rollback**: revert the Cloud Run revision and the Firebase release. The indexes are additive and harmless
  to the previous revision, so no schema rollback and no data migration is needed.

## Follow-ups (recorded, not delivered)

1. **`pg_trgm` GIN index** on the searched columns once a table exceeds ~10k rows per tenant — the leading-
   wildcard `ILIKE` is a scan today, which is accepted because these tables are catalog-sized (phase 2).
2. **Type-ahead pickers** (`SearchAnchor` against `/options?q=`) once a tenant count approaches the
   500-row `MAX_OPTIONS` cap; the truncation is already surfaced instead of hidden.
3. **Drop the redundant `idx_users_tenant`** (superseded by `idx_users_tenant_username`) in a reviewed
   changeset — `0003` is applied and must not be edited.
4. **Adopt the contract in `apps/inventory`** (`GET /api/v1/items` → `Page<ItemDto>`): now a small change
   thanks to the shared vocabulary, and worth doing so the app's own list follows the guideline too.
5. **Keyset (seek) paging** if a console ever needs deep pages without `OFFSET` cost.

## Verification

- `git diff --stat` matches this plan's artifact lists (4 modified docs + 1 new doc + 1 changelog entry,
  the platform libraries, the admin services/handlers, the Flutter package and the tests).
- Cross-check: every rule in `docs/UX_GUIDELINES.md` §1 is implemented by the shipped widgets or contract
  (`§2` maps them), and every shipped behaviour is covered by §1 — the one place the code goes *further*
  than the plan (a third index, `ScopeFilter` in `owner_filter.dart`) is recorded in phases 2 and 5.
- Full verification re-run from a working tree: `mvn test` → **BUILD SUCCESS**; `flutter analyze` +
  `flutter test` in `platform/keystone-admin-ui` → clean + **94 passed**; `flutter analyze` in
  `apps/inventory/frontend` → clean.


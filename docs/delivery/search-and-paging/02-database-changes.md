# Phase 2 — Database changes

## Scope

Paging turns `SELECT …` into `SELECT … ORDER BY … LIMIT n OFFSET m` for every list route, so the
**default order of each paged query must be index-supported**. No table, column or type changes: the
schema delta is two supporting indexes plus an explicit decision *not* to add trigram indexes yet.

## Artifacts

| Artifact | Change |
| --- | --- |
| `platform/keystone-admin/src/main/resources/admin-db/changelog/0005-paging-indexes.xml` | **New** changeset, guarded like `0003` (`preConditions` on `tableExists` + *not* `indexExists`) and idempotent. |
| `…/changelog/db.changelog-master.xml` | `<include file="0005-paging-indexes.xml" …/>` |

| Index | Definition | Serves |
| --- | --- | --- |
| `idx_tenants_name` | `tenants (name)` | `GET /api/v1/tenants` default order `name ASC` (also `/tenants/options`) |
| `idx_users_tenant_username` | `users (tenant_id, username)` | `GET /api/v1/users` and `/api/v1/tenant/users` — the `tenant_id` filter plus the `username` order in one index |

Already covered, so nothing is added for them: `idx_roles_tenant_code` and `idx_permissions_tenant_code`
(`0003-tenant-scoped-rbac.xml`) match the roles/permissions default order `(tenant_id, code)` exactly.

## Decisions

- **`idx_users_tenant` (from `0003`) becomes redundant** — `idx_users_tenant_username` serves every
  `tenant_id`-only lookup through its leading column. It is **kept**: `0003` is an applied changeset and
  the guideline forbids editing an applied changeset, so a future `DROP INDEX` is a separate, reviewed
  change. Recorded as a follow-up, not done here.
- **No `pg_trgm` GIN index for the `ILIKE '%term%'` search — yet.** A B-tree cannot serve a leading-
  wildcard `LIKE`, so search is a scan; the reason it is accepted now is that the four tables are
  catalog-sized (tenants, roles, permissions) or modest (users per tenant), and enabling an extension
  (`CREATE EXTENSION pg_trgm`) adds a deployment dependency on privileges we do not control in a
  Supabase-managed database. The trigger for revisiting is documented in the guideline: when a table
  exceeds ~10k rows per tenant, add a `pg_trgm` GIN index on the searched columns.
- **No new columns for search** — searching existing columns keeps the wire and the schema minimal; a
  materialized `search_text` column would be a second source of truth.
- **jOOQ codegen is unaffected**: indexes produce no generated classes, and codegen runs offline from
  the changelog (`docs/CODING_GUIDELINES_BACKEND.md` §7), so the build graph does not change.

## Dependencies

- Phase 1 (the contract fixes which queries exist and in what order).

## Execution (2026-09-28)

Delivered as **two changesets** in `0005-paging-indexes.xml`, guarded on `tableExists` + *not*
`indexExists` exactly like `0003`/`0004`, plus the master include. Deviating from the plan in one place,
recorded below.

| Changeset | Index | Serves |
| --- | --- | --- |
| `0005-tenants-name-index` | `idx_tenants_name (name)` | the tenants list's default order `name ASC` (and `/tenants/options`) |
| `0005-users-username-index` | `idx_users_username (username)` | the platform plane's unfiltered user list (`ORDER BY username` with no tenant filter) |
| `0005-users-tenant-username-index` | `idx_users_tenant_username (tenant_id, username)` | the tenant-scoped user list — filter and order in one index |

**Deviation from the plan:** a **third** index (`idx_users_username`) was added. The plan listed two, but
the platform plane's default user list has no tenant filter, and a composite `(tenant_id, username)` index
cannot serve an unfiltered `ORDER BY username` — without the plain index that list would sort the whole
table on every page. The plan file's phase table is superseded by this one.

Also recorded in the changeset itself: the roles/permissions default order is `tenant_id ASC NULLS FIRST,
code ASC`, which a plain `(tenant_id, code)` index does **not** serve (PostgreSQL's forward scan yields
NULLS LAST and Liquibase's `createIndex` has no nulls-first option). Both tables are catalog-sized
(tens of rows, seeded at bootstrap), so the sort is negligible and no index was added; a later changeset
can revisit if the catalog grows.

## Verification

- `PagingIndexesSchemaTest` (new, Testcontainers PostgreSQL): asserts all three indexes exist and that
  `idx_users_tenant_username` is defined as `(tenant_id, username)` — a changelog that never ran and one
  that ran are indistinguishable from the source alone. It also runs `AdminMigrationRunner.migrate()`
  **twice**, so `0005` is proven idempotent.
- `AdminIntegrationTest` (fresh container) applies `0001`–`0005` and bootstraps; the jOOQ codegen step in
  the same build still generates from the changelog, confirming `0005` is valid input for codegen too.
- `mvn test` (whole reactor) → **BUILD SUCCESS**.


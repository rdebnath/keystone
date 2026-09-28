# Phase 8 — Delivery

## Scope

Changelog, migration/deployment notes, and the documentation touchpoints a reviewer or operator needs.

## Artifacts

| Artifact | Change |
| --- | --- |
| `CHANGELOG.md` | an `### Added` entry under `[Unreleased]` describing both fields and their rules |
| `docs/delivery/tenant-country-user-phone/*` | this plan, updated with per-phase results |
| migration | **no manual step**: `0004-tenant-country-user-phone.xml` is included from the master changelog, so the existing `MigrationRunner` / `AdminMigrationRunner` applies it at startup (or via `scripts/migrate-schema.sh`) |

## Deployment notes

- The change is **additive and nullable**: a database migrated to `0004` still serves an older
  server, and each field is independent — deploy the server before the console, or the console first;
  neither ordering breaks.
- **No backfill** and no maintenance window: two `ALTER TABLE … ADD COLUMN` statements that do not
  rewrite the tables.
- **No new environment variable, secret or yaml key**, so no Cloud Run / Secret Manager change.
- Rollback is a code rollback plus an optional `ALTER TABLE … DROP COLUMN`; leaving the column in
  place is harmless because the old code never reads it.

## Execution (2026-09-28)

- `CHANGELOG.md` — an `### Added` entry at the top of `[Unreleased]` describing both fields, their
  rules, the routes that carry them and the two console fields.
- Migration: nothing manual. `db.changelog-master.xml` includes `0004`, so the existing
  `AdminMigrationRunner` (and `scripts/migrate-schema.sh`, and the app's `SchemaTool migrate`) applies
  it; the two `ADD COLUMN` statements do not rewrite the tables.
- No documentation outside this directory needed changing: `docs/ARCHITECTURE.md` and the coding
  guidelines describe mechanisms, not the tenant/user column list, and `README.md` covers setup.

## Verification

- `mvn test` (whole reactor) → **BUILD SUCCESS**: `keystone-admin` 88 tests and `inventory-server`
  15 tests, 0 failures, 0 skipped.
- `flutter analyze` + `flutter test` in `platform/keystone-admin-ui` → clean / 66 passed;
  `flutter analyze` in `apps/inventory/frontend` → clean.
- Reviewer checklist (`docs/CODING_GUIDELINES_BACKEND.md` §14): records not maps; persistence rows do
  not leak into handlers; the changelog owns the schema; validation answers `422`; no new secret, no
  new configuration; `.editorconfig` respected (<= 120 columns).


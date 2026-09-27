# Phase 8 — Delivery

**Scope** — document the new model, record the breaking change for anyone who hard-coded an action
code, and state the deploy-time behaviour.

**Artifacts**

| File | Change |
| --- | --- |
| `CHANGELOG.md` | `### Changed` entry: "Permission model is two levels per resource (read/write, read-only)" — `<resource>:read-only` / `<resource>:read-write` replace `create`/`read`/`update`/`delete`/`assign-role`; read/write implies read; no cleanup or migration (fresh development) |
| `docs/ARCHITECTURE.md` | §9.3 — replace the `platform:tenant:create` / `tenant:user:create` / `tenant:role:assign` examples with `<scope>:<resource>:<level>` and state that every resource has exactly two levels; §9.4 schema block unchanged (`permissions` still carries `code` + `scope`) |
| `README.md` | only if it lists the permission codes (it currently does not) — verify, no other change |

**Dependencies** — Phases 3–7 green.

**Verification** — `mvn clean verify` and `flutter analyze`/`flutter test` green; the changelog and
architecture text read correctly against the shipped catalog (`GET /api/v1/permissions` on a dev
database shows the 14 codes + `*`).

## Deploy notes

- **Nothing to migrate.** Fresh development on an unreleased platform: the catalog seeds the two
  levels per resource, no older codes exist, and no cleanup or backfill runs at startup. A dev/demo
  database that predates the two levels is reset with `SchemaTool reset`.
- **Backend and frontend ship together** (one release): the catalog plus the level checks are the
  contract, and `platform/keystone-admin-ui` is the only in-repo consumer — the host
  (`apps/inventory/frontend`) needs no change of its own.
- **Local/dev:** `SchemaTool reset` still gives a clean database; no Liquibase changeset was added,
  so there is no new migration to apply and no codegen churn.
- **No new configuration** — no env vars, no yaml keys, no deployment topology change.

## Execution record (2026-09-27)

- `CHANGELOG.md` (Unreleased → `### Changed`) and `docs/ARCHITECTURE.md` §9.3 updated; §9.4's schema
  block is unchanged (the `permissions` table still carries `code` + `scope`).
- `README.md` verified: it does not list permission codes, so it needs no change.
- Nothing to apply by hand: no Liquibase changeset, no new config/env var, no jOOQ codegen churn
  (`mvn -q -DskipTests compile` clean, generated sources byte-identical).
- Final verification: backend `mvn -pl platform/keystone-admin test` → 45 tests, 0 failures, 1 skipped
  (Testcontainers/Docker); frontend `flutter analyze` clean + `flutter test` 10/10.
- Follow-up (2026-09-27): the bootstrap prune of the old action codes was removed at the user's
  request — fresh development means there is nothing to clean up. `PermissionCatalog` no longer has a
  legacy list, `BootstrapRunner` has no prune method, and the changelog/architecture text says nothing
  about migration.

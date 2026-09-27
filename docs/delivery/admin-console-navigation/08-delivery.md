# Phase 8 — Delivery

**Scope** — document the new console behaviour and the API deltas, and state what (does not) need to
happen at deploy time.

**Artifacts**

| File | Change |
| --- | --- |
| `CHANGELOG.md` | `### Added` — the permission-driven collapsible navigation shell, the tenant→users drill-down, tenant add/edit/delete and user add/edit/delete in the console, the `Keystone` platform tenant in the tenant list, `GET /api/v1/users?tenantId=`, `PATCH /api/v1/users/{id}`, `MeDto.username`; `### Changed` — the tab shell (`DashboardScreen`) is replaced by `AdminShell` + router sections |
| `docs/ARCHITECTURE.md` | §9.1/§9.2 — the platform plane is surfaced in the tenant list as the synthetic `Keystone` tenant (reserved id, never persisted, `users.tenant_id IS NULL`); §9.6 — the client renders menu sections from the effective read permissions it receives from `/me` |
| `README.md` | verify only: it documents build/run/schema tooling and lists no routes, so no change is expected |
| `docs/delivery/admin-console-navigation/*.md` | execution records appended per phase (what was built, verification results, deviations, follow-ups) |

**Deploy notes**

- **Nothing to migrate.** No Liquibase changeset, no data backfill, no jOOQ codegen churn (the
  generated sources stay byte-identical — `mvn -q -DskipTests compile` proves it). The `Keystone`
  platform row is computed, never stored.
- **Backend and frontend ship together** (one release): the `platform` field on the tenant list, the
  `tenantId` filter, the new `PATCH /users/{id}` and `Me.username` are consumed by
  `platform/keystone-admin-ui`, which only the in-repo host (`apps/inventory/frontend`) mounts. The
  host router changes land in the same release.
- **Existing data is unaffected:** `GET /users` without a filter behaves exactly as before, an
  existing `POST /users` with `tenantId: null` still creates a platform user, and the tenant list
  simply gains a first row.
- **No new configuration** — no env var, no yaml key, no secret, no CORS or deployment change.
- **Local/dev check:** sign in as `admin@keystone` (bootstrap credentials), toggle the pane, open
  `Keystone`, add a tenant, drill into it, add a user with a `TENANT` role, edit that user's roles,
  then delete the user and the tenant.

**Dependencies** — Phases 3–7 green.
## Execution record (2026-09-27)

- `CHANGELOG.md` — `### Added` (the navigable console, in both backend and frontend bullet groups),
  `### Changed` (the tab shell → shell + routes, the `platform` flag on the tenant list),
  `### Deployment` (nothing to migrate; `flutter pub get` for the new `go_router` dependency).
- `docs/ARCHITECTURE.md` — §9.2 gains the synthetic `Keystone` platform tenant (reserved id/slug, never
  persisted, reserved-id/slug refusals); §9.6 records that the console renders its navigation from the
  effective read permissions and that write affordances need `:read-write`.
- `README.md` — verified: it documents layout, build, run, schema tooling, the container image and the
  JDK pin, and lists no routes or screens, so it needs no change (its module table still describes
  `keystone-admin-ui` as the admin console UI).
- **Deployment reality:** the change is code-only — no Liquibase changeset, no jOOQ codegen churn
  (`mvn -q -DskipTests compile` at the root is clean), no new env var, yaml key or secret, and no
  deployment-topology change. `platform/keystone-admin-ui` requires `flutter pub get` because of the
  added `go_router` dependency.
- **Manual check still to do** (needs a running server + credentials): sign in as `admin@keystone`,
  toggle the pane, open `Keystone` and a tenant, add/edit/delete a tenant, add a user to the tenant,
  edit that user's roles, then delete the user and the tenant.
- Final verification after the docs pass (Docker running): `mvn clean verify` → **BUILD SUCCESS** with
  **0 tests skipped** anywhere (keystone-admin 51, inventory-server 10, the platform modules' suites),
  so `AdminIntegrationTest` ran against a real Postgres container; `flutter analyze` clean and
  `flutter test` **36/36** in `platform/keystone-admin-ui`; `flutter analyze` clean in
  `apps/inventory/frontend`.
- That run corrected the expected status of a rejected value from `400` to **`422`**
  (`ProblemDetailMapper`: `VALIDATION -> 422`) in the integration test, this delivery's documents,
  `CHANGELOG.md` and `docs/ARCHITECTURE.md`.


**Verification** — `mvn clean verify` (Docker present, so `AdminIntegrationTest` runs) and
`flutter analyze` + `flutter test` clean; the manual check above performed once against the dev
environment; CHANGELOG and ARCHITECTURE text re-read against the shipped API.

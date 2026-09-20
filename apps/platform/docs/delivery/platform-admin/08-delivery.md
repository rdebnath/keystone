# Phase 8 — Delivery

**Scope** — docs + changelog + deployment notes.
**Artifacts** — update `README.md` layout table; this directory; note the §9.1 refinement.
**Dependencies** — Phases 1–7.
**Verification** — full `mvn clean verify` passes; docs consistent with code.

## Deployment notes

- New app `apps/platform` → new GCP project (Cloud Run + Supabase project) per §7.3.
- Required env: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `SUPABASE_URL`,
  `SUPABASE_SERVICE_ROLE_KEY`, `OIDC_ISSUER`, `OIDC_AUDIENCE`, `OIDC_JWKS_URL`,
  `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`.
- Bootstrap runs once, idempotently, at startup (service-role key).

## Result

- `README.md` layout table updated with `apps/platform`, `apps/platform/server`,
  `apps/platform/frontend`.
- `compose.yaml` gained a `postgres-platform` service (port 5433) for local dev.
- `.gitignore` updated for Flutter/Dart build output (`.dart_tool/`, `build/`, …).
- Root `CHANGELOG.md` added (Keep a Changelog / SemVer) with the platform-admin console entry
  under `[Unreleased]`.

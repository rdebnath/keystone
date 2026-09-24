# Phase 8 — Delivery

**Scope** — docs, changelog, deployment notes.

**Artifacts**
- `CHANGELOG.md` entry.
- `README.md` layout table: replace the `apps/platform` rows with `platform/keystone-admin`
  (library) + a shared UI package; note that apps host the platform library.
- `docs/ARCHITECTURE.md`: update to reflect the embedded platform + backend-proxied login.
- Deployment note: an inventory deploy now migrates + bootstraps the `platform` schema and
  creates the first platform user; there is no separate platform service/deploy.

**Dependencies** — Phases 1–7.

**Verification** — `mvn clean verify` passes; docs are consistent with the code.

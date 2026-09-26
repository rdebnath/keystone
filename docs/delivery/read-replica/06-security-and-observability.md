# Phase 6 — Security & observability

**Scope** — Light touch. Read/write splitting is internal routing; no auth/RBAC change.

**Artifacts**
- `platform/keystone-data/src/main/java/com/chetana/keystone/data/JooqDatabase.java` (modify) — DEBUG log the resolved read target on `read()` (replica vs primary) for diagnosability; never log credentials.
- No secrets in config files — `database.read.password` stays environment-supplied only (same rule as the primary password; see `docs/CODING_GUIDELINES_BACKEND.md` §5).

**Dependencies** — Phase 3.

**Verification** — Code review: no credentials logged; replica password not written to any committed YAML; structured SLF4J logging only.

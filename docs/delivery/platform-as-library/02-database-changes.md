# Phase 2 — Database changes

**Scope** — extend the `platform` schema to support `username@tenantid` login.

**Artifacts**
- New Liquibase changelog in the library (relocated from `apps/platform/server`):
  `0002-tenant-slug.xml` — add `tenants.slug VARCHAR(255) UNIQUE NOT NULL`; add
  `users.username`; unique `(username, tenant_id)`; backfill slug from existing rows (if any).
- Regenerate jOOQ types for the `platform` schema from the changelog (offline codegen).

**Dependencies** — Phase 1.

**Verification** — `mvn -pl platform/keystone-admin -am generate-sources` succeeds; generated
jOOQ exposes `TENANTS.SLUG` and `USERS.USERNAME`; the changelog applies idempotently on a
Testcontainers PostgreSQL.

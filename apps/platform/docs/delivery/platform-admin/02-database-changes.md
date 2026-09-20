# Phase 2 — Database changes

**Scope** — Liquibase changelog for identity/tenancy/RBAC; jOOQ codegen.
**Artifacts** — `apps/platform/server/src/main/resources/db/changelog/db.changelog-master.xml`,
`0001-initial-schema.xml`, `0002-seed-permissions-and-roles.xml`; generated jOOQ sources under
`com.chetana.keystone.platform.jooq`.
**Dependencies** — Phase 1 (schema).
**Verification** — `mvn -pl apps/platform/server -am generate-sources` produces tables; migration
applies in the integration test.

## Tables

- `tenants (id uuid PK, name text UNIQUE NOT NULL, created_at/updated_at timestamptz)`
- `users (id uuid PK, sub text UNIQUE NOT NULL, tenant_id uuid NULL FK tenants,
  must_change_password boolean NOT NULL DEFAULT false, created_at/updated_at timestamptz)`
- `roles (id uuid PK, code text UNIQUE NOT NULL, scope text CHECK IN ('PLATFORM','TENANT'),
  created_at/updated_at)`
- `permissions (id uuid PK, code text UNIQUE NOT NULL, scope text CHECK, created_at/updated_at)`
- `role_permissions (role_id FK, permission_id FK, PK(role_id, permission_id))`
- `user_roles (user_id FK, role_id FK, tenant_id uuid NULL, PK(user_id, role_id, tenant_id))`

## Seed (idempotent, in `0002`)

- Permission catalog + wildcard `*`.
- `platform-admin` role (scope PLATFORM) granted `*`.

## Result

- Schema implemented in `0001-initial-schema.xml`; `users` gained an `email` column for display.
- `user_roles` PK is `(user_id, role_id)` — `tenant_id` is nullable, so it cannot be part of the
  primary key (the §9.4 sketch had this bug; multi-tenant role assignment stays a future extension).
- Seed data (permission catalog + `platform-admin` role) moved out of Liquibase into the idempotent
  runtime `BootstrapRunner` — PostgreSQL `ON CONFLICT` syntax broke the offline jOOQ DDL codegen.
  The changelog is DDL-only, like the inventory app's.

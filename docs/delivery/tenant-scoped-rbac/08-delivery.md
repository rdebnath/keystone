# Phase 8 — Delivery

> **Executed 2026-09-27 (partially).** `CHANGELOG.md` has the Added + Changed entries, and
> `docs/ARCHITECTURE.md` is updated: §9.3 (ownership, per-owner codes, the two seeded admin roles and the
> read/write-only grants), §9.4 (owner columns, per-owner uniqueness with the partial index, the
> `TENANT`-scope `CHECK`, the index list, and the two pre-existing doc/schema mismatches corrected — the
> `user_roles` PK is `(user_id, role_id)` and there is no `user_roles` scope `CHECK`), §9.5 (tenant admin =
> holder of the tenant's `admin` role, the guardrails) and §9.6 (the two guard families, the derived-never-
> supplied tenant, `PermissionGuard.callerScope`). `README.md` needs no change: it describes layout and
> hosting, not the RBAC model.
>
> **Remaining:** the deployment note's "reset the DB" advice stands, but the phase-5 UI is outstanding, so
> the manual walk-throughs in `05`/`08` have not been run.

**Scope** — docs, changelog, and the migration/deployment note.

**Artifacts**

- `CHANGELOG.md` — Unreleased → Added: optional `tenant_id` owner on `roles` and `permissions`; the
  seeded per-tenant `admin` role (read/write `TENANT`-scope grants only); the tenant self-service plane
  (`/api/v1/tenant/users`, `/roles`, `/permissions`); the `GET /api/v1/roles|permissions?tenantId=`
  filter; `tenantId` on the role/permission DTOs and requests. Note the behaviour changes: `code` is
  now unique **per owner** (not globally), and the seeded admin roles (`platform-admin`, `admin`) can
  no longer be renamed or deleted.
- `docs/ARCHITECTURE.md`:
  - §9.3 — add ownership next to scope: a role/permission is *global* (`tenant_id IS NULL`, the
    platform-defined catalog, applicable to the platform plane **and every tenant**, still bounded by
    its `scope`) or *owned by a tenant*; a tenant-owned row is `TENANT` scope.
  - §9.4 — schema block: `roles (... tenant_id uuid NULL REFERENCES tenants)` and the same for
    `permissions`; per-owner uniqueness and the indexes (§2 of phase 2). **Correct the two
    pre-existing doc/schema mismatches**: the `user_roles` primary key is `(user_id, role_id)` (the doc
    claims a third `tenant_id` column in the key) and the claimed `user_roles` scope `CHECK` does not
    exist in the changelog — state the schema as it is, or raise the `CHECK` separately rather than
    smuggling it in here.
  - §9.5 — replace "tenant-defined custom roles are a later option" with the shipped rule: each tenant
    gets a seeded `admin` role granted the **read/write** `TENANT`-scope codes, and a holder of it is a
    **tenant admin** who creates that tenant's users, roles and permissions (grant only what you hold).
    Note the platform admin still creates the tenant and its first tenant admin.
  - §9.6 — add the **tenant self-service** plane: `/api/v1/tenant/roles` and `/api/v1/tenant/permissions`,
    the `tenant:role:*` / `tenant:permission:*` guards, that the tenant comes from the caller (never a
    parameter), and the "grant only what you hold" escalation guardrail.
- `README.md` — verify whether the platform-admin description needs the note; likely no change.
- **Deployment note** — the new `0003` changeset runs with the normal startup migration
  (`MIGRATE_ON_START`) or out of band (`SchemaTool migrate` / `scripts/migrate-schema.sh`). Because
  the platform is unreleased there is no backfill: a dev/demo database can be reset with
  `SchemaTool reset`. No new secret, environment variable or yaml key.

**Dependencies** — phases 2–7.

**Verification** — `mvn clean verify` (or the module-scoped equivalent) passes; `CHANGELOG.md` and
`docs/ARCHITECTURE.md` match the shipped contracts; `scripts/` need no edit (no new variable).

## Rollback

Reverting the code without reverting `0003` leaves two nullable columns and the per-owner constraints
in place — harmless to the old code, which never reads `tenant_id` but would then hit the per-owner
`code` uniqueness on a duplicate global insert. Because the platform is unreleased, the supported path
is `SchemaTool reset` rather than a down-migration; state this in the changelog note if it matters to
the operator.

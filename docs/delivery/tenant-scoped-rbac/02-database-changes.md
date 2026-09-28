# Phase 2 — Database changes (Liquibase)

> **Executed 2026-09-27.** Done as specified, with one deviation: the global `UNIQUE (code)` is dropped
> with `DROP CONSTRAINT IF EXISTS roles_code_key` / `permissions_code_key` — the plain `DROP CONSTRAINT`
> works on PostgreSQL but makes the H2-backed offline `DDLDatabase` fail hard, because it names the
> unnamed constraint differently. `IF EXISTS` drops it on PostgreSQL and is a harmless no-op in the
> renderer. The partial unique indexes are accepted by the renderer, so no fallback was needed. Verified
> by `TenantScopedRbacSchemaTest` (5 tests) against real PostgreSQL: the global pool stays unique, the
> same code is allowed in two tenants and rejected twice in one, a tenant-owned `PLATFORM`-scope row is
> rejected, and every index exists.

**Scope** — add optional ownership to `roles` and `permissions`, and the constraints that keep it
sound. DDL only; the seeded catalog is written by the bootstrap (phase 3), not by a changelog.

**Artifacts**

- `platform/keystone-admin/src/main/resources/admin-db/changelog/0003-tenant-scoped-rbac.xml` (new)
- `platform/keystone-admin/src/main/resources/admin-db/changelog/db.changelog-master.xml` (`<include>`
  of the new file)

**Dependencies** — phase 1 (design).

**Verification** — `mvn -pl platform/keystone-admin generate-sources` renders the changelog to DDL and
regenerates jOOQ types without error; the generated `Roles`/`Permissions` records carry `TENANT_ID`.
`AdminMigrationRunner` (Testcontainers) applies it; `PlatformSchemaTest`/`AdminIntegrationTest`
(Docker) pass.

## Changeset outline

Two changesets in `0003-tenant-scoped-rbac.xml` (or two files `0003-…` / `0004-…` if you prefer one
concern per file): the **columns + constraints**, then the **indexes**. Both `author="keystone"`,
guarded by a `<preConditions onFail="MARK_RAN">` on `NOT columnExists` / `NOT indexExists` for
`roles.tenant_id` — the same idempotency pattern as `0002`.

### 1. Columns, constraints, uniqueness

1. `addColumn tableName="roles"` → `tenant_id UUID NULL`, with
   `foreignKeyName="fk_roles_tenant" references="tenants(id)"`.
2. `addColumn tableName="permissions"` → `tenant_id UUID NULL`, with
   `foreignKeyName="fk_permissions_tenant" references="tenants(id)"`.
3. **Drop the global uniqueness, add per-owner uniqueness** (PostgreSQL-specific, so raw `<sql>`
   schema-qualified with `${database.defaultSchemaName}`, as `0001` already does for its `CHECK`s):
   ```sql
   ALTER TABLE ${database.defaultSchemaName}.roles DROP CONSTRAINT roles_code_key;
   ALTER TABLE ${database.defaultSchemaName}.roles
       ADD CONSTRAINT uq_roles_code_tenant UNIQUE (code, tenant_id);
   CREATE UNIQUE INDEX uq_roles_code_global
       ON ${database.defaultSchemaName}.roles (code) WHERE tenant_id IS NULL;

   ALTER TABLE ${database.defaultSchemaName}.permissions DROP CONSTRAINT permissions_code_key;
   ALTER TABLE ${database.defaultSchemaName}.permissions
       ADD CONSTRAINT uq_permissions_code_tenant UNIQUE (code, tenant_id);
   CREATE UNIQUE INDEX uq_permissions_code_global
       ON ${database.defaultSchemaName}.permissions (code) WHERE tenant_id IS NULL;
   ```
4. Scope guard:
   ```sql
   ALTER TABLE ${database.defaultSchemaName}.roles
       ADD CONSTRAINT chk_roles_tenant_scope CHECK (tenant_id IS NULL OR scope = 'TENANT');
   ALTER TABLE ${database.defaultSchemaName}.permissions
       ADD CONSTRAINT chk_permissions_tenant_scope CHECK (tenant_id IS NULL OR scope = 'TENANT');
   ```
5. `role_permissions` needs no change — ownership is a property of the role and of the permission,
   and the `(role, permission)` pairing is validated in the service.

### 2. Indexes

Every index below is justified by a **query the code runs** — no speculative indexes. Names follow
`idx_<table>_<columns>`; uniqueness stays on constraints.

| Table | Index | Serves |
| --- | --- | --- |
| `roles` | `UNIQUE (code, tenant_id)` (`uq_roles_code_tenant`) | lookup a tenant's role by code (`UserService.replaceRoles`, `RoleService.grantPermissions`, tenant-plane `PATCH`/`DELETE`) |
| `roles` | `(code) WHERE tenant_id IS NULL` (`uq_roles_code_global`) | bootstrap / global lookup by code; keeps global codes globally unique |
| `roles` | `idx_roles_tenant_code (tenant_id, code)` | `RoleService.list` (`WHERE tenant_id IS NULL OR tenant_id = ? ORDER BY code`) and the tenant plane's list |
| `permissions` | `UNIQUE (code, tenant_id)` (`uq_permissions_code_tenant`) | lookup a tenant's permission by code when granting |
| `permissions` | `(code) WHERE tenant_id IS NULL` (`uq_permissions_code_global`) | bootstrap; global lookup by code |
| `permissions` | `idx_permissions_tenant_code (tenant_id, code)` | `PermissionService.list` and the tenant plane's list |
| `role_permissions` | **existing PK** `(role_id, permission_id)` | "permissions of a role" (`RoleService.list`, `grantPermissions`) |
| `role_permissions` | `idx_role_permissions_permission (permission_id)` | "roles holding a permission" (`PermissionService.delete`, the `permissions` join in `PermissionResolver`) |
| `user_roles` | **existing PK** `(user_id, role_id)` | "roles of a user" (`UserService.replaceRoles`/`delete`, `PermissionResolver`) |
| `user_roles` | `idx_user_roles_tenant (tenant_id)` | `TenantService.delete` (`WHERE tenant_id = ?`) |
| `users` | **existing** `uq_users_sub`, `uq_users_username_tenant (username, tenant_id)` | login (`LoginService.resolveEmail`), `MeService`, bootstrap adoption, username uniqueness |
| `users` | `idx_users_tenant (tenant_id)` | `UserService.list(tenantId)` (users by plane) |
| `tenants` | **existing** `uq_tenants_slug` | `TenantResolver.resolveBySlug` at login |

Notes:

- The `WHERE tenant_id IS NULL OR tenant_id = ?` list filter is a `BitmapOr` over the partial index
  and `idx_*_tenant_code`; the `ORDER BY code` is then a sort. These are catalog-sized tables, so that
  is fine — revisit with `(tenant_id NULLS FIRST, code)` only if a plan shows the sort dominating.
- Verify with `EXPLAIN (ANALYZE)` on the container-backed test database that each index is actually
  chosen; **drop any index a plan never uses** (the guidelines forbid speculative work).
- `permissions.code` today has an inline `unique="true"` in `0001`, which is what made the catalog
  global; `0002`'s `users`/`tenants` unique constraints are already the right shape.

## Notes

- **Constraint names matter.** `roles_code_key` / `permissions_code_key` are the auto-generated names
  for the inline `unique="true"` constraints in `0001`. Confirm the rendered DDL
  (`target/generated-ddl/schema.sql`) before writing the `DROP CONSTRAINT` statements, and prefer the
  Liquibase `<dropUniqueConstraint>` tag where the name can be declared explicitly.
- The offline `DDLDatabase` renderer used by jOOQ codegen must accept the partial indexes; if it does
  not, drop them from the codegen rendering (they are optional to jOOQ — it only needs the columns
  and the `NOT NULL`/FK facts) and keep them as a hand-written `<sql>` changeset applied only against
  a real database. Verify in this phase.
- **No back-compat obligation**: the platform is unreleased. Existing dev/demo databases may be reset
  with `SchemaTool reset` instead of being backfilled.
- `user_roles` is deliberately untouched (see `plan.md` open question 7).

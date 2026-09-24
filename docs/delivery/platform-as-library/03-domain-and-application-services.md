# Phase 3 — Domain & application services

**Scope** — move identity/tenancy/RBAC + auth services into the library; add the login
use-case.

**Artifacts**
- Relocate `apps/platform/server` Java sources (identity, tenant, user, role, permission,
  auth, `MeService`/`MeHandler`, `BootstrapRunner`, `MigrationRunner`, `PermissionCatalog`,
  `PlatformModule`, config, supabase) into `platform/keystone-admin`.
- New `LoginService` + `TenantResolver`: parse `username@tenantid`, resolve the tenant by
  slug (or reserved `platform`), find the user, authenticate via Supabase (GoTrue password
  grant).
- Value records `LoginRequest` / `LoginResult`.

**Dependencies** — Phase 2.

**Verification** — unit tests for parsing/resolution and the login service (with a fake
Supabase client).

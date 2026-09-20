# Changelog

All notable changes to Keystone are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Platform admin console** — the first concrete platform feature, delivered as a new
  application `apps/platform` (see `apps/platform/docs/delivery/platform-admin/` for the plan and
  per-phase details).

  - **Backend** (`apps/platform/server`) — identity, tenancy, and RBAC administration:
    - Schema: `tenants`, `users` (`sub`, `email`, `tenant_id`, `must_change_password`), `roles`,
      `permissions`, `role_permissions`, `user_roles` (Liquibase changelog, jOOQ codegen).
    - **OIDC / Supabase Auth** resource-server auth (reuses `keystone-security`; the backend never
      stores or checks a password).
    - **Idempotent bootstrap** (`BootstrapRunner` + `SupabaseHttpAdminClient`) using the Supabase
      service-role key: provisions the `admin` identity in Supabase Auth, seeds the permission
      catalog and the `platform-admin` role (granted the wildcard `*` permission = all permissions),
      and assigns that role to the admin.
    - **Forced first-login password change** via `users.must_change_password` + `GET /me` +
      `POST /me/password-changed` (the client updates the Supabase Auth password, then clears the flag).
    - REST API: `GET/POST /api/v1/me`, `POST /api/v1/me/password-changed`, and permission-guarded
      CRUD for tenants, roles, permissions, and users.
    - `PermissionGuard` / `AuthFilter` enforce permission checks (`*` wildcard = all) at the boundary.

  - **Frontend** (`apps/platform/frontend`) — Flutter admin console (web, iOS, Android):
    - Login → forced change-password gate → dashboard with Tenants / Roles / Permissions / Users tabs.
    - Riverpod (no codegen), `dio` REST client, `go_router` auth gate, `supabase_flutter` for
      Supabase Auth (PKCE) + `updateUser` password change.

### Changed

- `user_roles` primary key is `(user_id, role_id)` rather than the §9.4 sketch
  `(user_id, role_id, tenant_id)` — `tenant_id` is nullable, so it cannot be part of the primary
  key. Multi-tenant role assignment remains a future extension.
- Seed data (permission catalog + `platform-admin` role) lives in the idempotent runtime bootstrap
  rather than Liquibase, so the changelog stays DDL-only for offline jOOQ codegen.

### Security

- Supabase service-role key and JWKS/issuer settings are server-only (env / Secret Manager); the
  client bundle ships only the public anon key and backend URL.

### Deployment

- New app requires its own Supabase project + Cloud Run service; required environment variables are
  listed in `apps/platform/docs/delivery/platform-admin/08-delivery.md`.

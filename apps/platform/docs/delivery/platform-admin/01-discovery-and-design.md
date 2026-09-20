# Phase 1 — Discovery & design

**Scope** — Lock contracts, schema, and UI flows before building.
**Artifacts** — API contract, DB schema, permission catalog, UI flow (this file).
**Dependencies** — none.
**Verification** — requester confirmed auth model + bootstrap approach.

## Auth

- Client authenticates with Supabase Auth (PKCE). Backend validates the resulting JWT.
- `Principal` = validated JWT `sub`.
- Bearer token on `Authorization` header; failures → `403` (RFC 9457).

## REST contract (all JSON; base `/api/v1`)

- `GET /healthz` → `200 "ok"` (public).
- `GET /me` → `MeDto { sub, tenantId, mustChangePassword, permissions[] }`.
- `POST /me/password-changed` → clears `must_change_password` after the client updates the
  Supabase Auth password. → `204`.
- Tenants: `GET /api/v1/tenants`, `POST /api/v1/tenants {name}` → `201`, `PATCH /api/v1/tenants/{id}`,
  `DELETE /api/v1/tenants/{id}`.
- Roles: `GET /api/v1/roles`, `POST /api/v1/roles {code, scope, permissions[]}`, `PATCH`, `DELETE`.
- Permissions: `GET /api/v1/permissions`, `POST /api/v1/permissions {code, scope}`, `DELETE`.
- Users: `GET /api/v1/users`, `POST /api/v1/users {email, temporaryPassword, roles[]}`,
  `PUT /api/v1/users/{id}/roles {roles[]}`.

## Permission catalog (seed)

- Wildcard `*` (PLATFORM) — granted only to `platform-admin`.
- Platform: `platform:tenant:*`, `platform:role:*`, `platform:permission:*`,
  `platform:user:*` (create/read/update/delete variants) + `platform:user:assign-role`.
- Tenant: `tenant:role:*`, `tenant:permission:*`, `tenant:user:*` + `tenant:user:assign-role`.

## DB schema (matches §9.4, extended)

`tenants`, `users (sub UNIQUE, tenant_id NULL, must_change_password)`, `roles (code, scope)`,
`permissions (code, scope)`, `role_permissions (role_id, permission_id)`,
`user_roles (user_id, role_id, tenant_id NULL)`. Scope is `CHECK (scope IN ('PLATFORM','TENANT'))`.

## UI flow

Login → (if `mustChangePassword`) Set-new-password → Dashboard
(tenants / roles / permissions / users tabs).

## Decisions logged

- "admin" ⇒ bootstrap email `admin@keystone.local`, initial password `changeit`
  (configurable via `security.bootstrapAdminEmail` / `security.bootstrapAdminPassword`).

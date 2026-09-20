# Delivery plan — Platform Admin Console

**Feature slug:** `platform-admin`
**Type:** App-specific (new application `apps/platform`)
**Status:** Confirmed (auth model + bootstrap approach confirmed with the requester)

## Sizing

New capability spanning database, domain, API, frontend, security, and testing →
**produce a plan**.

## Confirmed decisions

1. **Auth model** — OIDC / Supabase Auth (resource server validates JWTs). The backend
   never stores or checks a password (§9.1). No local `PasswordHasher`/login endpoint.
2. **First-user bootstrap** — automated, idempotent, service-role-key based:
   create the `admin` identity in Supabase Auth (Admin API) + seed the app DB
   (`users`, `platform-admin` role, permission catalog, wildcard "all" grant, role assignment).
3. **"all permission"** — a wildcard permission `code = "*"` (scope `PLATFORM`) granted to
   `platform-admin`; the permission resolver treats `*` as "everything".
4. **Forced password change** — `users.must_change_password` flag; `GET /me` exposes it; the
   Flutter client sets the new password via Supabase Auth (`updateUser`) and then clears the
   flag. Backend stays password-free.
5. **Location** — new app `apps/platform` (`server/` + `frontend/`), reusing the platform
   libraries. Package `com.chetana.keystone.platform`.

## Phases

1. Discovery & design — contracts, schema, UI flows (open questions recorded).
2. Database (Liquibase) — identity/tenancy/RBAC schema + seed; jOOQ codegen.
3. Domain & application services — ports + services for tenants/roles/permissions/users,
   permission resolution.
4. Server-side API — Javalin handlers, auth filter, permission guard. Realtime: N/A.
5. Frontend (Flutter) — login → change-password gate → admin dashboard.
6. Security & observability — bearer-JWT auth, `@RequirePermission`-style guard, logging.
7. Testing — unit + slice + integration (Testcontainers).
8. Delivery — docs, changelog, deployment note.

## Open questions (resolved during discovery)

- "admin" maps to a **bootstrap email** (default `admin@keystone.local`), initial password
  `changeit` — Supabase Auth identities are email-based.
- Roles/permissions are a **global catalog** with `scope` (PLATFORM|TENANT) per §9.4;
  tenant-scoped roles are assigned to users with a `tenant_id` on `user_roles`.
- Realtime broadcast is out of scope for the admin console first pass.

## Execution summary

- Backend (`apps/platform/server`) implemented and `mvn -pl apps/platform/server -am test` passes
  (5 tests). Frontend (`apps/platform/frontend`) implemented; `dart analyze lib` → no issues.
- Auth is OIDC / Supabase Auth (resource server validates JWTs, never stores/checks passwords);
  the first `admin` identity is bootstrapped idempotently with the service-role key and assigned
  `platform-admin` (wildcard `*` = all permissions). Forced first-login password change is driven
  by `users.must_change_password` + `GET /me` + Supabase `updateUser`.

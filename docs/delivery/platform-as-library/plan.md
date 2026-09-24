# Delivery Plan — Platform as an Embedded Library

**Feature slug:** `platform-as-library`
**Level:** Platform-level — touches `platform/keystone-*` libraries, shared architecture, and `docs/`.

## Sizing decision

**Produce a plan.** This is a large, cross-cutting architectural change that:

- introduces a new capability (embedded platform admin + backend-proxied login),
- touches multiple layers (DB, domain, API, auth, Flutter UI),
- changes the database schema (tenant slug, username) and the API/auth contract,
- affects security (the authentication flow itself), and
- requires migration + deployment notes.

## Summary

Today `apps/platform` is a standalone deployable app. The target is for **platform to be a
library** (Java backend + Flutter UI) that is **hosted together with each app** (currently
inventory only):

- The platform admin **backend** moves from `apps/platform/server` into a new
  `platform/keystone-admin` library; `apps/inventory/server` depends on it, serves the
  platform admin API, and runs the `platform` schema migration + first-user bootstrap at
  startup.
- The platform admin **UI** moves from `apps/platform/frontend` into a shared Flutter
  package; `apps/inventory/frontend` becomes the host "shell" that shows the common login
  and routes to the platform admin UI or the inventory UI based on the logged-in user.
- **Login is backend-proxied** (user decision): the client posts `username@tenantid` +
  password to the platform backend, which resolves tenant + user and authenticates against
  Supabase, returning a session.
- The **first platform user** is bootstrapped on inventory deploy into the `platform` schema.

## Phase list

1. Discovery & design
2. Database changes
3. Domain & application services
4. Server-side API & realtime
5. Frontend / UI (Flutter)
6. Security & observability
7. Testing
8. Delivery

## Resolved decisions

1. **Tenant identifier** — add unique `tenants.slug`; the platform admin logs in with the
   reserved slug `keystone` (e.g. `admin@keystone`), resolving to `users.tenant_id = NULL`.
2. **`apps/platform` fate** — delete `apps/platform` entirely (superseded by the library).
3. **Frontend auth** — backend-proxied login (password grant through the backend). Frontend
   uses `dio` + `flutter_secure_storage` + a bearer `dio` interceptor + silent refresh;
   `flutter_appauth` is dropped and `docs/CODING_GUIDELINES_FRONTEND.md` §1 is updated.
   Backend stays an OIDC resource server (validates Supabase JWTs) — no change there.
4. **Backend module name** — new module `platform/keystone-admin`; package
   `com.chetana.keystone.platform.admin`.
5. **Internal Supabase email** — a virtual/fake email, defaulting to
   `username@tenantid.com` (or a custom fake email entered at user creation), stored in
   `users.email` + Supabase Auth. **No email is sent**: users are created via the service-role
   Admin API with `email_confirm: true`, and no email-based flows (signup confirmation,
   password-reset email, magic link, invite) are used; the admin sets temporary passwords.

## Realtime (already aligned)

Supabase Realtime **Broadcast** (ephemeral, in-memory) — not Postgres Changes/table inserts.
String-based event routing, e.g. `/inventory/message_type/<username>` (channel
`inventory:<username>`, event `message_type`). Matches `keystone-realtime`
(`publish(channel, event, payload)`) and backend guidelines §8. No change required.

## Confirmation state

- [x] Plan reviewed — all open questions resolved.
- [x] Executed — Phases 2–8 delivered.

## Execution summary

- **Phase 2** — `tenants.slug` + `users.username` (DDL-only changelog `0002-tenant-slug-username.xml`).
- **Phase 3** — admin backend relocated to `platform/keystone-admin` (package
  `com.chetana.keystone.platform.admin`); `LoginService` / `TenantResolver` added.
- **Phase 4** — `POST /api/v1/auth/login` + `POST /api/v1/me/password`; wired into inventory
  `Main`; `apps/platform` deleted.
- **Phase 5** — admin UI relocated to `platform/keystone-admin-ui`; `apps/inventory/frontend` is
  the host shell routing platform vs. tenant by `/me`.
- **Phase 6** — uniform login errors (no account enumeration).
- **Phase 7** — `AdminConfigLoaderTest` + `AdminIntegrationTest` (skipped without Docker) +
  relocated unit tests.
- **Phase 8** — CHANGELOG, README, ARCHITECTURE, frontend guidelines, compose.yaml updated.

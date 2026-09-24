# Phase 1 — Discovery & design

**Scope** — lock requirements, acceptance criteria, and contracts before building.

**Artifacts** — none (this phase produces decisions, recorded here).

**Dependencies** — none.

**Verification** — every open question has an agreed answer; the contracts below are confirmed.

## Requirements (agreed)

- Platform backend + UI become a library hosted with each app (inventory first).
- On inventory deploy, the first platform user is created in the `platform` schema.
- One common login screen; platform user → platform admin UI, tenant user → inventory UI.
- Login identity is `username@tenantid` (backend-proxied, per user decision).

## Contracts to confirm

- `POST /api/v1/auth/login` — request `{ identifier: "alice@acme", password }` (or the split
  `{ username, tenant, password }`); response carries the Supabase session
  (`accessToken`, `refreshToken`, …). Uniform error for unknown user vs. bad password (no
  account enumeration).
- `GET /api/v1/me` already returns `{ sub, tenantId, mustChangePassword, permissions[] }`;
  the shell routes on `tenantId == null` (platform) vs. non-null (tenant).
- Platform admin API surface (tenants / users / roles / permissions) is unchanged, only
  relocated from `apps/platform/server` into the library.

## Resolved decisions

| # | Decision |
| --- | --- |
| 1 | Tenant identifier — add unique `tenants.slug`; platform admin uses reserved slug `keystone` (`admin@keystone`, `users.tenant_id = NULL`) |
| 2 | `apps/platform` — deleted entirely (superseded by the library) |
| 3 | Auth — backend-proxied password grant; frontend `dio` + `flutter_secure_storage` + bearer interceptor + silent refresh; drop `flutter_appauth`; backend stays an OIDC resource server |
| 4 | Backend module — `platform/keystone-admin`, package `com.chetana.keystone.platform.admin` |
| 5 | Internal email — virtual/fake email, default `username@tenantid.com` (or custom at creation); created via service-role Admin API with `email_confirm: true`; **no email is sent** (no signup-confirm / reset / magic-link / invite flows; admin sets temp passwords) |

## Additional contracts

- **Realtime** — Supabase Realtime **Broadcast** (ephemeral, in-memory; not Postgres
  Changes). String-based routing, e.g. `/inventory/message_type/<username>` → channel
  `inventory:<username>`, event `message_type`. Already matches `keystone-realtime` and
  backend guidelines §8.
- **Login resolution** — input `username@tenantid` (split on the last `@`); usernames
  lowercase, no `@`, unique per tenant; tenant slugs DNS-safe, unique. Backend resolves
  tenant by slug → user by `(username, tenant)` → fake email → Supabase password grant.
  Custom fake emails entered at creation must be unique across the Supabase project.

# Changelog

All notable changes to Keystone are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Platform as an embedded library** — the platform admin console is no longer a standalone app;
  it is now two hosted libraries, `platform/keystone-admin` (backend) and
  `platform/keystone-admin-ui` (Flutter UI). `apps/inventory` hosts them: its server serves the
  platform admin API and runs the `platform` schema migration + first-user bootstrap; its frontend
  hosts the common login and routes to the admin UI (platform user) or inventory UI (tenant user).

  - **Backend** (`platform/keystone-admin`, package `com.chetana.keystone.platform.admin`):
    - Schema: `tenants` (+ `slug`), `users` (+ `username`; `email` is the virtual/fake Supabase
      identity, default `username@tenantid.com`), `roles`, `permissions`, `role_permissions`,
      `user_roles` (DDL-only Liquibase changelog, offline jOOQ codegen).
    - **Backend-proxied login** (`POST /api/v1/auth/login`): the client posts `username@tenantid`
      + password; the backend resolves the tenant (reserved slug `keystone` = platform plane) and
      user, authenticates against Supabase Auth (password grant), and returns the OIDC session.
      Failures are uniform (no account enumeration).
    - **Idempotent bootstrap** (`BootstrapRunner`) using the Supabase service-role key: provisions
      the first platform admin (`admin@keystone`), seeds the permission catalog and the
      `platform-admin` role.
    - **Backend-proxied change-password** (`POST /api/v1/me/password`); forced first-login change
      via `users.must_change_password`.
    - REST API: `POST /api/v1/auth/login`, `GET /api/v1/me`, `POST /api/v1/me/password`,
      `POST /api/v1/me/password-changed`, and permission-guarded CRUD for tenants, roles,
      permissions, and users.

  - **Frontend** (`platform/keystone-admin-ui` + `apps/inventory/frontend`):
    - Common login (`username@tenantid`), forced change-password gate, and a dashboard with
      Tenants / Roles / Permissions / Users tabs.
    - Riverpod, `dio` REST client, `flutter_secure_storage` tokens, and a `go_router` auth gate in
      the host that routes by `/me` (platform vs. tenant).

- **CORS support** — per-environment `cors.allowedOrigins` (empty = disabled, `*` = any origin),
  applied via Javalin's bundled CORS plugin; `dev` allows any origin for the local Flutter web
  client.
- **App context path** — `server.contextPath` (default `/inventory`) prefixes every route, so the
  inventory service serves at `http://localhost:8080/inventory`.

### Changed

- **The permission model is two access levels per resource (`read-only` / `read-write`)** — the
  platform catalog seeds `platform:<resource>:read-only|read-write` and
  `tenant:<resource>:read-only|read-write` for tenants, roles, permissions and users (14 codes + the
  `*` wildcard, down from 30 action-granular `create`/`read`/`update`/`delete`/`assign-role` codes).
  A read/write grant covers read, create, update and delete; a read-only grant covers read only;
  `PUT /api/v1/users/{id}/roles` needs `platform:user:read-write`. `POST /api/v1/permissions`
  rejects a code that does not end in a level, and the admin UI's permission dialog now picks a
  resource + a level instead of typing a free-text code (the `*` wildcard renders as read/write).
  See `docs/delivery/permission-access-levels/`.

- Platform admin backend/frontend moved from the standalone `apps/platform` app into the hosted
  `platform/keystone-admin` / `platform/keystone-admin-ui` libraries; `apps/platform` removed.
- Frontend auth is now **backend-proxied** (previously `flutter_appauth`/Supabase client-side); the
  guidelines now specify backend-proxied OIDC login.
- **Coding guidelines: JSON payloads must be typed models** — the backend now requires a Java
  record for every JSON shape (no `Map<String, Object>`/`JsonNode` payloads, config values, or
  third-party API responses) and the frontend a `freezed`/`json_serializable` model (no raw
  `Map<String, dynamic>` outside a generated `fromJson` signature or a single `data`-boundary
  conversion). See `docs/CODING_GUIDELINES_BACKEND.md` §13 and `docs/CODING_GUIDELINES_FRONTEND.md`
  §14.
- **JSON payloads are now typed models end to end** — the Supabase Auth adapter sends and parses
  records (`CreateUserRequest`, `PasswordGrantRequest`, `UpdatePasswordRequest`, `GoTrueUser`,
  `ListUsersPage`, `TokenResponse`) instead of building `Map`s and walking `JsonNode`s, the Realtime
  broadcast body is the `RealtimeEnvelope<T>` record (the port no longer takes an untyped
  `Object payload`), and `Principal.claims` is the typed `Claims` record rather than a raw claim
  map.
- **The Flutter admin UI's models are now `freezed` + `json_serializable`** — `Me`, `Tenant`, `Role`,
  `Permission`, `User` and `Session` (`Session` moved beside them out of the API client) plus the six
  request models generate their own `fromJson`/`toJson`/`copyWith`; the generated `*.freezed.dart`/
  `*.g.dart` files are committed. `platform/keystone-admin-ui` gains
  `freezed_annotation`/`json_annotation` and `freezed`/`json_serializable`/`build_runner`, and its
- **Both Flutter packages declare the same Dart SDK constraint (`'>=3.8.0 <4.0.0'`)** —
  `apps/inventory/frontend` is aligned with `platform/keystone-admin-ui` (which requires it for
  `json_serializable`), so the client code shares one language version and one `dart format` style.
- **The Flutter packages are now `dart format` clean** — a mechanical formatting pass normalised the
  10 files in `platform/keystone-admin-ui` that were already unformatted before this change (the
  Dart "tall" style that applies once a package's language version is 3.7+); the app's files needed
  no change. No behaviour change: `flutter analyze` clean and `flutter test` 7/7 in the package.
- `user_roles` primary key is `(user_id, role_id)` (its `tenant_id` is nullable); tenant-scoped role
  assignment is carried on `user_roles.tenant_id`.

### Fixed

- Bootstrap no longer re-lists Supabase Auth users by email on restart: it reads the persisted
  `users.sub` first and provisions (create-or-adopt on conflict) only when absent, fixing the 409
  "user already exists" on restart.
- **JWT signature verification now accepts elliptic-curve signing keys** (`ES256`, i.e. Supabase
  Auth's P-256 keys) alongside RSA, selecting the JWKS key named by the token's `kid` and falling
  back to every key when the `kid` is absent or unknown. Only RSA keys were tried before, so every
  Supabase-issued access token was rejected with `403 Invalid access token signature` — login
  succeeded (the token was issued) but `GET /api/v1/me` failed, blocking the first login.
- **Supabase issuer corrected to include `/auth/v1`** (`https://<ref>.supabase.co/auth/v1`), which
  is the `iss` claim Supabase Auth puts in access tokens; the previous value omitted the suffix and
  failed the issuer check.

### Security

- Supabase service-role key and JWKS/issuer settings are server-only (env / Secret Manager); the
  client bundle ships only the public backend URL and Supabase anon key.
- Login failures are uniform (unknown tenant / user / password all yield the same 403) to prevent
  account enumeration.

### Deployment

- A single app (inventory) now migrates both the `inventory` and `platform` schemas and bootstraps
  the first platform user; there is no separate platform service/deploy.

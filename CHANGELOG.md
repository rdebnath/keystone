# Changelog

All notable changes to Keystone are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **An access-level filter on the permission catalogue** — `GET /api/v1/permissions` and
  `GET /api/v1/tenant/permissions` accept `access=read-only` / `access=read-write`, matched as the **last segment
  of the code** (so the wildcard `*`, which carries no level, belongs to neither set). It composes with
  `q`/`scope`/`tenantId` and the page window, is validated like every other parameter (an unknown value is a
  `422`, an absent one filters nothing), and both planes serve it. The console's Permissions screen and the role
  picker gained a **Level** control for it, carried in the URL as `access=…` so a filtered catalogue survives a
  refresh and can be linked.

- **Server-side search, filtering and paging on every list** — the four admin lists (`tenants`, `users`,
  `roles`, `permissions`) are now paged, searched and filtered **by the server**, on both planes
  (`/api/v1/…` and `/api/v1/tenant/…`), so a search covers the whole collection rather than the page the
  console happens to be showing. Every list route takes `page` (0-based, ≤ 10 000), `size` (1–100, default
  25), `sort` + `order` (per-resource whitelists, with `id`/`code` tiebreakers so a page window is
  stable), `q` (a ≤ 100-character case-insensitive *contains* over the resource's documented columns,
  with `%`, `_` and `\` escaped so a search stays a search) and its existing filters (`tenantId`, plus
  `scope` on roles and permissions). An invalid window, an unknown sort key or an over-long term is a
  `422`; a page past the end is `200` with the real totals; **paging never widens visibility**, since the
  window is applied to exactly the `WHERE` the unpaged list used. The synthetic platform tenant is a
  **pinned, searchable first row** — counted in `totalElements`, and absent from a result whose term it
  does not match. **Breaking:** a list response is no longer a bare JSON array but a typed page envelope
  (`{items, page, size, totalElements, totalPages, hasNext, hasPrevious}`); the console in this
  repository is updated in the same change and no other client exists. Three new **unpaged** `/options`
  routes (`/api/v1/tenants/options`, `/api/v1/roles/options`, `/api/v1/tenant/roles/options`) serve the
  console's pickers — a dropdown must offer every choice, so a page cannot feed it — capped at 500 rows
  and reporting `truncated` rather than cutting a set short silently. The vocabulary is shared, not
  per-service: `PageRequest`, `Page`, `OptionList`, `SortOrder` and `SearchTerm` in `keystone-common`
  (`com.chetana.keystone.common.query`), `Search` in `keystone-data`, `QueryParams` in `keystone-web`,
  plus `0005-paging-indexes.xml` for the indexes each default order needs (`idx_tenants_name`,
  `idx_users_username`, `idx_users_tenant_username`). The admin console gains a debounced search box,
  filters, a sort control and a pager on every list, two distinct empty states, list state in the URL,
  and the shared `SearchField`/`ListToolbar`/`PagedListView`/`PaginationBar`/`SortSelect` widgets that
  make those rules the default. A list toolbar lines up as one row — the search box's label floats like
  the dropdowns', and the sort key's direction toggle lives **inside** the sort field — so controls of
  different heights share one top edge instead of drifting apart (`docs/UX_GUIDELINES.md` §1.16). The
  rules themselves are written down in the new **`docs/UX_GUIDELINES.md`** (§1), referenced by both
  coding guidelines and by `AGENTS.md`.

- **A country on a tenant, and a phone number on a user** — `tenants` gains a nullable `country` and
  `users` a nullable `phone_number`. Both are optional and validated server-side on write: the country
  is trimmed and upper-cased against the JDK's officially assigned ISO 3166-1 alpha-2 codes (`in` →
  `IN`, `IND` → `422`), and the phone number has the separators a human types (spaces, hyphens, dots,
  parentheses) stripped before the E.164 rule is applied, so `+91 98765 43210` stores as
  `+919876543210` and a number that is not E.164 answers `422`. An absent or blank value clears the
  field instead of being rejected; nothing is backfilled, so every existing tenant and user stays
  valid. `GET`/`POST /api/v1/tenants`, `GET`/`POST /api/v1/users` and
  `PATCH /api/v1/users/{id}` — plus the tenant self-service plane's user routes — carry the new
  fields; `PATCH` clears a blank or absent one, consistent with the full-body update semantics the
  tenant routes already had for `name`/`slug`. The admin console's tenant dialog gains a **Country**
  field and the user editor a **Phone number** field on both the create and edit forms, and each row
  shows its value when recorded. `GET /api/v1/me` is deliberately unchanged.

- **Tenant-scoped roles and permissions, and a tenant self-service plane** — `roles` and `permissions`
  gain an optional **owner** (`tenant_id`): `NULL` is the global platform-defined catalog (usable by the
  platform plane and every tenant), a tenant id is a row only that tenant and the platform plane can
  see. `code` is now unique **per owner** rather than globally, so two tenants can each have a `manager`
  role. Every tenant is seeded — in the same transaction that creates it — with its own `admin` role,
  the mirror of `platform-admin`, granted the **read/write** `TENANT`-scope codes only (a read/write
  grant already satisfies read). A holder of that role administers its own tenant through the new plane:
  `/api/v1/tenant/users` (list/create/update/delete, plus `PUT …/{id}/roles` and `PUT …/{id}/password`),
  `/api/v1/tenant/roles` and `/api/v1/tenant/permissions`, guarded by `tenant:user|role|permission:*`.
  The tenant is always derived from the caller's own user row — a tenant route has no path, query or body
  field that could name one — and a platform caller is refused there. Guardrails: grant only what you
  hold (holding a read/write code counts as holding the read-only one) and never the wildcard; a global
  row is read-only to a tenant; another tenant's row is a `404`; a tenant may not re-define a code the
  global catalog already carries; and the seeded `platform-admin` / `admin` roles can no longer be
  renamed or deleted on either plane. Deleting a tenant now removes the roles and permissions it owns, and
  `GET /api/v1/roles|permissions` accept `?tenantId=`.

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

- **A navigable admin console** — the platform console is a shell with a hideable left pane whose
  entries are the sections the signed-in user may read (`platform:<resource>:read-only|read-write`, or
  the `*` wildcard), instead of a fixed tab bar. Sections are real routes (`/tenants`, `/tenants/:id`,
  `/users`, `/roles`, `/permissions`), so they are deep-linkable; the pane is an inline column on wide
  layouts and a drawer on narrow ones.

  - **Backend** (`platform/keystone-admin`):
    - `GET /api/v1/tenants` returns the platform plane as a synthetic first row — `Keystone`
      (`platform: true`, reserved id `00000000-0000-0000-0000-000000000000`, reserved slug
      `keystone`, null timestamps). It is never persisted; `PATCH`/`DELETE` on it answer `422`, and
      creating or renaming a tenant to the reserved slug is rejected.
    - `GET /api/v1/users?tenantId=<uuid>` filters the user list: the reserved platform tenant id lists
      the platform users (`users.tenant_id IS NULL`), any other id lists that tenant's users, and an
      absent parameter keeps returning everyone. `POST /api/v1/users` accepts the reserved id as an
      alias for the platform plane.
    - `PATCH /api/v1/users/{id}` renames a user and replaces its role set in one transaction
      (`UserUpdateRequest(username, roles)`); the email stays immutable because it is the virtual
      Supabase Auth identity bound to `users.sub`.
    - `GET /api/v1/me` now also returns `username`.

  - **Frontend** (`platform/keystone-admin-ui`): `AdminShell` (collapsible pane, sections filtered by
    the caller's permissions, not-authorized and empty-console states), `TenantsScreen` (platform row
    plus add/edit/delete), `TenantUsersScreen` (the drill-down that answers "which users does this
    tenant have?", platform users included), `UsersScreen` (all users with a tenant filter) and a
    shared user editor (rename + role checklist limited to the user's plane). The hosting app routes
    the console under a `ShellRoute` and lands on the first section the caller may read.

- **Password management** — a signed-in user can change their own password at any time, an
  administrator can reset another user's password from the console, and every password field can be
  revealed to check what was typed.

  - **Self-service change** — `POST /api/v1/me/password` accepts `{currentPassword, password}`.
    Outside the forced first-login state the current password is **required and verified** against
    Supabase Auth (a real password grant for the caller's own identity), so a stolen bearer token alone
    cannot take an account over; a wrong one answers `422 Current password is incorrect.`. The forced
    first-login flow keeps its `{password}`-only body. The console exposes it as a "Change password"
    entry in the pane → dialog, reachable by every signed-in caller (it is not a permission), including
    one whose every section is denied.
  - **Console reset for another user** — `PUT /api/v1/users/{id}/password` (`{temporaryPassword}`) sets
    a user's temporary password in Supabase Auth and re-arms the forced change
    (`must_change_password = true`), exactly like `POST /api/v1/users`. It is guarded by the existing
    `platform:user:read-write` grant (no new permission code), answers `404` for an unknown user, `422`
    for a blank password, and **refuses the caller's own account** (`422`), which must use the verified
    flow — otherwise the current-password check above would be optional for a privileged caller. The
    user list gains a "Reset password" action with a revealable temporary-password field.
  - **Password fields can be revealed** — a shared `PasswordField` (trailing eye toggle, tooltip
    "Show password"/"Hide password") on the login screen, the first-login screen, the change-password
    dialog, the reset dialog and the user editor's temporary-password field. Toggling only switches the
    rendering (`obscureText`): the value, the focus and the submitted body are unchanged.
  - **No schema or catalog change** — the temporary password travels UI → API → Supabase Auth and is
    never stored in the platform schema or echoed back (`204`), and effective permissions are
    untouched. `PATCH /api/v1/users/{id}` still edits only `username` + `roles`.

- **CORS support** — per-environment `cors.allowedOrigins` (empty = disabled, `*` = any origin),
  applied via Javalin's bundled CORS plugin; `dev` allows any origin for the local Flutter web
  client.
- **App context path** — `server.contextPath` (default `/inventory`) prefixes every route, so the
  inventory service serves at `http://localhost:8080/inventory`.
- **The hosted UI is app-branded** — the shared platform UI renders its product title from a new
  `AppBranding` config (`appBrandingProvider`), which the hosting application injects once at its
  composition root. `apps/inventory` supplies `Keystone - Inventory Management`, so the login
  heading (previously the bare platform name `Keystone`), the browser tab title and the PWA
  manifest are app-specific.
- **Optional startup migration and platform bootstrap** — two switches, each with a yaml default and an
  environment override, both defaulting to the previous behavior (`on`):
  - `startup.migrateOnStart` (env `MIGRATE_ON_START`) — the automatic Liquibase run for this app's
    schema **and** the platform schema at startup.
  - `bootstrap.enabled` (env `BOOTSTRAP_ON_START`) — the startup seed of the permission catalog, the
    `platform-admin` role and the first platform admin user (provisioned in Supabase Auth).

  With the bootstrap disabled its admin credentials are no longer required; `SchemaTool migrate`
  always migrates, so it is the out-of-band path when the startup run is switched off.
- **Explicit migration and bootstrap steps** — the work the startup switches guard can now be run
  outside the server, so a release can migrate and seed as their own steps:
  - `scripts/migrate-schema.sh` wraps the existing `SchemaTool migrate` (own + platform schema).
  - `BootstrapTool` + `scripts/bootstrap-admin.sh` seed the permission catalog, the `platform-admin`
    role and the first platform admin user without starting the server. `BootstrapTool` requires the
    bootstrap to be enabled and otherwise fails with a clear message, so a deployment that switched
    the seed off cannot skip it silently; the wrapper forces `BOOTSTRAP_ON_START=true` for its run.
  - `scripts/start-server-migrate-and-bootstrap.sh` starts the server with both switches forced on
    (warning when it overrides an explicit `false`), for a local run against an environment whose
    configuration has them off.
- **Logging configuration owned by the service** — a service ships its own `logback.xml`, and platform
  libraries can only carry a test-scoped one (they must never put a logging configuration on a
  service's classpath).

  - `apps/inventory/server/src/main/resources/logback.xml` is the inventory service's configuration:
    it logs to stdout (Cloud Run captures stdout/stderr; the container writes no log files) with the
    level read from `LOG_LEVEL` (default `INFO`), so a dev service can turn on `DEBUG` without a new
    image. There was no configuration at all before, so Logback fell back to its default root level of
    `DEBUG` and jOOQ's `LoggerListener` logged every executed statement *with its bound parameters* —
    the email in the dev console noted in `docs/delivery/password-management/08-delivery.md`. That
    logger is now pinned at `INFO`, so not even `LOG_LEVEL=DEBUG` can put a parameter value in a log.
  - **No library ships a logging configuration or a logging backend.** A `logback.xml` under a
    library's `src/main/resources` travels inside its jar and becomes a second `logback.xml` on the
    hosting service's classpath, where which one wins is classpath order — not a decision. Every
    library with tests now keeps `src/test/resources/logback-test.xml` (never packaged, and Logback
    loads it ahead of `logback.xml`) with a test-scoped `logback-classic` dependency;
    `keystone-admin`'s was `runtime` and is now `test`, so the service — not the library — supplies
    the backend at runtime.
  - `apps/inventory/server/src/test/resources/logback-test.xml` keeps `mvn test` readable (short
    format, `INFO` for `com.chetana.keystone`, `WARN` elsewhere) while the packaged configuration
    keeps its Cloud Run settings.
  - The rule is stated in `docs/CODING_GUIDELINES_BACKEND.md` §10 and the README's run section.

### Changed

- **A role's permissions are now picked from the catalogue, not typed** — the admin console's create-role
  dialog asked for permission codes as free text (`Permissions (comma-separated)`), the one place a grant
  could be chosen and the one place a typo, a missing access level or a code of the wrong scope became a
  `422`. The field is now a **browsable picker** over the same paged, searchable catalogue the Permissions
  screen lists (`GET /api/v1/permissions` · `/api/v1/tenant/permissions`, unchanged): server-side search,
  sort and paging, the shared list states, the totals described (`1–25 of 43 · Page 1 of 2`), and a selection
  that survives paging and searching — with a count and a removable chip per code, so a set gathered across
  pages is reviewable before it is saved. The picker's query is **seeded from the role's owner and scope**, so
  every row offered is one `RoleService.grantPermissions` would accept; the scope is stated as text rather
  than offered as a filter that could only produce refusals, and a code the caller may not grant
  (`CallerScope.requireGrantable`: never the wildcard, never a code it does not hold — write implying read) is
  rendered **disabled with the reason** rather than selected and then refused. Changing the role's owner or
  scope drops the picks that no longer match and names them. **Roles also gained *Edit*** (`PATCH
  /api/v1/roles/{id}`, which the backend already exposed): the same dialog, prefilled, with the owner and a
  tenant-owned role's scope read-only because both are immutable server-side, and with no *Edit* action at all
  for the immutable seeded roles. **Console-only change** — no API, DTO, guard, database or deployment
  change; `docs/UX_GUIDELINES.md` §1.17 ("a picker over a set too large for one control is a paged list") was
  added to say how such a picker must behave.
- **A role or permission `code` no longer identifies one row** — `roles.code` and `permissions.code` were
  globally unique; they are now unique **per owner** (see Added), so a code can be carried by the global
  catalog and by any number of tenants. Lookups are owner-scoped, and a tenant may not re-define a code
  the catalog already carries, so a code still resolves to one row for a given caller. `roles` and
  `permissions` also gained a `tenantId` field on their DTOs and requests, and `GET /api/v1/roles` /
  `GET /api/v1/permissions` an optional `?tenantId=` filter.
- **The seeded administrative roles are now immutable** — `platform-admin` could be deleted by anyone
  holding `platform:role:read-write`, which locked the platform out of its own console; it and each
  tenant's `admin` now refuse rename and delete on both planes.

- **The first platform admin's username and email are per-environment yaml values** — `AdminConfigLoader`
  no longer carries built-in `admin` / `admin@keystone.com` fallbacks, and `admin-config/application.yaml`
  now holds them as blank, documented placeholders. Each deployment sets `bootstrap.adminUsername` /
  `bootstrap.adminEmail` in `admin-config/application-{env}.yaml` (`dev` and `demo` carry the previous
  values), so a different login name and email — e.g. to avoid a clash in a shared Supabase project —
  needs no code change. A missing value while the bootstrap is enabled now fails fast instead of
  silently seeding the built-in identity. The password is unchanged: `BOOTSTRAP_ADMIN_PASSWORD`, then
  yaml (the base file keeps `changeit` as a development default).

- **The admin console's tab shell is replaced by the navigable shell** — `DashboardScreen` and its
  `TabBar` are gone; `AdminShell` plus the router's section routes take their place, and the console's
  menu, screens and REST client moved to `AdminRoutes`/`AdminClient`-backed providers. The hosting
  frontend (`apps/inventory/frontend`) mounts the console under a `ShellRoute` at `/tenants`,
  `/tenants/:tenantId`, `/users`, `/roles` and `/permissions`; `platform/keystone-admin-ui` now depends
  on `go_router`. The console's tenant list carries a non-persisted first row (see Added), so a client
  that assumed every returned tenant is a database row must read the new `platform` flag.
- **The error-status contract is now documented as implemented** — `docs/CODING_GUIDELINES_BACKEND.md`
  (§8 validation, §9 error handling, §13 validation, §14 review checklist) and
  `docs/CODING_GUIDELINES_FRONTEND.md` §9 said a rejected value returns `400` while the shipped
  `ProblemDetailMapper` maps `ValidationException` → `422`; the guidelines now agree with the mapper
  (`404` unknown, `409` conflict, `422` rejected value, `403` denied, `500` unexpected) and reserve
  `400` for a request the framework could not parse. Documentation only — no behaviour change.
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

- **A failed Supabase password grant is no longer silent.** Every non-2xx from GoTrue (a wrong password,
  a revoked service-role key, a rate limit, a Supabase outage) was mapped to the same uniform
  `403 Invalid username, tenant, or password.` with nothing written to the log — on 2026-09-27 that made
  a wrong password typed into the browser field look exactly like a code regression for over an hour.
  `SupabaseHttpAdminClient.login` now logs one `WARN` with the HTTP status and GoTrue's machine-readable
  code (`Supabase password grant failed (HTTP 400, invalid_credentials)`) and parses the body into a
  typed `GoTrueError` record — never the email, the password, the body or GoTrue's `msg` (which can echo
  the submitted identifier). A non-JSON body reads as `unparsable` instead of throwing, so the endpoint's
  failure mode is unchanged: the client still gets the uniform `403`.
- **The build no longer emits `unknown enum constant jakarta.xml.bind.annotation.XmlAccessType.FIELD`
  warnings** (`keystone-data`, `keystone-admin`). jOOQ's `org.jooq.conf.*` classes — the
  `Settings`/`MappedSchema`/`RenderMapping` used to schema-qualify generated SQL — are JAXB-annotated,
  and jOOQ declares `jakarta.xml.bind:jakarta.xml.bind-api` as an *optional* dependency that the JDK no
  longer provides, so `javac` could not resolve the annotation's enum constant while compiling against
  those classes. The API is now pinned centrally (`<jakarta-xml-bind.version>`, matching the version
  jOOQ manages) and declared `provided` in the two modules that reference `org.jooq.conf` — compile-time
  only, nothing marshals XML and nothing changes on the runtime classpath or in the container image.
  The two unused-parameter warnings in `ProblemDetailMapperTest` were cleaned up at the same time with
  unnamed variables (`_`), so the IDE's inspections on the touched files are clean.
- **Enter now submits the login and change-password forms on the web build** — pressing Enter after
  typing the credentials did nothing. Both screens now declare `textInputAction` (`next`/`done`)
  plus `onFieldSubmitted`, so Enter moves from the username (or the new password) to the next
  field and submits from the last one; the first field is autofocused, and `_submit` is
  re-entrancy guarded so a keyboard action and a button activation cannot double-submit.
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

- **A global role can no longer hold a tenant's own permission** — `RoleService` selected a grantable permission
  with the *list* helper `ownerFilter`, where "no owner" means "no restriction", so a platform-defined (global)
  role could be granted a permission a single tenant had defined for itself. A global role is held by every
  tenant it is assigned to, so that made one tenant's own permission visible to all the others. Grants are now
  decided by the role's own owner (`grantableTo`): the global catalog, plus the owner's own rows — and the
  catalog alone for a role with no owner, refused with "a global role may hold the global catalog only". No rows
  were migrated: an existing role keeps its grants until its next update, which now refuses to carry such a grant
  forward. The console follows the rule — a role with no owner is picked from the global catalogue, and its owner
  filter is gone.

- Supabase service-role key and JWKS/issuer settings are server-only (env / Secret Manager); the
  client bundle ships only the public backend URL and Supabase anon key.
- Login failures are uniform (unknown tenant / user / password all yield the same 403) to prevent
  account enumeration.
- **Password changes prove ownership, resets do not.** A change of your *own* password requires the
  current one whenever the forced first-login state is over (verified server-side with a Supabase
  password grant), and the console's reset deliberately refuses the caller's own account so that proof
  cannot be bypassed by a holder of `platform:user:read-write`. Both operations are logged server-side
  (target user id + actor `sub` only); no password, token, key, email or GoTrue `msg` is ever logged.

### Deployment

- **Password management and the login-diagnosability fix need no migration.** No Liquibase changeset, no
  jOOQ codegen churn and no `PermissionCatalog` change: the reset is a write on the existing
  `platform:user` resource, and the temporary password is handed to Supabase Auth and never stored. No
  new dependency either, so no `flutter pub get` step — but `platform/keystone-admin-ui`'s generated
  request models are rebuilt (committed), and backend + frontend ship together in one release. The API
  is backward compatible: a `{password}`-only change-password body is still accepted for the forced flow,
  so an older console keeps working against the new server.
- **The navigable admin console needs no migration.** No Liquibase changeset and no jOOQ codegen churn
  (the generated sources are byte-identical): the `Keystone` platform tenant is computed, not stored.
  `platform/keystone-admin-ui` gains the `go_router` dependency, so run `flutter pub get` in it and in
  `apps/inventory/frontend`; the backend and frontend ship together in one release.
- A single app (inventory) now migrates both the `inventory` and `platform` schemas and bootstraps
  the first platform user; there is no separate platform service/deploy.
- **The startup migration and the platform bootstrap are now switchable per deployment** (see Added).
  Nothing changes by default. A deployment that sets `MIGRATE_ON_START=false` must apply the migrations
  itself — `scripts/migrate-schema.sh` / `SchemaTool migrate`, e.g. as a Cloud Run job — before the
  revision serves traffic, since the bootstrap (and every query) still expects the schemas to exist;
  a deployment that sets `BOOTSTRAP_ON_START=false` seeds with `scripts/bootstrap-admin.sh` when it
  wants to.
- **Logging configuration ships with the image — no migration, no new runtime dependency.** The
  service's `logback.xml` is packaged in `apps/inventory/server`'s jar and applied on startup; set
  `LOG_LEVEL` on the revision to change verbosity (default `INFO`). Moving `keystone-admin`'s
  `logback-classic` to `test` scope only takes it out of that *library's* transitive graph — the
  service declares the backend itself, so a deployment is unaffected.

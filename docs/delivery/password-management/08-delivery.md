# 08 — Delivery

## Scope / Artifacts / Dependencies / Verification

- **Scope** — the release artifacts: changelog, architecture doc, deployment notes, the manual
  walk-through that proves the feature on a running server.
- **Artifacts** — `CHANGELOG.md`, `docs/ARCHITECTURE.md` (§9.1 Authentication, §9.5 Delegated
  administration), this file.
- **Dependencies** — phases 3–7 complete and green.
- **Verification** — `git diff` of the docs, plus the walk-through below against the local dev server.

## CHANGELOG entries (to add under `[Unreleased]`)

- **Added**
  - **Self-service password change** — `POST /api/v1/me/password` accepts `{currentPassword, password}`;
    outside the forced first-login flow the current password is required and verified against Supabase
    Auth (a wrong one answers `422 "Current password is incorrect."`), so a stolen bearer token cannot
    silently take an account over. The console exposes it as a "Change password" entry in the pane →
    dialog for any signed-in user.
  - **Console password reset for another user** — `PUT /api/v1/users/{id}/password`
    (`{temporaryPassword}`) sets a user's temporary password in Supabase Auth and re-arms the forced
    change (`must_change_password = true`). It needs `platform:user:read-write` (no new permission code)
    and refuses the caller's own account, which must use the verified flow. The user list gains a
    "Reset password" action with a revealable temporary-password field.
  - **Password fields can be revealed** — a shared `PasswordField` (trailing eye toggle, tooltip
    "Show password"/"Hide password") on the login screen, the first-login screen, the change-password
    dialog, the reset dialog and the user editor. Toggling does not change the value or the body sent.
- **Fixed**
  - **A failed Supabase password grant is no longer silent** — every non-2xx from GoTrue was mapped to
    the uniform `403 "Invalid username, tenant, or password."` with nothing logged, so a wrong password,
    a revoked service-role key, a rate limit and a Supabase outage were indistinguishable (this is what
    made the 2026-09-27 `admin@keystone` login look like a regression). `SupabaseHttpAdminClient.login`
    now logs one `WARN` with the HTTP status and GoTrue's machine-readable code
    (`… (HTTP 400, invalid_credentials)`), and the body/envelope parsing is a typed `GoTrueError` record
    that tolerates unknown fields. The client-visible `403` is unchanged.
- **Security**
  - Password changes and resets are audited server-side (`INFO` with the target user id and the actor
    `sub`); no password, token, key, email or GoTrue `msg` is ever logged.

## Documentation updates

- `docs/ARCHITECTURE.md`
  - **§9.1 Authentication** — record the password-management rules next to the identity model: the
    Java service never *stores* a credential, but it proxies two password operations (change-own with
    current-password proof; admin reset that forces a change) through the service-role GoTrue admin API.
  - **§9.5 Delegated administration** — add the reset to the delegation table (platform admin resets a
    user's temporary password; the target must change it on next login; self-reset is refused).
- `docs/CODING_GUIDELINES_*` — no change: the feature follows the existing rules (typed records at
  every boundary, `422` for a rejected value, no PII in logs, one form widget per flow).
- `README.md` — unchanged (no new configuration, no new secret, no new command).

## Deployment

- **No migration** — no Liquibase changeset, no jOOQ codegen churn, no new column, no new permission
  code and therefore no bootstrap change (`PermissionCatalog` is untouched).
- **No new dependency** — no `pom.xml` or `pubspec.yaml` change, so no `flutter pub get` step for the
  console or the host app. `build_runner` output for the one new request model is committed.
- **No configuration** — no env var, no `--dart-define`, no CORS or `APP_ENV` change.
- Backend and frontend ship together in one release, as with `admin-console-navigation`; the API is
  backward compatible, so the old client keeps working against the new server (`{password}`-only bodies
  are still accepted for the forced flow) and the new console needs the new server only for the reset
  action.
- **Rollback** — revert the release; no data was migrated and the only lasting effect of a reset is the
  password the admin set in Supabase Auth (the platform rows are untouched apart from
  `must_change_password`).

## Manual verification (local dev)

```bash
# 1. forced flow untouched: sign in as admin@keystone, change the password when asked
# 2. reveal toggle: type a password on the login screen and click the eye (text becomes readable)
# 3. voluntary change: console pane -> Change password
#      wrong current password -> 422 detail shown on the form
#      right current password -> success; sign out and sign in with the new password
# 4. admin reset: Users -> a user row -> key icon -> temporary password -> 204
#      the row shows "must change password"; sign in as that user with the temporary password
# 5. diagnosability: POST /api/v1/auth/login with a wrong password
#      response: 403 ACCESS_DENIED (unchanged)   server log: WARN … (HTTP 400, invalid_credentials)
```

## Follow-ups (not blockers) — see the consolidated list after the Result below

- A password **policy** (length/complexity/expiry) is still a product decision; nothing here constrains
  it, and GoTrue keeps its own minimum.

## Result — executed (2026-09-27)

**Release artifacts**

- `CHANGELOG.md` — an **Added** block ("Password management": self-service change with the
  current-password proof, the console reset with the self-reset refusal, the reveal toggle, no schema or
  catalog change), a **Fixed** entry for the now-logged Supabase password grant, a **Security** entry for
  the ownership-proof/delegation rules, and a **Deployment** entry (no migration, no new dependency, one
  release, backward-compatible body).
- `docs/ARCHITECTURE.md` — §9.1 gained "Password operations are proxied, never stored" (the two flows,
  what each proves, what is logged, the uniform `403` with a server-side reason) and §9.5 now records the
  reset in the platform admin's delegation, including the self-reset refusal.
- `README.md` — verified unchanged: no new configuration, secret, endpoint or command for operators.
- No `pom.xml`/`pubspec.yaml` change; the regenerated `requests.freezed.dart`/`requests.g.dart` are
  committed with the feature.

**Verification**

- `mvn clean verify` (repo-wide) — **BUILD SUCCESS**, 93 tests, 0 failures, 0 errors, 0 skipped.
- **Live walk-through** against the dev server (`localhost:8080/inventory`, real Supabase Auth + real
  Postgres, fresh build): **24/24 checks passed** —

  ```text
  login as admin with the bootstrap password ....... 200
  wrong password stays a uniform 403 .............. 403  (detail unchanged)
  forced first-login change ....................... 204  (then /me mustChangePassword=false)
  voluntary change without currentPassword ........ 422
  voluntary change with a wrong currentPassword ... 422
  blank new password .............................. 422
  voluntary change with the right currentPassword . 204
  the new password is the one Auth holds .......... 200   (+ the replaced one: 403)
  reset another user's password ................... 204
  the target signs in with the temporary one ...... 200   (+ their old one: 403)
  the target is flagged for a forced change ....... mustChangePassword=true
  blank temporary password / unknown user ......... 422 / 404
  resetting your own account ...................... 422  ("Use the change-password flow for your own account")
  reset without a token ........................... 403
  ```

  The server log showed the new WARN line (`… (HTTP 400, invalid_credentials)`) and the reset audit
  `INFO` line (phase 6), and the temporary verification tenant/user were deleted afterwards.

**Environment note (dev project):** the walk-through logs in as `admin@keystone`, so it changed that
account's password and `must_change_password`; the state was restored to the documented bootstrap state
(`changeit` + a forced change on next login, no leftover tenant/user, no leftover Supabase Auth user).
The server was restarted from the IDE's `Inventory` run configuration afterwards.

## Follow-ups (not blockers)

- A password **policy** (length/complexity/expiry) is still a product decision; nothing here constrains
  it, and GoTrue keeps its own minimum.
- "Forgot password" (email recovery) and tenant-plane reset remain unbuilt; both would need a decision
  on who may trigger a recovery mail.
- **Dev logging level:** with no `logback.xml` the root logger is DEBUG, so jOOQ's `LoggerListener`
  logs executed statements *with bound parameters* (emails appear in the dev console). Pre-existing and
  untouched by this feature; pinning a per-environment log level (or raising jOOQ's listener to INFO)
  would be a small, worthwhile follow-up.
  **Resolved** (after this feature): the service now ships
  `apps/inventory/server/src/main/resources/logback.xml` — stdout, `LOG_LEVEL` (default `INFO`), and
  `org.jooq.tools.LoggerListener` pinned at `INFO` so no level setting can leak bound parameters.
- `docs/ARCHITECTURE.md` §9.4 still documents `user_roles`' primary key differently from
  `0001-initial-schema.xml` (pre-existing, spotted during `admin-console-navigation`).

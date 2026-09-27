# Delivery Plan — Password management & login diagnosability

**Feature slug:** `password-management`
**Level:** Platform-level — touches `platform/keystone-admin` (backend), `platform/keystone-admin-ui`
(Flutter), and `docs/`. No app code, no router change, no database change.

## Sizing decision

**Produce a plan.** Three requests that together are a capability, not a tweak:

1. a **diagnosability bug fix** in the GoTrue adapter (every non-2xx is reported to operators as
   "wrong password" with no log at all — the reason the `admin@keystone` login took an hour to explain);
2. a **password-management gap**: a user whose `must_change_password` is `false` has *no* way to change
   their own password, and no console path can reset one (`POST /users` only provisions; `PATCH` has no
   password field);
3. a **UI change on every password field** (reveal what was typed).

It adds an API route and a request-body contract, changes the behaviour of an existing route, and is
security-relevant (it decides who may set whose password and what must be proven first).

## Summary

| # | Request | Delivered by |
| --- | --- | --- |
| 1 | Log the GoTrue failure instead of hiding it behind the uniform 403 | `SupabaseHttpAdminClient.login` logs `WARN` with the HTTP status + GoTrue `error_code` (never the email, password or message); new `GoTrueError` wire record |
| 2a | Self-service password change when `mustChangePassword` is `false` | `POST /api/v1/me/password` gains `currentPassword`, **required and verified** unless the caller is in the forced first-login state; console pane gains a "Change password" dialog |
| 2b | Admin/console reset for another user | new `PUT /api/v1/users/{id}/password` (`temporaryPassword`) — permission-guarded, targets another user only, re-arms the first-login change; "Reset password" action on the user row |
| 3 | Users can reveal the password text | shared `PasswordField` (visibility toggle) used by the login screen, the first-login screen, the change-password dialog, the reset dialog and the user editor |

## Phase list

1. Discovery & design — `01-discovery-and-design.md`
2. Database changes — **skipped.** No DDL and no new permission code: the reset is a write on the
   existing `platform:user` resource (`platform:user:read-write`), the new request bodies are transient,
   and no column is added. Keeping the changelog DDL-only keeps the offline jOOQ codegen deterministic.
3. Domain & application services — `03-domain-and-application-services.md`
4. Server-side API & realtime — `04-server-side-api-and-realtime.md`
5. Frontend / UI (Flutter) — `05-frontend-ui-flutter.md`
6. Security & observability — `06-security-and-observability.md`
7. Testing — `07-testing.md`
8. Delivery — `08-delivery.md`

## Design in one page

### Three password paths (who proves what)

```
forced first login      POST /me/password            {password}
  (must_change_password=TRUE)   caller just authenticated with that password — nothing else to prove
                                -> GoTrue admin updatePassword + must_change_password=false

voluntary change        POST /me/password            {currentPassword, password}
  (must_change_password=FALSE)  currentPassword is REQUIRED and verified with a GoTrue password grant
                                -> wrong  => 422 "Current password is incorrect."  (token theft cannot
                                   silently take the account over)
                                -> right  => GoTrue admin updatePassword

admin reset             PUT /users/{id}/password     {temporaryPassword}
                                needs platform:user:read-write; target must NOT be the caller
                                -> GoTrue admin updatePassword + must_change_password=true
```

The forced flow keeps working unchanged (`{password}` only), so the shipped first-login screen and its
tests are unaffected.

### Reveal toggle

```
┌ Change password ─────────────────────────────┐
│ Current password  ••••••••              👁   │   Icons.visibility / visibility_off
│ New password      ••••••••              👁   │   toggles obscureText in place
│ Confirm password  ••••••••              👁   │   (nothing is sent or stored either way)
└──────────────────────────────────────────────┘
```

### Why a dialog and not a route for the voluntary change

`/change-password` is the **forced** gate: the host router redirects away from it as soon as
`/me.mustChangePassword` is false, and the console shell only renders `AdminSection` routes. The
voluntary change is therefore a dialog opened from the console pane (`Change password` entry, above
`Sign out`), which needs no host-router change and cannot be confused with the gate. Both hosts render
one shared form widget, so the two flows cannot drift.

## Decisions (proposed)

1. **Current password verification is server-side** — the client cannot be trusted to prove it, and the
   backend already holds the service-role key. Verified with a normal GoTrue password grant against
   `users.email` (looked up by `sub`), for the same reason `LoginService` does.
2. **A rejected current password is a `422`** (`ValidationException`) — the shipped mapping calls a
   rejected *value* 422, and the dialog shows the `detail` on the field.
3. **The admin reset re-arms the forced change** (`must_change_password = true`): the value the admin
   typed is a *temporary* password by definition, exactly like `POST /users`.
4. **The reset endpoint refuses to target the caller** (`422`): otherwise "prove your current password"
   is bypassable by anyone holding `platform:user:read-write`, including its holder. The caller uses the
   verified flow instead.
5. **No new permission code and no new database column** — the reset is a write on `platform:user`; the
   temporary password is passed straight to GoTrue and never stored in the platform schema.
6. **The GoTrue failure is logged without the payload** — status + machine-readable `error_code` only
   (`msg` can echo the submitted identifier, the body carries the password).
7. **`PasswordField` is a plain wrapper, not a new input flavour** — it renders one `TextFormField`
   plus a suffix `IconButton`, so existing tests (`find.byType(TextFormField).at(1)`) keep working.
8. **Tenant-plane reset is out of scope** — the console and `UserHandler` are platform-plane only today
   (`tenant:user:*` codes exist in the catalog but no tenant handler consumes them).

## Open questions

1. Confirm decision 4 (reject a self-targeted reset with `422`) — the alternative is to allow it as a
   convenience, which makes the current-password check optional for privileged callers.
2. Confirm decision 3 (reset always re-arms the forced change) — the alternative is a "set a permanent
   password" mode, which the console should then ask about explicitly.
3. Confirm the voluntary change lives in a **dialog** (see "Why a dialog…") rather than in a new
   `/account/password` route added to the host router.
4. Should the reset dialog offer a "Generate" button for the temporary password? Proposed: **no** —
   manual entry with the reveal toggle; a generator is a separate UX decision.

## Confirmation state

- [x] **Plan reviewed — approved as written** (2026-09-27), with the instruction to run through all phases
      without pausing. Decisions 1–8 implemented as written; the four open questions were answered by the
      proposed defaults (reject a self-targeted reset, a reset always re-arms the forced change, the
      voluntary change is a dialog, no password generator).
- [x] **Phase 3 executed** — see `03-domain-and-application-services.md`.
- [x] **Phase 4 executed** — see `04-server-side-api-and-realtime.md`.
- [x] **Phase 5 executed** — see `05-frontend-ui-flutter.md`.
- [x] **Phase 6 executed** — see `06-security-and-observability.md`.
- [x] **Phase 7 executed** — 59 backend tests + 53 frontend tests green; `mvn clean verify` green.
- [x] **Phase 8 executed** — CHANGELOG + ARCHITECTURE updated, live walk-through 24/24 (see
      `08-delivery.md`).
- **One addition to the plan while executing** (recorded in phase 5): the no-readable-section branch of
  `AdminShell` now carries an AppBar "Change password" action, because that branch renders no menu at all
  and "changing your own password" is not a permission — without it the very case the feature was built
  for (a signed-in user who cannot reach `/change-password` any more) would still be unreachable for that
  caller.

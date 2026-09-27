# 01 — Discovery & design

## Scope / Artifacts / Dependencies / Verification

- **Scope** — pin the requirements, the two request-body contracts, the acceptance criteria and the
  explicit non-goals for `password-management`.
- **Artifacts** — this file; the API contracts mirrored into phases 3–5.
- **Dependencies** — none (first phase).
- **Verification** — the acceptance criteria below are the checklist phases 3–7 are reviewed against.

## Requirements (as requested)

| # | Requirement | Source |
| --- | --- | --- |
| R1 | A GoTrue failure must be *visible* to an operator; it must stop masquerading as "invalid credentials" | request 1 |
| R2 | A signed-in user must be able to change their own password even when `must_change_password` is `false` | request 2 |
| R3 | An administrator must be able to reset another user's password from the console | request 2 |
| R4 | A user must be able to reveal what they typed in a password field | request 3 |

R1 is a bug fix: on 2026-09-27 the `admin@keystone` login answered `403 "Invalid username, tenant, or
password."` for a *wrong password typed into the browser field*, while the same response would have been
produced by a `401`, a `429` rate limit or a `5xx` from Supabase — with nothing in the server log to
tell them apart.

## Acceptance criteria

### R1 — diagnosability

1. `SupabaseHttpAdminClient.login` logs **exactly one `WARN`** when GoTrue answers non-2xx, containing
   the HTTP status and the machine-readable GoTrue code (`error_code`, else `code`), e.g.
   `Supabase password grant failed (HTTP 400, invalid_credentials)`.
2. The log line carries **no** email, password, token or GoTrue `msg` (msg can echo the submitted
   identifier) and no request body.
3. A non-JSON or empty error body still logs (the code reads as `unparsable`) and the failure mode is
   unchanged: `AccessDeniedException` → `403` for the client.
4. A 2xx path logs nothing new.

### R2 — self-service change

5. `POST /api/v1/me/password` with `{"password": "…"}` keeps working when the caller's
   `must_change_password` is `true` (the shipped first-login flow) → `204`.
6. When `must_change_password` is `false`:
   - `{"password": "…"}` alone → `422` (`currentPassword` is required);
   - `{"currentPassword": "wrong", "password": "…"}` → `422` `"Current password is incorrect."`;
   - `{"currentPassword": "<the real one>", "password": "…"}` → `204`, and the *new* password
     authenticates on the next login (verified against GoTrue, not just the flag);
   - a blank `password` → `422` either way.
7. The console provides the flow: a "Change password" entry in the pane, above "Sign out", opening a
   dialog with current/new/confirm fields; success shows a snack bar and closes the dialog.

### R3 — console reset

8. `PUT /api/v1/users/{id}/password` with `{"temporaryPassword": "…"}`:
   - without `platform:user:read-write` → `403`;
   - unknown user id → `404`;
   - blank `temporaryPassword` → `422`;
   - `id` = the caller's own user → `422` (use the verified flow; see decision 4 in `plan.md`);
   - otherwise → `204`, the target's GoTrue password becomes the temporary one, and
     `must_change_password` becomes `true` (so the next login forces a change — and the console shows
     "must change password" on that row).
9. The user list exposes the action (key icon) wherever edit/delete are shown, behind the same
   `canManage` flag, with a confirmation-free dialog that does not close until the call succeeds.
10. The reset never stores the temporary password in the platform schema (it only reaches GoTrue), and
    the API returns `204` with no body — no password is echoed back.

### R4 — reveal

11. Every password input in the platform UI has a trailing visibility toggle that switches the field
    between obscured and plain text in place; the toggle is keyboard/pointer reachable with a tooltip
    ("Show password" / "Hide password").
12. Toggling never changes the value, focus or the submitted request.
13. Covered fields: login screen, first-login change-password screen, the new change-password dialog,
    the reset-password dialog, and the user editor's temporary-password field.

## Contracts

```jsonc
// POST /api/v1/me/password          (204; caller's own password)
{ "currentPassword": "…",   // required unless users.must_change_password = true
  "password": "…" }         // new password

// PUT /api/v1/users/{id}/password   (204; another user's temporary password)
{ "temporaryPassword": "…" }
```

- Backend records: `identity/ChangePasswordRequest` (gains `currentPassword`), new
  `user/ResetPasswordRequest`.
- Flutter mirrors: `models/requests.dart` → `ChangePasswordRequest` (gains a nullable
  `currentPassword`), new `ResetPasswordRequest`.
- No response body change on either route (`204`), so no model is added for a response.

## Error mapping (unchanged, reused)

| Failure | Exception | Status |
| --- | --- | --- |
| unknown user id | `NotFoundException` | `404` |
| missing/blank field, wrong current password, self-targeted reset | `ValidationException` | `422` |
| wrong password / unknown tenant at login | `AccessDeniedException` | `403` |
| missing `platform:user:read-write` | `AccessDeniedException` | `403` |

## Non-goals

- No password *policy* change (length/complexity/expiry) — a separate decision; GoTrue enforces its own
  minimum. The console keeps its existing `>= 8` client-side hint.
- No "forgot password" / email-recovery flow, no self-service signup.
- No tenant-plane (tenant-admin) reset UI: the console is platform-plane only.
- No password history, no "MFA", no session invalidation on change.
- No change to `/api/v1/auth/login` behaviour: the uniform `403` stays (anti-enumeration).

## Result

Design fixed; the contracts above are what phases 3–5 implement. No code, schema or configuration
change in this phase.

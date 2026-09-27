# 07 — Testing

## Scope / Artifacts / Dependencies / Verification

- **Scope** — prove the acceptance criteria of `01-discovery-and-design.md`, including the
  "the new password actually authenticates" check (not just the flag write).
- **Artifacts** — `platform/keystone-admin/src/test/...` (integration + unit), `platform/keystone-admin-ui/test/...`
  (widget + model tests).
- **Dependencies** — phases 3–6 implemented.
- **Verification** — the commands at the bottom, run and recorded in this file.

## Backend — unit

| Test | Asserts |
| --- | --- |
| `SupabaseHttpAdminClientTest.should_read_the_gotrue_failure_code` | `invalid_credentials` is read from a real GoTrue 400 body (`{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}`) |
| `SupabaseHttpAdminClientTest.should_fall_back_to_the_status_code_when_error_code_is_absent` | a body with only `code` still yields a code |
| `SupabaseHttpAdminClientTest.should_report_a_non_json_body_as_unparsable` | an HTML/empty body returns `unparsable` instead of throwing (the endpoint's failure mode cannot regress) |
| `GoTrueWireRecordsTest` (extend) | the new `GoTrueError` record tolerates unknown fields |

Where a test needs an HTTP round trip, use the JDK's `com.sun.net.httpserver.HttpServer` bound to
`localhost:0` and an `AdminConfig.Supabase` pointing at it — no new test dependency, no real Supabase.

## Backend — integration (`AdminIntegrationTest`, Testcontainers PostgreSQL + real Guice injector)

The fake `SupabaseAdminClient` in that test grows from "always succeeds" into a small password store so
the verification path can be exercised honestly:

```java
// passwords: email -> current password, seeded with the bootstrap "changeit" and updated by
// updatePassword(...). login(email, password) throws AccessDeniedException unless it matches.
```

| Scenario | Expected |
| --- | --- |
| forced change: `POST /me/password {password}` while `mustChangePassword = true` | `204`; `/me` then reports `mustChangePassword: false` |
| voluntary change without `currentPassword` | `422` |
| voluntary change with a wrong `currentPassword` | `422` `"Current password is incorrect."` |
| voluntary change with the right `currentPassword` | `204`; the *new* password authenticates at `POST /api/v1/auth/login` |
| blank `password` | `422` |
| `PUT /users/{id}/password {temporaryPassword}` for another user | `204`; the target's row reports `mustChangePassword: true`; `POST /api/v1/auth/login` with the temporary password succeeds |
| reset with a blank `temporaryPassword` | `422` |
| reset for an unknown id | `404` |
| reset targeting the caller's own user | `422` |
| reset without an `Authorization` header | `403` |

The existing assertions in that test (bootstrap login, tenant/user CRUD, the platform tenant row) stay
green: the reset route is additive and the change-password call keeps its old shape.

## Frontend — widget/unit (`platform/keystone-admin-ui/test`)

| Test | Asserts |
| --- | --- |
| `password_field_test.dart` (new) | obscured by default; tapping the eye reveals the typed text (`EditableText.obscureText == false`); tapping again re-hides; the value survives the toggles; the tooltip flips |
| `auth_screens_test.dart` (extend) | the login screen submits the *same* values after revealing the password; the forced screen still sends `{password}` with no `currentPassword`; the form keeps its focus/Enter behaviour |
| `change_password_dialog_test.dart` (new) | the dialog asks for the current password, sends `currentPassword` + `password`, surfaces the backend `detail` on failure and stays open, and closes on success |
| `user_editor_test.dart` (extend) | the row shows "Reset password" only when `canManage`; the dialog sends `PUT /users/{id}/password` with the typed temporary password; a failure keeps it open and shows the `detail` |
| `admin_shell_test.dart` (extend) | the pane renders "Change password" for every caller (also one whose sections are all denied) and opening it shows the dialog |
| `models_test.dart` (extend) | `ResetPasswordRequest.toJson()` emits `{"temporaryPassword": …}`; `ChangePasswordRequest` omits `currentPassword` when null and includes it when set |

## Commands

```bash
# backend (Docker required for the Testcontainers test)
mvn -q -pl platform/keystone-admin -am test

# frontend
/Users/rajeshdebnath/manual-install/flutter/bin/flutter analyze
/Users/rajeshdebnath/manual-install/flutter/bin/dart format --set-exit-if-changed lib test
/Users/rajeshdebnath/manual-install/flutter/bin/flutter test
```

Run the frontend commands in `platform/keystone-admin-ui` and again in `apps/inventory/frontend`
(the host app compiles the changed screens through the package).

## Result — executed (2026-09-27)

| Suite | Result |
| --- | --- |
| `mvn -pl platform/keystone-admin -am test` | **keystone-admin 59 tests, 0 failures, 0 errors, 0 skipped** (Testcontainers PostgreSQL, real Guice + Javalin), plus the 23 tests of the reactor's other modules |
| `flutter analyze` (`platform/keystone-admin-ui`) | clean |
| `dart format --set-exit-if-changed lib test` | clean (4 files reformatted by the pass) |
| `flutter test` (`platform/keystone-admin-ui`) | **53/53 passed** (was 36) |
| `flutter analyze` (`apps/inventory/frontend`) | clean; `flutter test` has no directory there (the app ships no tests) |
| `mvn clean verify` (repo-wide) | see phase 8 |

New/extended backend tests: `SupabaseHttpAdminClientTest` (7 — `failureCode` for `error_code`, for the
numeric fallback, for a non-JSON body, the `WARN` capture that also asserts the credentials are absent
from the line, and a real socket round trip for the 400/200 paths), `GoTrueWireRecordsTest` (8 — the new
`GoTrueError`), `AdminIntegrationTest` (the stateful `FakeSupabaseAdminClient` plus forced/voluntary
change, missing/wrong/blank current password, and the reset's `204`/`403`/`404`/`422` matrix and
"the new password really authenticates").

New/extended frontend tests: `password_field_test.dart` (3), `change_password_dialog_test.dart` (4),
`auth_screens_test.dart` (reveal + forced-flow assertions), `user_editor_test.dart` (2),
`admin_shell_test.dart` (3, including the narrow drawer path), `models_test.dart` (3).

Two failures were hit and fixed while executing (both test-side, no production change):

1. A Java text block in `SupabaseHttpAdminClientTest` ended a line with a double backslash, so Jackson
   saw a stray `\` in the token body (`JsonParseException`) — the continuation is a single `\`.
2. `find.text('…')` matches an `EditableText` by its *controller* value even when the field is obscured,
   so a "the password is not visible" assertion on `find.text` was meaningless: the tests now assert
   `EditableText.obscureText` (the rendering switch) and the controller value instead.

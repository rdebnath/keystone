# 05 — Frontend / UI (Flutter)

## Scope / Artifacts / Dependencies / Verification

- **Scope** — the reveal toggle on every password input, the voluntary "Change password" flow, and the
  console's "Reset password" action.
- **Artifacts** — `platform/keystone-admin-ui`: new `core/password_field.dart`, new
  `features/auth/change_password_form.dart` + `change_password_dialog.dart`,
  `features/auth/login_screen.dart`, `features/auth/change_password_screen.dart`,
  `features/auth/auth_service.dart`, `features/admin/admin_shell.dart`,
  `features/admin/user_editor.dart`, `core/api_client.dart`, `models/requests.dart`,
  `keystone_admin_ui.dart`.
- **Dependencies** — phase 4 (routes/contracts). No `apps/inventory/frontend` change: the host router,
  its `ShellRoute` and `AdminRoutes` are untouched (the voluntary change is a dialog, see `plan.md`).
- **Verification** — `flutter analyze`, `dart format`, `flutter test` in `platform/keystone-admin-ui`
  and `apps/inventory/frontend`.

## 1. `core/password_field.dart` (new) — the reveal toggle

```dart
/// A password input that can be revealed in place: one [TextFormField] plus a trailing visibility
/// toggle. The toggle only switches `obscureText`; the controller, the value, the focus and the
/// submitted body are unaffected.
class PasswordField extends StatefulWidget {
  const PasswordField({
    super.key,
    required this.controller,
    required this.labelText,
    this.helperText,
    this.validator,
    this.autofocus = false,
    this.textInputAction,
    this.onFieldSubmitted,
    this.focusNode,
    this.initialVisible = false,
  });
  // ... fields as above
}
```

- Reuses the existing conventions: `TextFormField` + `InputDecoration`, `_visible` is widget state.
- Trailing `IconButton(icon: Icon(_visible ? Icons.visibility_off : Icons.visibility))` with
  `tooltip: _visible ? 'Hide password' : 'Show password'` and `onPressed: toggle`, so it is reachable
  by pointer and keyboard.
- Renders exactly one `TextFormField`, so existing lookups (`find.byType(TextFormField).at(i)`) and
  Enter-to-submit behaviour (`textInputAction` / `onFieldSubmitted`) keep working.
- Exported from `keystone_admin_ui.dart` so hosting apps and tests can use it.

## 2. `features/auth/change_password_form.dart` (new)

One form, two hosts (forced screen, voluntary dialog), so the two flows cannot drift:

```dart
/// The change-password fields. [requiresCurrentPassword] adds the "Current password" field for the
/// voluntary flow; the forced first-login flow omits it (the caller just signed in with it).
class ChangePasswordForm extends ConsumerStatefulWidget {
  const ChangePasswordForm({
    super.key,
    required this.requiresCurrentPassword,
    required this.onSaved,
    this.submitLabel = 'Save password',
  });
}
```

- Fields: `Current password` (only when required), `New password` (validator `>= 8`), `Confirm
  password` (must equal the new one) — all three are `PasswordField`s.
- Enter moves focus down the chain and submits from the last field (kept from the current screen,
  including the re-entrancy guard).
- Submits `ref.read(authServiceProvider).changePassword(password: …, currentPassword: …)`; on failure
  shows the backend `detail` (`apiErrorMessage`) — the field-level message for a rejected current
  password — and stays open; on success calls `onSaved()` and `ref.invalidate(meProvider)`.

## 3. Auth surface

- `AuthService.changePassword(String password, {String? currentPassword})` →
  `api.changePassword(ChangePasswordRequest(password: password, currentPassword: currentPassword))`.
- `ChangePasswordScreen` (the **forced** gate at `/change-password`) becomes a thin host:
  `ChangePasswordForm(requiresCurrentPassword: false, …)` with its existing app bar/text and
  "redirects once `/me` shows `mustChangePassword == false`" comment.
- `showChangePasswordDialog(BuildContext)` (new, `change_password_dialog.dart`): an `AlertDialog`
  titled "Change password" hosting the same form with `requiresCurrentPassword: true` and
  `submitLabel: 'Change password'`; returns when the form saved and shows a success snack bar. No route,
  no router change.

## 4. Console pane entry (`admin_shell.dart`)

- `_AdminMenu` gains a "Change password" `ListTile` (icon `Icons.password_outlined`) between the
  sections' `Spacer()` and the "Sign out" entry; it calls `showChangePasswordDialog(context)` and, in
  the drawer, closes the drawer first (`onSelected`).
- No permission gate: every authenticated caller may change *their own* password.

## 5. `features/admin/user_editor.dart` — reset another user's password

- `UserList`'s `_UserTile` trailing row gains a key `IconButton` (`Icons.key_outlined`, tooltip
  "Reset password") ahead of edit/delete, shown under the same `canManage` flag.
- `showResetPasswordDialog(context, user)` (new, in the same file): an `AlertDialog` "Reset password"
  with the target's `username`/`email` read-only (reusing `_ReadOnlyField`), one `PasswordField`
  labelled "Temporary password" (`helperText: 'The user must change it on next login'`), and
  "Cancel"/"Reset" actions.
- On confirm: `ref.read(apiClientProvider).resetUserPassword(user.id, ResetPasswordRequest(
  temporaryPassword: …))`, then `ref.invalidate(usersProvider)` (the row now shows
  "must change password") and a success snack bar; failures show the backend `detail` and keep the
  dialog open. The dialog never receives or displays the password again (the API returns `204`).
- The existing create/edit dialog's "Temporary password" field becomes a `PasswordField`.

## 6. `core/api_client.dart` + `models/requests.dart`

```dart
/// `PUT /api/v1/users/{id}/password` — sets another user's temporary password (`204`, no body).
Future<void> resetUserPassword(String id, ResetPasswordRequest request) async {
  await dio.put<void>('/api/v1/users/$id/password', data: request.toJson());
}
```

- New `@freezed` `ResetPasswordRequest({required String temporaryPassword})` with a generated
  `fromJson`/`toJson` (`build_runner` run committed, like the other request models).
- `ChangePasswordRequest` gains `String? currentPassword` (nullable, so the forced flow's body is
  unchanged) — this is the mirror of the backend record (`docs/CODING_GUIDELINES_FRONTEND.md` §14).

## 7. Where each password field gets the toggle

| Screen / dialog | File | Field |
| --- | --- | --- |
| Login | `features/auth/login_screen.dart` | Password |
| Forced first login | `features/auth/change_password_screen.dart` (via `ChangePasswordForm`) | New, Confirm |
| Voluntary change | `features/auth/change_password_dialog.dart` (via `ChangePasswordForm`) | Current, New, Confirm |
| Create/edit user | `features/admin/user_editor.dart` | Temporary password |
| Reset another user | `features/admin/user_editor.dart` | Temporary password |

## 8. Test-double impact

`AuthService.changePassword` gains an optional named parameter, so the `_RecordingAuthService` double in
`test/auth_screens_test.dart` is updated to record `(currentPassword, password)` pairs. No other
existing test changes: the forced screen still sends `{password}` only, the login screen keeps its two
`TextFormField`s (the toggle is a suffix button), and `AdminShell`/`UserList` tests keep their finders
(the new pane entry and the new row action are additive; tests that iterate `AdminSection.values` do not
see them because the entry is not an `AdminSection`).

## Result — executed (2026-09-27)

Artifacts, all in `platform/keystone-admin-ui`:

- **New** `lib/src/core/password_field.dart` — `PasswordField` (one `TextFormField` + suffix toggle,
  tooltips "Show password"/"Hide password"), exported from `keystone_admin_ui.dart`.
- **New** `lib/src/features/auth/change_password_form.dart` — the shared form
  (`requiresCurrentPassword`, `submitLabel`, `onSaved`), keeping Enter-to-advance/submit and the
  re-entrancy guard; success invalidates `meProvider`, failure shows the backend `detail` and keeps the
  host open.
- **New** `lib/src/features/auth/change_password_dialog.dart` — `showChangePasswordDialog(context)`
  (voluntary flow, asks for the current password, `Cancel` action, success snack bar).
- `change_password_screen.dart` is now a thin host of the shared form (`requiresCurrentPassword: false`)
  and no longer a `ConsumerStatefulWidget`.
- `admin_shell.dart` — pane entry "Change password" above "Sign out" plus, in the no-readable-section
  branch, an AppBar action with the same tooltip (otherwise a platform user with no sections could not
  reach it at all — an addition to the plan, consistent with "changing your own password is not a
  permission"); the drawer is closed before the dialog opens.
- `user_editor.dart` — "Reset password" key action on every manageable row, `showResetPasswordDialog`
  (`PUT /users/{id}/password`, read-only target, temporary-password field, invalidates `usersProvider`),
  and the create dialog's temporary-password field is now a `PasswordField`.
- `api_client.dart` — `resetUserPassword(id, request)`; `models/requests.dart` —
  `ChangePasswordRequest.currentPassword` (`@JsonKey(includeIfNull: false)`, so the forced body is
  byte-identical) and the new `ResetPasswordRequest`; `build_runner` regenerated the committed
  `requests.freezed.dart`/`requests.g.dart`.
- `auth_service.dart` — `changePassword(password, {currentPassword})`.

Verification: `flutter analyze` clean, `dart format` clean, `flutter test` **53/53** (was 36);
`apps/inventory/frontend` `flutter analyze` clean (that app has no `test/` directory), so no host-router
change was needed.


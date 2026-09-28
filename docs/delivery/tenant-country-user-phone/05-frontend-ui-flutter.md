# Phase 5 — Frontend / UI (Flutter)

## Scope

The console (`platform/keystone-admin-ui`) learns both fields: the models/requests, the generated
`freezed`/`json_serializable` code, and the two dialogs that edit a tenant or a user.

## Artifacts

| Artifact | Change |
| --- | --- |
| `lib/src/models/models.dart` | `Tenant` gains `String? country`; `User` gains `String? phoneNumber` |
| `lib/src/models/models.freezed.dart`, `models.g.dart` | regenerated for the two classes |
| `lib/src/models/requests.dart` | `CreateTenantRequest` / `UpdateTenantRequest` gain `String? country`; `CreateUserRequest` / `UpdateUserRequest` gain `String? phoneNumber` |
| `lib/src/models/requests.freezed.dart`, `requests.g.dart` | regenerated for the four classes |
| `lib/src/features/admin/tenants_screen.dart` | `Country` field in the create/edit dialog; the code in the row subtitle |
| `lib/src/features/admin/user_editor.dart` | `Phone number` field on the create and edit forms; shown in the row subtitle |
| `test/models_test.dart`, `test/user_editor_test.dart` | cases for the new fields |

## Design notes

- **Nullable, not defaulted.** `country` / `phoneNumber` use `String?` (like `Tenant.tenantId` and
  `Role.tenantId`) rather than `@Default('')`, because `null` is a meaningful state ("not recorded")
  and the dialog must be able to tell it apart from an empty string. Omitting a `null` field on the
  wire leaves the server's own "clear it" semantics in charge, which is what the backend does with an
  absent value.
- **Free-text fields with a shape check, not a hard-coded picker.** A country dropdown would mean
  committing a hand-copied ~250-entry ISO list to the client (and adding a package for one, which the
  offline environment cannot `pub get`), so the dialog uses a two-letter text field whose only client
  rule is "two letters" and upper-cases the value. The authority on *which* code is valid is the
  server, which answers `422` and is surfaced by the existing `showApiError` path. The phone field is
  the same: a shape hint, and the server's E.164 rule decides.
- **`JsonKey(includeIfNull: false)` is *not* used.** The server treats absent and `null` identically,
  so always sending the field keeps the generated code and the payload shape predictable.
- **The tenant dialog stops popping a `(String, String)` tuple** and pops a `(String, String, String?)`
  record instead, so the caller renders the same single "save" path for create and edit.
- **No new Riverpod provider or `dio` method.** `createTenant`/`updateTenant`/`createUser`/`updateUser`
  already take the request objects, whose `toJson()` now carries the new fields.

## Dart toolchain

The Flutter/Dart SDK lives at `~/manual-install/flutter` and is not on `PATH` by default; the generated
`*.freezed.dart` / `*.g.dart` files are committed, so they are **regenerated** with

```
export PATH="$HOME/manual-install/flutter/bin:$PATH"
cd platform/keystone-admin-ui
dart run build_runner build
```

`build_runner` wrote 6 outputs and the diff was scoped to exactly the six new components (verified with
`git diff --stat`: `models.freezed.dart` +86/−…, `requests.freezed.dart` +168/−…, and the two `.g.dart`
files) — **no version churn**, because the cached `freezed` 4.x / `json_serializable` 6.x are the
versions the committed files were produced with. Hand-editing was therefore not needed.

## Dependencies

- Phase 4 (the wire field names).

## Verification

- `flutter analyze` → **No issues found!**
- `flutter test` → **66 tests, all passed** (the 6 new cases plus the updated payload assertions, which
  is what proves the `freezed` + `json_serializable` wiring is right).
- `cd apps/inventory/frontend && flutter analyze` → **No issues found!** (the host app consumes the
  changed package).


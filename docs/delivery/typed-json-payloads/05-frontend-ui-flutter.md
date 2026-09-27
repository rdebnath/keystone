# Phase 5 — Frontend / UI (Flutter)

**Scope** — Remove the inline `Map` request bodies from the admin UI's REST client so every POST
body is a typed model.

**Artifacts**
- `platform/keystone-admin-ui/lib/src/models/requests.dart` (new) — `LoginRequest`,
  `ChangePasswordRequest`, `CreateTenantRequest`, `CreateRoleRequest`,
  `CreatePermissionRequest`, `CreateUserRequest`, each with a named-parameter constructor,
  `fromJson`, and `toJson`.
- `…/lib/keystone_admin_ui.dart` (modify) — export the new models.
- `…/lib/src/core/api_client.dart` (modify) — `login(LoginRequest)`,
  `changePassword(ChangePasswordRequest)`, `createTenant/CreateRole/CreatePermission/CreateUser`
  take their request model and post `request.toJson()`.
- `…/lib/src/features/auth/auth_service.dart` (modify) — builds `LoginRequest` /
  `ChangePasswordRequest`.
- `…/lib/src/features/admin/{tenants,roles,permissions,users}_screen.dart` (modify) — construct the
  named-argument request model at the 4 create call sites.

**Dependencies** — Phase 4 (the field names mirror the backend request records).

**Verification** — no `data: {` map body remains anywhere in the client; every remaining
`Map<String, dynamic>` is a `fromJson`/`toJson` signature or the single `dio` response generic.
`flutter analyze` (clean), `flutter test` (7/7) and `dart format` (on every file authored here) pass
with Flutter 3.47.5 / Dart 3.13.4.

## Notes

- **The `freezed` + `json_serializable` migration is complete** (it was deferred in the first pass
  only because no Dart/Flutter SDK was available then). Added `freezed_annotation`/`json_annotation`
  as dependencies and `freezed`/`json_serializable`/`build_runner` as dev-dependencies; converted
  `models.dart` + `requests.dart` to `@freezed` classes; `dart run build_runner build
  --delete-conflicting-outputs` generated `models.freezed.dart`, `models.g.dart`,
  `requests.freezed.dart`, `requests.g.dart` (committed — generated files are not gitignored).
- **`Session` moved** from `core/api_client.dart` into `models/models.dart` so every response model
  is a `freezed` class in one place; it is still exported from the package barrel, so consumers are
  unaffected.
- **`environment.sdk` raised to `>=3.8.0`** in the package's `pubspec.yaml`: `json_serializable`
  6.14.1 refuses to run otherwise (`The language version (3.4.0) … does not match the required range
  ^3.8.0`), and the toolchain present is Dart 3.13.4. Side effect to be aware of: Dart switches the
  formatter to the new "tall" style from language version 3.7, so `dart format` now wants to
  reformat the package (see the delivery follow-ups).
- Defaulted wire fields use `@Default(...)`, which freezed turns into `@JsonKey(defaultValue: …)` —
  confirmed in the generated `_$MeFromJson` (`mustChangePassword ?? false`,
  `permissions … ?? const <String>[]`) and `_$SessionFromJson` (`tokenType ?? 'bearer'`,
  `(json['expiresIn'] as num?)?.toInt() ?? 0`), reproducing the previous hand-written fallbacks.
- `Map<String, dynamic>` remains in exactly the two places the guideline permits: generated-style
  `fromJson`/`toJson` signatures, and `dio`'s decoded response passed straight into `fromJson` in
  the same statement (`api_client.dart`).
- `AuthService.signIn`/`changePassword` keep plain `String` parameters — they take raw user input
  and build the request model themselves, so no screen constructs a payload.
- `createUser` previously took 5 positional arguments (2 of them nullable strings); the request
  model now names every field, which also removes the argument-order hazard.

## Results

- `requests.dart` added (6 `freezed` models) and exported; `models.dart` converted (6 `freezed`
  models incl. `Session`); `ApiClient` and the 5 call sites updated; `grep 'data: {'` over
  `platform/keystone-admin-ui/lib` and `apps/inventory/frontend/lib` returns nothing.
- Generated code committed: `models.freezed.dart`, `models.g.dart`, `requests.freezed.dart`,
  `requests.g.dart`.
- Verified: `flutter analyze` → "No issues found" for both the package and `apps/inventory/frontend`;
  `flutter test` → 7/7 pass; `dart format` clean on every file authored or rewritten here.
- The package is `dart format` clean: a follow-up mechanical pass normalised the 10 pre-existing
  unformatted files, with `flutter analyze` clean and 7/7 tests passing afterwards.

# Phase 5 — Frontend / UI (Flutter)

**Scope** — make the two levels visible and selectable in the admin console. The REST contract does
not change, so `ApiClient` and the request models stay as they are.

**Artifacts**

| File | Change |
| --- | --- |
| `platform/keystone-admin-ui/lib/src/models/models.dart` | `Permission` gains `const Permission._();` + a derived `access` getter returning the new `PermissionAccess` enum, parsed from the code suffix (`:read-write` → `readWrite`, `:read-only` → `readOnly`) — same pattern as `Me.isPlatformAdmin` |
| `platform/keystone-admin-ui/lib/src/features/admin/permissions_screen.dart` | create dialog: replace the free-text **Code** field with a **Resource** dropdown (the 7 catalog resources, each showing its scope) + a **Type** dropdown (`Read-only` / `Read/write`), showing the composed code; list tile subtitle becomes `PLATFORM · read/write` |
| `platform/keystone-admin-ui/lib/src/features/admin/roles_screen.dart` | permissions field hint documents the two code shapes (`platform:tenant:read-only, platform:role:read-write`); no functional change — the role form keeps taking catalog codes |
| `platform/keystone-admin-ui/test/models_test.dart` | new cases for the derived access (`:read-only` → readOnly, `:read-write` → readWrite) |
| `platform/keystone-admin-ui/test/dialogs_test.dart` | unchanged (its `splitList` cases still apply) |

**Not changed:** `lib/src/models/requests.dart` (`CreatePermissionRequest(code, scope)` and
`CreateRoleRequest(code, scope, permissions)` keep their shape), `lib/src/core/api_client.dart`,
`lib/src/core/providers.dart`, the barrel `keystone_admin_ui.dart`.

**Dependencies** — Phases 3–4 (catalog code format is final); the frontend consumes whatever codes
the seeded catalog exposes.

**Verification** — `cd platform/keystone-admin-ui && flutter analyze` clean and `flutter test` green
(models test extended); manual check: create `platform:tenant:read-only` and
`platform:tenant:read-write` permissions through the dialog and confirm both render with the right
level label, then build a role with one of each.

## Design details

```dart
/// The two access levels a platform permission can have (`docs/ARCHITECTURE.md` §9.3).
enum PermissionAccess {
  readOnly('read-only'),
  readWrite('read/write');

  const PermissionAccess(this.label);
  final String label;
}
```

- The **Resource** dropdown is static UI data (the 7 catalog resources), not a server call: the
  seeded catalog is platform-defined, and the *scope* is derived from the resource namespace prefix
  (`platform:` → `PLATFORM`, `tenant:` → `TENANT`), so the dialog cannot create the invalid
  `scope` + `code` combinations that `RoleService`/`PermissionService` would reject.
- Composing the code client-side keeps the backend contract unchanged; the backend still validates
  the suffix (Phase 3, decision 7), so a hand-crafted request cannot introduce a third level.
- Optional (out of scope unless requested): decorators that hide write actions for callers whose
  `/me` permissions lack `:read-write` for the resource. The backend remains the only security
  boundary (`docs/ARCHITECTURE.md` §9.6).

## Execution record (2026-09-27)

- `PermissionAccess` (`readOnly('read-only')` / `readWrite('read-write')` with `suffix` + `label`,
  `fromCode`) added to `models.dart`; `Permission.access` is the derived (nullable) level.
- `permissions_screen.dart`: the dialog is now Resource + Type dropdowns with a live `Code:` preview
  and the scope derived from the resource; the list shows `scope · level`. `roles_screen.dart`: hint
  text shows the two code shapes.
- `dart run build_runner build` regenerated `models.freezed.dart` — required, otherwise
  `flutter analyze` fails with "Missing concrete implementation of 'getter Permission.access'".
- Verification: `dart format lib test` (2 files reformatted), `flutter analyze` → **No issues
  found!**, `flutter test` → **9/9 passed** (2 new `Permission` cases in `models_test.dart`).
- Environment note: the Flutter SDK is not on `PATH` here; it was used from
  `/Users/rajeshdebnath/manual-install/flutter/bin` (Flutter 3.47.5 / Dart 3.13.4).

## Follow-up (2026-09-27)

- The wildcard row rendered as "unknown level" (it carries no `:level` suffix); it now reads as
  read/write, because `*` grants everything. Implemented as `Permission.wildcard` plus a branch in
  `Permission.access`, so the level is decided in the model and not in the screen.
- Confirmed with the user that the seeding is unchanged: the catalog keeps both levels per resource
  (14 rows + `*`) and `platform-admin` keeps the `*` grant — read-only must stay grantable to other
  roles, so the read-only rows cannot be dropped.
- Verification: `dart format` (1 file), `flutter analyze` clean, `flutter test` **10/10** (a
  wildcard case was added to `models_test.dart`).

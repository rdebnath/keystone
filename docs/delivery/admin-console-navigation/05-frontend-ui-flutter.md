# Phase 5 — Frontend / UI (Flutter)

**Scope** — replace the tab shell with a permission-driven, hideable left pane; add the
tenant→users drill-down; expose tenant add/edit/delete and user add/edit/delete; keep the models
typed and the REST layer the only place that talks to `dio`.

**Artifacts**

| File | Change |
| --- | --- |
| `lib/src/core/permissions.dart` **(new)** | `PlatformResource` codes (`platform:tenant`, `platform:role`, `platform:permission`, `platform:user`), `CatalogResource(code, scope)` + the shared `catalogResources` const list (moved out of `permissions_screen.dart`), and a `readCode(resource)` / `writeCode(resource)` composer |
| `lib/src/models/models.dart` | `Me`: `username` (`@Default('')`), `allows(code)`, `allowsResource(resource)`, `canWrite(resource)`; `Tenant`: `platform` (`@Default(false)`) |
| `lib/src/models/requests.dart` | `UpdateTenantRequest(name, slug)`, `UpdateUserRequest(username, roles)` |
| `lib/src/core/api_client.dart` | `tenants()` unchanged; `updateTenant(id, request)`, `deleteTenant(id)`; `users({String? tenantId})`; `updateUser(id, request)` (PATCH); `createUser`/`createTenant` unchanged |
| `lib/src/core/providers.dart` | `usersProvider` becomes `FutureProvider.family<List<User>, String?>` (null = all users, reserved id = platform users); `tenantByIdProvider(id)` derived from `tenantsProvider` |
| `lib/src/core/errors.dart` **(new)** | `apiErrorMessage(Object error, String fallback)` — reads the RFC 9457 `detail` from a `DioException` response, so "Cannot delete tenant with users" reaches the user |
| `lib/src/core/dialogs.dart` | add `confirmDialog(context, title, message, confirmLabel)` (destructive confirm) beside the existing `splitList`/`promptText` |
| `lib/src/features/admin/admin_shell.dart` **(new)** | `AdminShell`, `AdminSection` (path/label/icon/read-resource), `AdminRoutes` constants, the collapsible pane, the not-authorized placeholder, header + sign out |
| `lib/src/features/admin/dashboard_screen.dart` | **deleted** — superseded by `AdminShell` (unreleased platform) |
| `lib/src/features/admin/tenants_screen.dart` | platform row + tenant list, row menu (Edit/Delete), FAB (Add), tap → drill-down; all writes gated by `canWrite(platform:tenant)` |
| `lib/src/features/admin/tenant_users_screen.dart` **(new)** | tenant header (name, slug, "platform plane" hint) + that tenant's users with Add/Edit/Delete |
| `lib/src/features/admin/user_editor.dart` **(new)** | shared create/edit user dialog + the user row widget used by both user screens |
| `lib/src/features/admin/users_screen.dart` | all users with a tenant filter dropdown, reusing the shared row + editor |
| `lib/src/features/admin/permissions_screen.dart` | uses the shared `catalogResources` (no behaviour change) |
| `lib/keystone_admin_ui.dart` | export `admin_shell.dart`, `tenant_users_screen.dart`, `user_editor.dart`, `core/errors.dart`, `core/permissions.dart`; drop the `dashboard_screen.dart` export |
| `apps/inventory/frontend/lib/router.dart` | `ShellRoute` at `/` → `AdminShell`; sub-routes `/tenants`, `/tenants/:tenantId`, `/users`, `/roles`, `/permissions`; redirect to the first permitted section after login |

**Shell**

```dart
final sections = AdminSection.values
        .where((s) => me.allowsResource(s.resource))
        .toList(growable: false);
```

- Wide (`LayoutBuilder` width >= 800): the pane is an `AnimatedContainer` whose width animates
  264 → 0 (`AnimatedSize`/`ClipRect` around the column), toggled by the AppBar hamburger with the
  tooltip `Hide menu` / `Show menu`.
- Narrow: the same `_AdminMenu` widget is the `Scaffold.drawer` body; the hamburger opens it and a
  selection closes it via `Navigator.pop`.
- Selection comes from `AdminShell(location: state.matchedLocation, child: child)`:
  `AdminSection.forLocation(location)` matches by path prefix, so `/tenants/<id>` keeps **Tenants**
  highlighted.
- If the caller cannot read the section in the location, the body renders a `_NotAuthorized` panel
  (no API call). If the caller can read nothing at all, the shell renders a single explanatory panel.
- The AppBar title is the current section's label; the pane header shows the branding title, the
  signed-in username and the plane (`Platform` when `me.isPlatformAdmin`); the pane footer signs out
  (`authService.signOut()` + the existing `signedInProvider`/`meProvider` reset).

**Tenants screen**

- Rows: leading avatar (`Icons.shield` + tooltip for the platform row, the name's initial otherwise),
  title = name, subtitle = `slug · platform users` for the platform row and `slug · <id>` otherwise.
- `Keystone` (`tenant.platform`) has **no** row menu; every other row has `Edit` / `Delete` when the
  caller holds `platform:tenant:read-write`.
- Add (`FloatingActionButton.extended`) and the edit dialog share one `_TenantFormDialog` (name +
  slug, hint `lowercase-id (used in username@slug)`, slug normalised on the way out).
- Delete asks for confirmation, names the tenant, and surfaces the backend `detail` on failure (a
  tenant with users answers `409`).
- Tapping a row `context.go(AdminRoutes.tenantUsers(tenant.id))`.

**Tenant users screen** (`/tenants/:tenantId`)

- Resolves the tenant from `tenantByIdProvider(tenantId)` (loading spinner while `tenantsProvider`
  resolves; a "tenant not found" panel when the id is unknown).
- Header: name, slug, and for the platform row an explanatory line ("Platform users — no tenant").
- Body: `usersProvider(tenantId)` rendered by the shared `UserList`; Add user passes the tenant as
  fixed context so the tenant is not chosen twice.

**Users screen** (`/users`)

- Tenant filter (`DropdownButton<String?>`, items `All tenants` + the tenant list including
  `Keystone`) → `usersProvider(selected)`; the selection defaults to `All`.
- Same `UserList`, with the tenant column visible in the subtitle (`username · slug`).

**Shared user editor** (`user_editor.dart`)

```dart
UserEditor(
  tenant: tenant,          // fixed on the tenant screen; a dropdown on the all-users screen
  user: existing,          // null = create
  roles: rolesForPlane,    // PLATFORM roles for platform users, TENANT roles otherwise
)
```

| Field | Create | Edit |
| --- | --- | --- |
| Username | `TextFormField` (required) | same, pre-filled |
| Tenant | dropdown (Keystone → platform) or fixed context | read-only text |
| Email | optional `TextFormField` (hint `blank = username@<slug>.com`) | read-only (the GoTrue identity) |
| Temporary password | required, obscured | hidden |
| Roles | `CheckboxListTile` per role of the plane, subtitle = the role's permission codes | same, pre-checked |

- Create → `POST /api/v1/users` with `tenantId: tenant.platform ? null : tenant.id`;
  edit → `PATCH /api/v1/users/{id}` with `UpdateUserRequest(username, checkedRoles)` (one call).
- Both screens refresh with `ref.invalidate(usersProvider)` (invalidates the whole family).
- Role list comes from `rolesProvider` filtered by `scope` (`PLATFORM` / `TENANT`), mirroring the
  backend rule; when the roles are still loading the dialog shows a spinner instead of an empty list.

**Host router** (`apps/inventory/frontend/lib/router.dart`)

```dart
if (!me.mustChangePassword && (atLogin || atChangePassword)) {
    return me.isPlatformAdmin ? AdminRoutes.firstAllowed(me) : '/inventory';
}
...
ShellRoute(
  builder: (_, state, child) => AdminShell(location: state.matchedLocation, child: child),
  routes: [
    GoRoute(path: AdminRoutes.tenants, builder: (_, __) => const TenantsScreen()),
    GoRoute(path: AdminRoutes.tenantUsersPattern, builder: (_, state) =>
        TenantUsersScreen(tenantId: state.pathParameters['tenantId']!)),
    GoRoute(path: AdminRoutes.users, builder: (_, __) => const UsersScreen()),
    GoRoute(path: AdminRoutes.roles, builder: (_, __) => const RolesScreen()),
    GoRoute(path: AdminRoutes.permissions, builder: (_, __) => const PermissionsScreen()),
  ],
),
```

- `AdminRoutes.firstAllowed(me)` returns the first section the caller can read, so a `platform:user`
  -only admin lands on `/users` instead of a section they cannot open.
- The auth gate, the first-login password gate and the tenant-user route (`/inventory`) keep their
  current behaviour.

**Not changed:** `auth_service.dart`, `login_screen.dart`, `change_password_screen.dart`,
`token_storage.dart`, `branding.dart`, `env.dart`, `log.dart`, `roles_screen.dart`.

**Dependencies** — Phases 3–4 (the platform row, `platform` field, `GET /users?tenantId=`,
`PATCH /users/{id}`, `Me.username`).

**Verification** — `cd platform/keystone-admin-ui && dart format lib test`,
`dart run build_runner build --delete-conflicting-outputs` (freezed/json output is committed),
`flutter analyze` clean, `flutter test` green; manual: sign in as `admin@keystone`, toggle the pane,
drill into `Keystone` and a tenant, then add/edit/delete a tenant and edit a user's roles.

## Execution record (2026-09-27)

- **New:** `core/permissions.dart` (`PlatformResource` codes, `CatalogResource` + `catalogResources`,
  moved out of `permissions_screen.dart`), `core/errors.dart` (`apiErrorMessage` reading the RFC 9457
  `detail` from a `DioException`, `showApiError`, `showApiSuccess`), `core/panels.dart`
  (`MessagePanel` for empty/error/not-authorized states),
  `features/admin/admin_shell.dart` (`AdminShell`, `AdminSection`, `AdminRoutes`),
  `features/admin/tenant_users_screen.dart`, `features/admin/user_editor.dart`
  (`UserList`, `showUserEditor`, the shared dialog with the plane-filtered role checklist).
- **Rewritten:** `tenants_screen.dart` (platform row, row menu Edit/Delete, Add tenant, tap → drill-down,
  confirm + problem-detail errors) and `users_screen.dart` (tenant filter + shared `UserList` +
  Add user). `permissions_screen.dart` now uses the shared catalog and the shared error/empty panels.
  The barrel exports the shell, the screens and the new core files. `dashboard_screen.dart` **deleted**.
- **Models/requests/API/providers:** `Me.username` + `allows`/`allowsResource`/`canWrite`;
  `Tenant.platform`/`isPlatform`/`userPlane`; `Role.isPlatformScope`; `UpdateTenantRequest`,
  `UpdateUserRequest`; `ApiClient.updateTenant`/`deleteTenant`/`users({tenantId})`/`updateUser`/
  `deleteUser`; `usersProvider` is now a `family<List<User>, String?>` plus `tenantByIdProvider`.
- **Host:** `apps/inventory/frontend/lib/router.dart` — `ShellRoute` over the console sections, with the
  post-login landing on `AdminRoutes.firstAllowed(me)`; the tenant-user route is unchanged.
- **Deviations from the plan (all recorded here):**
  1. `go_router: ^14.6.2` added to `platform/keystone-admin-ui/pubspec.yaml` — the package had no router
     dependency, and the shell navigates (`context.go`) exactly as the frontend guidelines prescribe.
  2. `rolesForTenantProvider` was dropped after drafting: the role checklist filters `rolesProvider` by
     `Role.isPlatformScope` instead, so no provider exists that nothing uses.
  3. `AdminShell.paneKey` is public so the toggle can be asserted by size.
  4. `build_runner` in this version rejects/ignores `--delete-conflicting-outputs`; plain
     `dart run build_runner build` is the command now.
- **Defect found and fixed by the new widget test:** the pane originally carried its background as
  `AnimatedContainer(color: …)`, which put a `DecoratedBox` between the `ListTile`s and their `Material`
  ("ListTile background color or ink splashes may be invisible"). The pane now wraps its content in a
  `Material` inside the animated box.
- **Verification:** `dart format lib test` (6 files reformatted) and `flutter analyze` →
  **No issues found!** in both `platform/keystone-admin-ui` and `apps/inventory/frontend`;
  `flutter test` → **25/25 passed** (new `admin_shell_test.dart`: section filtering per permission,
  pane hide/show by width, nested-location highlight, unreadable deep link, no-readable-section panel,
  `AdminRoutes` helpers). `flutter pub get` resolved the new dependency in both packages.
- Environment note: the Flutter SDK is not on `PATH`; used
  `/Users/rajeshdebnath/manual-install/flutter/bin` (Flutter 3.47.5 / Dart 3.13.4).
- Not yet done here: the model/editor unit tests and the tenant/users screen widget tests are Phase 7,
  and no manual click-through against a live backend has been performed (no dev credentials).

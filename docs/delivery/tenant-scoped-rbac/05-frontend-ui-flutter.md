# Phase 5 — Frontend / UI (Flutter)

**Scope** — surface the owner in `keystone-admin-ui` (the **platform** console): filter the
roles/permissions lists by tenant, show which rows are global vs. tenant-owned, create with an owner,
and keep the user role picker to roles assignable in that user's plane.

**The tenant console is in this phase (option b — confirmed).** `keystone-admin-ui` is a library the
hosting app already consumes via a `path:` dependency, so the tenant console is built **inside the
same library**: the screens are shared, and only the client they talk to differs. No screen is
duplicated.

## Execution note (2026-09-27) — DONE

Flutter 3.47.5 was found (`flutter`/`dart` live on the **login shell's** PATH only, not a bare exec
shell), so this phase was executed as designed — with **one class instead of two**: `ConsoleScope` is a
single object parameterised by the plane (`ConsoleScope.platform` / `.tenant`) rather than two
implementations, which achieves the same "one implementation per screen" with less code. Deviations and
additions:

- `ApiClient` gained a `basePath` (`/api/v1` or `/api/v1/tenant`), so one client serves both planes and
  `ConsoleScope` is a thin, testable policy layer over it: it drops any `tenantId` a caller passes on the
  tenant plane and forces the owner to null on a tenant write.
- `providers.dart`: `dioProvider` + `apiClientProvider` + `tenantApiClientProvider` + `consoleProvider`;
  `rolesProvider` / `permissionsProvider` / `usersProvider` are families keyed by the owner filter and
  read through the console — so **every existing screen became plane-aware without a screen being
  duplicated**. `AdminShell` takes a `sections` list (default: the platform set) and `AdminSection` became
  a class with `values` / `tenantValues`, so the tenant console is the same shell with the tenant sections.
- `OwnerFilter` (new, shared by the roles and permissions screens) replaces two ad-hoc selectors.
- Screens: the owner is shown per row and `OwnerFilter` renders only on the platform plane; the create
  dialogs drop the owner/scope choice on the tenant plane; `UsersScreen` / `UserList` / `UserEditor` hide
  the tenant field and read the tenant list only where it exists.
- Hosting app: `router.dart` adds the tenant console `ShellRoute` (`/tenant/users|roles|permissions`) and
  routes a tenant user holding a tenant console permission into it, leaving an ordinary tenant user in
  `/inventory`.

**Verification** — `flutter analyze` clean and `flutter test` **59 passed** in
`platform/keystone-admin-ui`: the 6 new ones are 4 in `console_test.dart` (the tenant plane uses the
tenant routes, never sends a tenant parameter, and never sends an owner on a write) and 2 in
`models_test.dart` (row ownership, absent owner = global). `flutter analyze` is also clean in
`apps/inventory/frontend` — that package has no `test/` directory, so `flutter test` there reports
"Test directory not found" (pre-existing, unchanged).

## Console abstraction — where the de-duplication happens

The existing screens call `apiClientProvider` directly. Introduce the seam they depend on instead:

- `lib/src/core/console.dart` (new): `ConsoleScope` — the surface the screens use (`users()`,
  `createUser()`, `updateUser()`, `deleteUser()`, `assignRoles()`, `resetPassword()`, `roles()`,
  `createRole()`, `updateRole()`, `deleteRole()`, `permissions()`, `createPermission()`,
  `deletePermission()`), plus `bool get isPlatformPlane` and `String? get tenantId`.
- Two implementations over the existing `ApiClient`: `PlatformConsole` (`/api/v1/…`, the `?tenantId=`
  filter, `Me.isPlatformAdmin`) and `TenantConsole` (`/api/v1/tenant/…`, no tenant filter,
  `Me.tenantId != null`).
- `consoleProvider` returns the one matching the signed-in `Me`. Each screen watches it instead of
  `apiClientProvider`, so a screen keeps exactly **one** implementation and both consoles get it.

## Shell, sections and routing

- `AdminShell` / `AdminSection` stop hardcoding `PlatformResource`: the shell takes the **section set**
  as a parameter — the platform set (tenants, users, roles, permissions) and the tenant set (users,
  roles, permissions — **no tenants**). Add `TenantResource` (`tenant:user`, `tenant:role`,
  `tenant:permission`) beside `PlatformResource`, and `TenantRoutes`
  (`/tenant/users`, `/tenant/roles`, `/tenant/permissions`) beside `AdminRoutes`, with
  `firstAllowed(me, sections)` picking the landing route.
- On the tenant plane the tenant **selector disappears**: the owner picker is gone from the role and
  permission dialogs (the owner is always the caller's tenant), the user create form drops its tenant
  field, and `user_editor` hides it too. The role picker keeps only assignable roles, which the
  tenant-plane list already returns.
- Hosting app (`apps/inventory/frontend/lib/router.dart`): route a tenant user into the tenant console
  when they hold any `tenant:user|role|permission:<level>`; otherwise `/inventory` exactly as today, so
  an ordinary inventory user is unaffected. The `ShellRoute` builder picks the section set from the
  same `consoleProvider`, so the menu and the routes cannot drift.

**Artifacts**

| File | Change |
| --- | --- |
| `lib/src/models/models.dart` | `Role` and `Permission` gain `String? tenantId`; a `bool get isGlobal => tenantId == null` helper |
| `lib/src/models/models.freezed.dart`, `models.g.dart` | regenerated (`dart run build_runner build --delete-conflicting-outputs`) |
| `lib/src/models/requests.dart` | `CreateRoleRequest` / `CreatePermissionRequest` gain `String? tenantId` (omitted when null, `@JsonKey(includeIfNull: false)`, matching `ChangePasswordRequest`'s pattern) + regenerated files |
| `lib/src/core/api_client.dart` | `roles({String? tenantId})` / `permissions({String? tenantId})` add `queryParameters` exactly like `users({String? tenantId})` |
| `lib/src/core/console.dart` (new) | `ConsoleScope`, `PlatformConsole`, `TenantConsole`, `consoleProvider` — the seam that lets one screen serve both planes |
| `lib/src/core/providers.dart` | `rolesProvider` / `permissionsProvider` become `FutureProvider.family<List<…>, String?>`, mirroring `usersProvider`; provider set for the tenant plane (or the screens simply watch `consoleProvider`) |
| `lib/src/core/permissions.dart` | `TenantResource` (`tenant:user`, `tenant:role`, `tenant:permission`) beside `PlatformResource`; `catalogResources` unchanged (the catalog itself is global) |
| `lib/src/features/admin/admin_shell.dart` | shell + sections parameterised by the plane's section set; `TenantRoutes`; `firstAllowed(me, sections)` |
| `lib/src/features/admin/roles_screen.dart` | tenant selector in the app bar on the **platform** plane only (Global / a tenant); owner shown in the subtitle; create dialog gains an owner picker on the platform plane and omits it on the tenant plane |
| `lib/src/features/admin/permissions_screen.dart` | same: owner selector + owner shown in the subtitle (alongside scope and level) |
| `lib/src/features/admin/users_screen.dart`, `user_editor.dart` | tenant field hidden on the tenant plane; role picker limited to roles assignable in the plane, so the UI cannot offer a role the backend would reject |
| `lib/src/features/tenant/` (new) | the tenant console entry, if a distinct wrapper is neater than a parameterised `AdminShell` |
| `lib/keystone_admin_ui.dart` | export the new files |
| `apps/inventory/frontend/lib/router.dart` | hosting-app wiring: tenant console routes + the redirect rule for a tenant user holding tenant console permissions (the one app-side file in this phase) |

**Dependencies** — phase 4 (the API filter and the wire field).

**Verification** — `dart format lib test`, `dart run build_runner build --delete-conflicting-outputs`
(freezed/json output is committed), `flutter analyze` clean, `flutter test` green; manual: as the
platform admin create a role owned by a tenant and confirm it is invisible under another tenant; then
sign in as that tenant's admin and run the whole flow — create a user, give it a role, reset its
password — confirming the Tenants section is absent and no request carries a tenant other than the
caller's.

## Notes

- The owner selector follows the existing convention: the reserved platform tenant row from
  `GET /api/v1/tenants` (`platform: true`) means the **platform plane**. For the list filter that
  means "global only"; for the create dialog "no owner" (omit `tenantId`) means the same thing, so the
  dialog can simply map the reserved row to `null`.
- **The tenant console never sends a `tenantId`** — the backend derives it from the caller. Sharing one
  screen across both planes is what makes that structural: there is no place for a tenant admin to name
  another tenant, rather than a filter that could be bypassed.
- Keep the wire field out of the UI logic — models stay the only place field names live.

# Phase 5 — Frontend / UI (Flutter)

**Scope** — extract the platform admin UI into a shared Flutter package and make the
inventory frontend the host shell.

**Artifacts**
- New Flutter package (e.g. `platform/keystone-admin-ui/` or a `packages/` directory)
  containing `LoginScreen`, `AuthProvider`/`AuthService` (backend-proxied), the `/me`
  provider, and the admin screens (dashboard, tenants, users, roles, permissions) plus their
  models and router.
- `apps/inventory/frontend`: a real Flutter app — imports the package, initializes Supabase
  (session/Realtime), and owns the shell `GoRouter` that routes on `/me`:
  `tenantId == null` → admin dashboard (package); otherwise → inventory home.
- Add the inventory home/feature screens (initial scope).
- Remove `apps/platform/frontend`.

**Dependencies** — Phase 4.

**Verification** — `flutter analyze` + `dart format` clean; widget test: login → platform
user lands on admin UI, tenant user lands on inventory UI.

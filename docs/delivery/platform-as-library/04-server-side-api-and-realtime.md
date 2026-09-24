# Phase 4 — Server-side API & realtime

**Scope** — wire the admin library's routes into the inventory server; add the login endpoint.

**Artifacts**
- `apps/inventory/server`: add the `keystone-admin` dependency; bind `PlatformModule` (or a
  combined module) in `InventoryModule`; register the admin `RouteConfigurer`s; run the
  `platform` schema migration + `BootstrapRunner` in `Main` alongside the inventory schema.
- New `AuthHandler` exposing `POST /api/v1/auth/login`.
- Remove `apps/platform/server` (and the `apps/platform` holder) from the root `pom.xml`.

**Dependencies** — Phase 3.

**Verification** — slice test: an embedded server serves `/api/v1/auth/login` and `/me`;
the inventory app starts and bootstraps the first platform user against Testcontainers +
faked Supabase.

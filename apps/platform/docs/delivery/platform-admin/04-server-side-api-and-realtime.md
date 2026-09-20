# Phase 4 — Server-side API

**Scope** — Javalin handlers + auth filter + permission guard. Realtime: N/A (admin console).
**Artifacts** — `AuthFilter`, `PermissionGuard`, `MeHandler`, `TenantHandler`, `RoleHandler`,
`PermissionHandler`, `UserHandler`, `BootstrapRunner`, `Main`, `PlatformModule`.
**Dependencies** — Phase 3.
**Verification** — slice tests (JavalinTest) + integration test.

## Contract mapping

- `AuthFilter` runs `before("/api/*")`: validate bearer token → store `Principal` in ctx.
- `PermissionGuard.require(ctx, "code")` enforces permission (allows `*`).
- `BootstrapRunner` (idempotent, service-role key) creates the Supabase Auth admin + seeds DB.

# Phase 7 — Testing

**Scope** — unit, slice, and integration coverage.
**Artifacts** — `ConfigLoaderTest`, `PermissionResolverTest`, `PlatformIntegrationTest`
(Testcontainers PostgreSQL, fake `TokenAuthenticator` + fake `SupabaseAdminClient`).
**Dependencies** — Phases 2–4.
**Verification** — `mvn -pl apps/platform/server -am test`.

## Notes

- Integration test builds the real Guice `Injector` with `Modules.override(...)` for the two
  external ports (JWT validation + Supabase Admin API) and exercises the REST flow against an
  embedded Javalin server.

## Result

- `ConfigLoaderTest` (4 tests) + `PlatformIntegrationTest` (1 test, Testcontainers PostgreSQL) all
  pass via `mvn -pl apps/platform/server -am test`.
- Integration test covers bootstrap → `GET /me` (wildcard `*`, `mustChangePassword`) → forced
  password change → permission guard `403` → tenant creation.

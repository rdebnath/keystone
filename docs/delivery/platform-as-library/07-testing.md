# Phase 7 — Testing

**Scope** — cover the moved/changed behavior end-to-end.

**Artifacts**
- Backend unit: `TenantResolver`, `LoginService` (fake Supabase client).
- Backend slice: `/auth/login` + `/me` handlers via `JavalinTest` / embedded server.
- Backend integration: real Guice `Injector` + embedded server + Testcontainers PostgreSQL;
  assert first-user bootstrap, platform admin login, and tenant/user creation.
- Frontend: widget tests for shell routing (platform vs. tenant) and the login screen.

**Dependencies** — Phases 2–6.

**Verification** — `mvn clean verify` and `flutter analyze` / `flutter test` green.

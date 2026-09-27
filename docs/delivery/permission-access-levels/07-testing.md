# Phase 7 — Testing

**Scope** — cover the two-level rule at three levels: pure unit tests for the catalog and the guard
decision, an integration test for the HTTP `403`/`200` behaviour and the seeded catalog, and
Flutter model tests for the derived access label.

**Artifacts**

| Test | Type | Covers |
| --- | --- | --- |
| `platform/keystone-admin/src/test/java/…/admin/PermissionCatalogTest.java` **(new)** | unit | exactly two codes per resource and no others; the catalog is exactly the 14 level codes + `*`; codes unique; each code's level matches its `Access`; every code's prefix matches its `scope`; `acceptedCodes(resource, READ_ONLY)` = `[:read-only, :read-write]` and `(…, READ_WRITE)` = `[:read-write]`; `hasAccessLevel(...)` per suffix |
| `platform/keystone-admin/src/test/java/…/admin/auth/PermissionGuardTest.java` **(new)** | unit | the pure `grants(...)` helper: `:read-only` satisfies read, not write; `:read-write` satisfies both; `*` satisfies both; an unrelated resource or an empty set denies; `tenant:user:read-write` does not satisfy `platform:user:read-write` |
| `platform/keystone-admin/src/test/java/…/admin/identity/AccessTest.java` **(new)** | unit | `suffix()` per constant (`read-only`, `read-write`) and `suffixes()` (exactly those two) |
| `platform/keystone-admin/src/test/java/…/admin/AdminIntegrationTest.java` **(extend)** | integration (Testcontainers, `disabledWithoutDocker`) | after `bootstrap()`: `GET /api/v1/permissions` exposes both levels on both planes (`platform:tenant:read-only|read-write`, `tenant:user:read-only|read-write`) |
| `platform/keystone-admin-ui/test/models_test.dart` **(extend)** | Flutter unit | `Permission.access` for `:read-only` / `:read-write` codes |

**Dependencies** — Phases 3–5.

**Verification (commands)**

```
mvn clean verify                                       # backend + integration (Docker for Testcontainers)
mvn -pl platform/keystone-admin -am test               # fast backend loop
cd platform/keystone-admin-ui && flutter analyze && flutter test
```

## Implementation notes

- `PermissionGuardTest` deliberately tests the extracted pure `grants(...)` helper instead of a
  mocked `Context`/resolver: `PermissionResolver` is a `final` class with a `@Platform DataAccess`
  dependency, and the codebase's existing slice tests (`AuthFilterTest`) show the Javalin route
  plumbing is already covered — the new behaviour worth pinning is the code-set decision.
- `AdminIntegrationTest` needs a second caller: replace the fixed `TokenAuthenticator` lambda with
  one that maps the bearer token to a `sub` (`test-token` → `ADMIN_SUB` for the existing
  assertions, a second token → the read/write and read-only users created through the API), then
  re-request the same routes per level. The users are provisioned through `POST /api/v1/users`
  against the existing fake `SupabaseAdminClient`.
- The integration test seeds nothing but the bootstrap: it asserts the catalog over HTTP with the
  admin token, so no second caller (and no role plumbing) is needed.
- If Docker is unavailable, the integration test skips (existing `@Testcontainers(disabledWithoutDocker
  = true)`) — the unit tests above are the always-on gate.
- No Liquibase/codegen tests are affected (Phase 2 skipped): the generated jOOQ sources are byte-for-byte
  unchanged, which the plain `mvn clean verify` confirms.

## Execution record (2026-09-27)

- Added `PermissionCatalogTest` (6 cases), `PermissionGuardTest` (5 HTTP cases via `JavalinTest` +
  preset context attributes, so it runs without Docker) and `AccessTest` (2 cases); extended
  `AdminIntegrationTest` with the catalog assertion.
- `mvn -pl platform/keystone-admin test` → **45 tests, 0 failures, 0 errors, 1 skipped**
  (`AdminIntegrationTest`, no Docker in this environment — its new assertion awaits a Docker/CI run).
- Frontend: `flutter analyze` clean, `flutter test` **9/9** (see `05-frontend-ui-flutter.md`).
- Deviation from the plan: the per-level HTTP assertions (read-only reads/`403`s, read-write does
  both, `*` does both, empty set denies) live in `PermissionGuardTest` — a runnable slice test —
  rather than only in the Testcontainers integration test, which is skipped here. The integration
  test keeps the catalog-over-HTTP assertion.
- Follow-up (2026-09-27): with the legacy prune removed, `PermissionCatalogTest` pins the exact fresh
  catalog (`containsExactlyInAnyOrder` over the 14 level codes + `*`) instead of asserting that old
  codes are absent, and the integration test asserts both planes' levels instead of absence of the
  replaced codes. Test counts are unchanged (45 backend, 10 frontend).

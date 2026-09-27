# Phase 7 — Testing

**Scope** — pin the new backend rules with unit/slice tests where they can run without Docker, extend
the Testcontainers integration test with the end-to-end flows, and cover the permission-driven menu
and the new models on the Flutter side.

**Artifacts**

| Test | Type | Covers |
| --- | --- | --- |
| `platform/keystone-admin/src/test/java/…/admin/PlatformSchemaTest.java` **(new)** | unit | `PLATFORM_TENANT_ID` is the reserved all-zero UUID, differs from any id `UuidIdGenerator` produces (random v4), `PLATFORM_TENANT_NAME`/`RESERVED_SLUG` are `Keystone`/`keystone`, `isPlatformTenant` matches only the reserved id (null-safe) |
| `…/admin/tenant/TenantServiceTest.java` **(new)** | unit | the pure validators: `validateSlug` rejects the reserved slug (and accepts `keystone-corp`), `requireTenantRow`/the reserved-id guard rejects `PLATFORM_TENANT_ID` — extracted as static helpers so they need no database |
| `…/admin/user/UserServiceTest.java` **(new)** | unit | the pure helpers: `normalized(roles)` de-duplicates, the plane rule (a `PLATFORM` role requires a platform user, a `TENANT` role a tenant user) and the reserved-id → `null` tenant normalisation |
| `…/admin/AdminIntegrationTest.java` **(extend)** | integration (Testcontainers, `disabledWithoutDocker`) | see below |
| `platform/keystone-admin-ui/test/admin_shell_test.dart` **(new)** | Flutter widget | menu filtered by `Me.permissions`: wildcard shows all four sections; `platform:tenant:*` shows only Tenants; a read-only tenant admin sees no Add/Edit/Delete; toggling the hamburger hides and shows the pane |
| `platform/keystone-admin-ui/test/models_test.dart` **(extend)** | Flutter unit | `Me.allows`/`allowsResource`/`canWrite` (read/write implies read, read-only does not imply write, wildcard allows everything); `Me.username`; `Tenant.platform` (defaults to false for a legacy payload) |
| `platform/keystone-admin-ui/test/user_editor_test.dart` **(new)** | Flutter widget | the editor offers only roles of the user's plane, pre-checks the user's roles, and posts a single update request (recording fake API client) |

**Integration test additions** (after the existing bootstrap assertions)

1. `GET /api/v1/tenants` returns the synthetic `Keystone` row first
   (`"platform":true`, reserved id, `"slug":"keystone"`) and the created tenant's row carries
   `"platform":false`.
2. `GET /api/v1/users?tenantId=<reserved id>` returns the bootstrapped platform admin;
   `GET /api/v1/users?tenantId=<acme id>` returns `alice` and not the admin;
   `GET /api/v1/users` (no filter) returns both.
3. `PATCH /api/v1/users/{aliceId}` with `{username, roles}` returns `200` with the new username and
   role set; `GET /users?tenantId=<acme id>` reflects both; assigning a `PLATFORM` role to the tenant
   user answers `422`.
4. `PATCH /api/v1/tenants/<reserved id>` and `DELETE /api/v1/tenants/<reserved id>` answer `422`;
   `POST /api/v1/tenants` with `slug=keystone` answers `422`.
5. `DELETE /api/v1/tenants/<acme id>` while `alice` exists answers `409`; after deleting the user it
   answers `204` and the tenant disappears from the list.
6. `PATCH /api/v1/users/{id}` with a read-only caller (a second bearer token mapped to a
   `platform:user:read-only` user, as the existing test infrastructure already supports) answers `403`.

**Verification (commands)**

```bash
mvn clean verify                                        # backend + Testcontainers integration
mvn -pl platform/keystone-admin -am test                 # fast backend loop
cd platform/keystone-admin-ui && flutter analyze && flutter test
```

- The Flutter SDK is not on `PATH` in this environment; use
  `/Users/rajeshdebnath/manual-install/flutter/bin/flutter` (Flutter 3.47.5 / Dart 3.13.4), as recorded
  by the previous feature's delivery notes.
- Docker is available here (`/Users/rajeshdebnath/.docker/bin/docker`), so the integration test should
  be run rather than skipped — the previous feature left it unrun locally.

**Dependencies** — Phases 3–5.

**Verification of the phase** — all suites green (or the integration test explicitly reported as
skipped with the reason), and the new widget tests fail if the permission filter is removed (checked by
temporarily loosening it during development).

## Execution record (2026-09-27)

- **Backend, runnable here:** `PlatformSchemaTest` **(new, 4 cases)** — the reserved all-zero id, the
  `Keystone`/`keystone` names, `isPlatformTenant` true only for the reserved id (random v4 ids and
  `null` are customer tenants). `TenantSlugTest` **(extended, +2)** — `keystone` is reserved,
  `keystone-corp`/`acme` are not. That pins the two rules the UI and the API both depend on.
- **Deviation from the plan:** a `TenantServiceTest`/`UserServiceTest` for the service-level validators
  was dropped. Those helpers are `private static` (Java has no package-private access for a test), and
  the only ways to reach them would be to widen their visibility for the test or to introduce a
  utility class that exists for the test's sake — neither is warranted. The rule that matters (a slug
  is reserved, an id is the platform plane) is now a tested predicate on `TenantSlug`/`PlatformSchema`,
  and the service composition is asserted over HTTP in `AdminIntegrationTest`.
- **Backend, requires Docker (skipped here):** `AdminIntegrationTest` extended with the tenant list
  (synthetic `Keystone` row first, `"platform":true`, real tenant `false`), the three user-list scopes
  (platform / one tenant / all), the tenant role grant + rename via `PATCH /users/{id}` (asserting
  `"username":"alice-b"` and the new role set), the cross-plane rejection (422), the reserved slug and
  id refusals (422), and the delete sequence (409 with users → 204 for the user → 204 for the tenant).
  A Java-visible detail: Javalin's test client exposes only
  `delete(path, body, requestHandler)` (Kotlin default arguments are not callable from Java), so the
  delete-with-header calls pass an explicit `null` body — the first compile attempt failed on
  `delete(path, handler)`. The user-creation body is also read once, because the test client streams
  its response.
- **Frontend, runnable here:** `models_test.dart` **(extended to 15 cases)** — `Me.allows`/
  `allowsResource`/`canWrite` (wildcard allows everything, read/write implies read, read-only does not
  imply write, one resource does not grant another, absent fields default) and `Tenant.platform`/
  `isPlatform`/`userPlane` (default false for a payload without the field; the `Keystone` row parses
  and tolerates null timestamps), plus `Role.isPlatformScope`.
  `user_editor_test.dart` **(new, 3 widget cases)** with a recording `HttpClientAdapter` and overridden
  providers: the edit dialog offers only TENANT roles for a tenant user, shows the email as the
  non-editable Supabase identity, and sends **one** `PATCH /api/v1/users/user-1` with
  `{username, roles}` (roles sorted); the create dialog for the `Keystone` row shows "Platform users —
  no tenant", offers only PLATFORM roles, and posts `tenantId: null`.
  `admin_shell_test.dart` **(written during Phase 5, 8 cases)** covers the permission-filtered menu,
  the pane width toggling, the nested-location highlight, the unreadable-section placeholder and the
  no-readable-section panel.
- **Verification (commands run):** `mvn -pl platform/keystone-admin test` → **51 tests, 0 failures,
  0 errors, 0 skipped** (once Docker was available); `flutter analyze` → clean in both packages;
  `flutter test` → **36/36 passed**.
- **Closed out (2026-09-27, Docker started):** `mvn clean verify` → **BUILD SUCCESS, 0 skipped
  anywhere** (keystone-admin 51, inventory-server 10, and the other modules' suites) — so
  `AdminIntegrationTest` executed against a real `postgres:17-alpine` container and its assertions all
  hold: the synthetic `Keystone` row first with `"platform":true`, the three user-list scopes, the
  role grant + rename through `PATCH /users/{id}`, and the refusal/delete sequences.
- **Correction found by that run:** the expected status for a rejected *value* was wrong in the first
  draft of the test (and in these documents): `ValidationException` maps to **422** in
  `ProblemDetailMapper` (`VALIDATION -> 422`), not 400. The test now asserts 422 for the cross-plane
  role assignment and for the reserved slug/id refusals, and the plan/discovery/domain/API/security,
  `CHANGELOG.md` and `docs/ARCHITECTURE.md` texts were corrected to match.


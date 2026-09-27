# Phase 4 — Server-side API & realtime

**Scope** — switch every guarded route from an action code to a level check. Routes, verbs, status
codes and payloads do not change; only the guard call does.

**Artifacts** — four handlers, 15 call sites:

| File | Route | Before | After |
| --- | --- | --- | --- |
| `tenant/TenantHandler.java` | `GET /api/v1/tenants` | `platform:tenant:read` | `requireRead(ctx, PLATFORM_TENANT)` |
| | `POST /api/v1/tenants` | `platform:tenant:create` | `requireWrite(ctx, PLATFORM_TENANT)` |
| | `PATCH /api/v1/tenants/{id}` | `platform:tenant:update` | `requireWrite(ctx, PLATFORM_TENANT)` |
| | `DELETE /api/v1/tenants/{id}` | `platform:tenant:delete` | `requireWrite(ctx, PLATFORM_TENANT)` |
| `role/RoleHandler.java` | `GET /api/v1/roles` | `platform:role:read` | `requireRead(ctx, PLATFORM_ROLE)` |
| | `POST /api/v1/roles` | `platform:role:create` | `requireWrite(ctx, PLATFORM_ROLE)` |
| | `PATCH /api/v1/roles/{id}` | `platform:role:update` | `requireWrite(ctx, PLATFORM_ROLE)` |
| | `DELETE /api/v1/roles/{id}` | `platform:role:delete` | `requireWrite(ctx, PLATFORM_ROLE)` |
| `permission/PermissionHandler.java` | `GET /api/v1/permissions` | `platform:permission:read` | `requireRead(ctx, PLATFORM_PERMISSION)` |
| | `POST /api/v1/permissions` | `platform:permission:create` | `requireWrite(ctx, PLATFORM_PERMISSION)` |
| | `DELETE /api/v1/permissions/{id}` | `platform:permission:delete` | `requireWrite(ctx, PLATFORM_PERMISSION)` |
| `user/UserHandler.java` | `GET /api/v1/users` | `platform:user:read` | `requireRead(ctx, PLATFORM_USER)` |
| | `POST /api/v1/users` | `platform:user:create` | `requireWrite(ctx, PLATFORM_USER)` |
| | `PUT /api/v1/users/{id}/roles` | `platform:user:assign-role` | `requireWrite(ctx, PLATFORM_USER)` |
| | `DELETE /api/v1/users/{id}` | `platform:user:delete` | `requireWrite(ctx, PLATFORM_USER)` |

Imports gain `com.chetana.keystone.platform.admin.PermissionCatalog` (static constants) and drop
nothing else.

**Not changed:** `MeHandler` (uses `guard.principal(ctx)` only — no permission check), the
`/api/v1/auth/login` routes, `AuthFilter`, CORS, and every response DTO. **No Realtime change** —
realtime authorization is channel-based, not permission-code-based, and nothing in this change
alters channels or broadcast payloads.

**Dependencies** — Phase 3 (`requireRead` / `requireWrite` and the resource constants exist).

**Verification** — `mvn -pl apps/inventory/server -am package` compiles; the phase-7 integration test
proves `200`/`403`/`201` per level; a manual `curl` against a dev server with a read-only role
returns `403` with an RFC 9457 body for `POST /api/v1/tenants`.

## Notes

- One guard call per route, at the top of the lambda, exactly as today — no service-level
  permission checks are introduced.
- Deliberate behaviour change to call out in the changelog: a caller holding
  `platform:user:read-only` can no longer assign roles (needs `platform:user:read-write`), and a
  caller holding `platform:tenant:update` but not `platform:tenant:delete` now needs the single
  `platform:tenant:read-write` code.

## Execution record (2026-09-27)

- All 15 call sites switched (4 tenants, 4 roles, 3 permissions, 4 users); each handler static-imports
  its `PermissionCatalog` resource constant. Delivered with Phase 3 (atomic).
- Verified by `mvn -q -DskipTests compile` (repo-wide, clean) and the module test suite
  (32 passed / 1 skipped). The HTTP-level `200`/`403` proof belongs to Phase 7's integration test,
  which needs Docker — not available in this environment.

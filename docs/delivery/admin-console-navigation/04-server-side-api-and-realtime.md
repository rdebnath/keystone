# Phase 4 — Server-side API & realtime

**Scope** — expose the tenant-scoped user listing and the user update on the existing routes, keep
every route behind the same permission guard, and publish nothing.

**Artifacts**

| File | Change |
| --- | --- |
| `…/admin/user/UserHandler.java` | `GET /api/v1/users` reads the optional `tenantId` query parameter; new `PATCH /api/v1/users/{id}` |
| `…/admin/tenant/TenantHandler.java` | **unchanged** — `POST`/`PATCH`/`DELETE` already exist and now inherit the service-level guards from Phase 3; no route or DTO change |
| `…/admin/MeHandler.java` | **unchanged** — the extra `username` field rides on `MeDto` |
| `keystone-realtime` | **not used** — no publication is added (see below) |

**Routes**

```java
routes.get("/api/v1/users", ctx -> {
    guard.requireRead(ctx, PLATFORM_USER);
    String tenantId = ctx.queryParam("tenantId");
    ctx.json(service.list(tenantId == null ? null : Ids.uuid(tenantId)));
});

routes.patch("/api/v1/users/{id}", ctx -> {
    guard.requireWrite(ctx, PLATFORM_USER);
    UUID id = Ids.uuid(ctx.pathParam("id"));
    UserUpdateRequest request = ctx.bodyAsClass(UserUpdateRequest.class);
    ctx.json(service.update(id, request));
});
```

- The filter is a **query parameter on the existing endpoint**, not a new sub-resource, so
  `GET /api/v1/users` without it behaves exactly as before (all users, `platform:user:read-only`).
- `Ids.uuid(...)` already turns a malformed `tenantId` into a `ValidationException` → `422`; an unknown
  but well-formed tenant id simply returns an empty list (no enumeration concern at the platform
  plane, which is admin-only).
- `PATCH /users/{id}` is guarded by `requireWrite(ctx, PLATFORM_USER)` — the same write used by create,
  delete and `PUT /users/{id}/roles` (`docs/ARCHITECTURE.md` §9.3: role assignment is a write on the
  user resource). It responds `200` with the updated `UserDto`, matching `PATCH /tenants/{id}` and
  `PATCH /roles/{id}`.
- Tenant routes keep their existing guards (`requireRead`/`requireWrite` on `PLATFORM_TENANT`); the new
  rejections (reserved id, reserved slug) surface as `422` through the global RFC 9457 handler, and the
  "tenant has users" rule stays a `409` (`ConflictException`) — the UI shows the returned `detail`.
- `PUT /api/v1/users/{id}/roles` is untouched (still guarded, still delegating to the same role logic).

**Realtime** — none. The admin API publishes no broadcast today (`keystone-admin` does not depend on
`keystone-realtime`), and the console refreshes by invalidating its Riverpod providers after a
successful mutation. Adding live cross-admin updates would be a separate feature with its own channel
design, so it stays out of scope rather than being half-wired.

**Dependencies** — Phase 3 (`UserService.list(UUID)`, `UserService.update(...)`, `UserUpdateRequest`).

**Verification** — `mvn -pl platform/keystone-admin -am compile` clean; route behaviour is pinned in
Phase 7 (`AdminIntegrationTest` over Testcontainers, plus the guard slice tests).

## Execution record (2026-09-27)

- `UserHandler`: `GET /api/v1/users` reads the optional `tenantId` query parameter and passes
  `null` when absent (`Ids.uuid` keeps turning a malformed value into a 422); `PATCH /api/v1/users/{id}`
  added, guarded by `requireWrite(ctx, PLATFORM_USER)`, responding `200` with the updated `UserDto`.
- `TenantHandler` and `MeHandler` needed no change: the tenant routes already exist (and now inherit the
  service-level reserved-id/slug guards), and `username` rides on `MeDto`.
- Realtime: nothing published, as planned (`keystone-realtime` is not wired into `keystone-admin`).
- Verification: `mvn -q -pl platform/keystone-admin -am -DskipTests compile` clean; the existing
  `PermissionGuardTest`/`AuthFilterTest` slices still pass (45 tests, 1 skipped overall).
- Delivered together with Phase 3 because `UserService.list` changed signature (see
  `03-domain-and-application-services.md`).


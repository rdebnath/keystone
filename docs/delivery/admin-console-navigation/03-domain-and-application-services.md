# Phase 3 — Domain & application services

**Scope** — model the platform plane as a reserved tenant, make tenant create/update/delete safe
against it, add tenant-scoped user listing and the user-update use case, and expose the caller's
username. No REST wiring (Phase 4) and no Flutter code (Phase 5).

**Artifacts**

| File | Change |
| --- | --- |
| `platform/keystone-admin/src/main/java/…/admin/PlatformSchema.java` | add `PLATFORM_TENANT_ID` (`UUID.fromString("00000000-0000-0000-0000-000000000000")`), `PLATFORM_TENANT_NAME` (`"Keystone"`) and `isPlatformTenant(UUID)`; javadoc says the row is synthetic and never persisted |
| `…/admin/tenant/TenantDto.java` | add `boolean platform` (last-but-one component, before the timestamps) |
| `…/admin/tenant/TenantService.java` | `list()` returns `platformTenant()` first, then the persisted tenants (map to `platform=false`); `create`/`update` reject the reserved slug; `update`/`delete` reject the reserved id; `platformTenant()` returns the DTO with `null` timestamps |
| `…/admin/user/UserUpdateRequest.java` **(new)** | `record UserUpdateRequest(String username, List<String> roles)` — mirrors the wire body; `email` is deliberately absent |
| `…/admin/user/UserService.java` | `list(UUID tenantId)` filters (`null` = all, reserved id = `tenant_id IS NULL`, else that tenant); `create` maps the reserved id to the platform plane; new `update(UUID, UserUpdateRequest)`; `checkUsernameUnique` gains an "exclude this user" form; private `upsertRoles` reused by `assignRoles` and `update` |
| `…/admin/identity/MeDto.java`, `…/admin/identity/MeService.java` | `MeDto` gains `username`; `MeService.me` fills it from the already-loaded `users` row |

**Design**

```java
// PlatformSchema — reserved constants for the synthetic platform plane.
public static final String RESERVED_SLUG = "keystone";
public static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
public static final String PLATFORM_TENANT_NAME = "Keystone";

public static boolean isPlatformTenant(UUID tenantId) {
    return PLATFORM_TENANT_ID.equals(tenantId);
}
```

```java
// TenantService.list() — synthetic platform row first, then the persisted tenants.
public List<TenantDto> list() {
    return Stream.concat(Stream.of(platformTenant()), data.read().selectFrom(TENANTS)
                    .orderBy(TENANTS.NAME)
                    .fetch()
                    .map(r -> new TenantDto(r.getId(), r.getName(), r.getSlug(), false, r.getCreatedAt(), r.getUpdatedAt())))
            .toList();
}

private static TenantDto platformTenant() {
    return new TenantDto(PlatformSchema.PLATFORM_TENANT_ID, PlatformSchema.PLATFORM_TENANT_NAME,
            PlatformSchema.RESERVED_SLUG, true, null, null);
}
```

- `create`/`update` call a new `validateSlug(slug)` that rejects `PlatformSchema.RESERVED_SLUG` with a
  `ValidationException("slug is reserved: keystone")`; `update`/`delete` call `requireTenantRow(id)`
  first, which rejects the reserved id with
  `ValidationException("The platform tenant cannot be modified")`.
- The reserved id is never persisted, so no code path may insert it (`create` always mints a new id).

```java
// UserService — tenant-scoped listing.
public List<UserDto> list(UUID tenantId) {
    var query = data.read().selectFrom(USERS).orderBy(USERS.USERNAME);
    if (tenantId != null) {
        query = query.where(PlatformSchema.isPlatformTenant(tenantId)
                ? USERS.TENANT_ID.isNull()
                : USERS.TENANT_ID.eq(tenantId));
    }
    // ... unchanged role grouping and mapping
}
```

```java
// UserService.update — rename + replace the role set in one transaction.
public UserDto update(UUID id, UserUpdateRequest request) {
    var existing = data.read().selectFrom(USERS).where(USERS.ID.eq(id)).fetchOne();
    if (existing == null) {
        throw new NotFoundException("User not found: " + id);
    }
    String username = Username.normalize(request.username());
    if (!username.equals(existing.getUsername())) {
        checkUsernameUnique(username, existing.getTenantId(), id);
    }
    List<String> roles = normalized(request.roles());
    OffsetDateTime now = now();
    data.transaction(tx -> {
        tx.update(USERS)
                .set(USERS.USERNAME, username)
                .set(USERS.UPDATED_AT, now)
                .where(USERS.ID.eq(id))
                .execute();
        replaceRoles(tx, id, existing.getTenantId(), roles);
    });
    return new UserDto(id, existing.getSub(), username, existing.getEmail(), existing.getTenantId(),
            existing.getMustChangePassword(), roles, existing.getCreatedAt(), now);
}
```

- `checkUsernameUnique(username, tenantId, excludeUserId)` keeps the existing per-plane uniqueness rule
  (`users.tenant_id IS NULL` for platform users) and excludes the user being renamed; the old
  two-argument form delegates with `null`.
- `replaceRoles(tx, userId, tenantId, codes)` is the existing delete-then-insert role logic factored
  out of `assignRoles(...)` (which keeps its signature and behaviour) — role scope is validated
  against the user's plane exactly as today (`PLATFORM` roles ⇒ platform user, `TENANT` roles ⇒ tenant
  user).
- `create` normalises the tenant: `UUID tenantId = PlatformSchema.isPlatformTenant(request.tenantId()) ? null : request.tenantId();`
  so the console can post the reserved id it received from `GET /tenants` while the row still stores
  `tenant_id IS NULL`. `null` continues to mean the platform plane (existing callers unaffected).

**Dependencies** — Phase 1 (design confirmed).

**Verification** — `mvn -pl platform/keystone-admin -am compile` clean; unit-level checks land in
Phase 7. `mvn -q -DskipTests compile` at the root confirms no jOOQ/codegen churn.
## Execution record (2026-09-27)

- `PlatformSchema`: added `PLATFORM_TENANT_ID` (all-zero UUID), `PLATFORM_TENANT_NAME` (`Keystone`)
  and the null-safe `isPlatformTenant(UUID)`.
- `TenantDto`: gained `boolean platform` (third component, before the timestamps).
- `TenantService`: `list()` now returns `platformTenant()` first (`platform: true`, reserved id/name/slug,
  null timestamps) followed by the persisted tenants mapped with `platform: false`; `create`/`update`
  normalize the slug through the new `validateSlug(...)` (which rejects the reserved `keystone` slug);
  `update`/`delete` call `requireModifiableTenant(id)` first (rejects the reserved id with a
  `ValidationException` → 422).
- `UserUpdateRequest` **(new record)** — `(String username, List<String> roles)`; the javadoc records
  why `email` is absent.
- `UserService`: `list(UUID tenantId)` (null = all, reserved id = `tenant_id IS NULL`, else that tenant)
  with the role grouping joined to `users` so it is bounded by the same filter; `create` normalizes the
  reserved tenant id to `null` via `tenant(...)`; new `update(UUID, UserUpdateRequest)` renames +
  replaces roles in one transaction and returns the updated `UserDto`; the private role-replacement
  method is now `replaceRoles(DSLContext, ...)` (shared by `create`, `update` and `assignRoles`, whose
  public contract is unchanged); `checkUsernameUnique` gained an excluding overload plus a shared
  `tenantCondition(...)` helper (same per-plane uniqueness rule as before).
- `MeDto`/`MeService`: `username` added to the profile (from the already-loaded `users` row).
- Deviation from the plan wording: the shared private role method is named `replaceRoles`, not
  `upsertRoles` (it deletes then inserts, so "replace" is accurate).
- Verification: `mvn -q -pl platform/keystone-admin -am -DskipTests compile` clean;
  `mvn -pl platform/keystone-admin test` → **45 tests, 0 failures, 0 errors, 1 skipped** (the skip is the
  Testcontainers `AdminIntegrationTest`). No jOOQ/codegen churn.
- Delivered together with Phase 4: `UserService.list` changed signature, so the handler had to move in
  the same step to keep the module compiling (as recorded in `04-server-side-api-and-realtime.md`).


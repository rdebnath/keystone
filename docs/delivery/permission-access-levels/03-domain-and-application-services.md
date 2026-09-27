# Phase 3 — Domain & application services

**Scope** — introduce the two-level catalog and teach the persistence-adjacent services to use it:
the `Access` value object, the rewritten `PermissionCatalog`, the level-aware `PermissionGuard`,
bootstrap seeding (unchanged) and code-shape validation in `PermissionService`.

**Artifacts**

| File | Change |
| --- | --- |
| `platform/keystone-admin/src/main/java/…/admin/identity/Access.java` | **new** — `READ_ONLY` ("read-only"), `READ_WRITE` ("read-write"), `suffix()`, `suffixes()` |
| `platform/keystone-admin/src/main/java/…/admin/PermissionCatalog.java` | **rewrite seed list** — 7 resource constants, `RESOURCES`, `PERMISSIONS = seeds()` (14 two-level rows + `*`), `code(...)`, `acceptedCodes(...)`, `hasAccessLevel(...)` |
| `platform/keystone-admin/src/main/java/…/admin/auth/PermissionGuard.java` | **replace** `require(ctx, code)` with `requireRead` / `requireWrite` / private `requireAny` plus the pure `grants(Set<String>, List<String>)` helper; keep `principal(ctx)` |
| `platform/keystone-admin/src/main/java/…/admin/BootstrapRunner.java` | **unchanged** — the existing seed loop already inserts `PermissionCatalog.PERMISSIONS` with `onConflictDoNothing` and grants `platform-admin` the wildcard |
| `platform/keystone-admin/src/main/java/…/admin/permission/PermissionService.java` | `validate(...)` additionally rejects a code that does not end in a level (`PermissionCatalog.hasAccessLevel`) |
| `platform/keystone-admin-ui`/… | not this phase |

**No change:** `PermissionResolver` (still resolves a `Set<String>` of codes), `MeService`/`MeDto`,
`RoleService` (it already validates that every granted code exists and matches the role's scope),
`PermissionDto`/`PermissionRequest`, `AdminModule`, all jOOQ-generated types, all Liquibase
changelogs.

**Dependencies** — Phase 1 confirmed (decisions 2–7 and open questions 1–4 answered).

**Verification** — `mvn -pl platform/keystone-admin -am test` compiles and the phase-7 unit tests
(`PermissionCatalogTest`, `PermissionGuardTest`, `AccessTest`) pass.

## Catalog shape

```java
public static final List<Permission> PERMISSIONS = List.of(
        new Permission(WILDCARD, "PLATFORM"),
        new Permission(code(PLATFORM_TENANT, Access.READ_ONLY), "PLATFORM"),
        new Permission(code(PLATFORM_TENANT, Access.READ_WRITE), "PLATFORM"),
        … // 2 rows per resource, platform scope then tenant scope
        new Permission(code(TENANT_USER, Access.READ_WRITE), "TENANT"));
```

`PermissionService.list()` keeps ordering by `code`, so the catalog tab shows the two levels of each
resource adjacently (`platform:tenant:read` before `platform:tenant:write`).

## Guard logic

```java
public void requireRead(io.javalin.http.Context ctx, String resource) {
    requireAny(ctx, PermissionCatalog.acceptedCodes(resource, Access.READ_ONLY));
}

public void requireWrite(io.javalin.http.Context ctx, String resource) {
    requireAny(ctx, PermissionCatalog.acceptedCodes(resource, Access.READ_WRITE));
}
```

`acceptedCodes` is the whole rule in one place: `READ_ONLY → [":read-only", ":read-write"]`,
`READ_WRITE → [":read-write"]`. `requireAny` reuses the cached `permissions` request attribute
(`PermissionGuard.PERMISSIONS_ATTRIBUTE`) exactly as `require` does today, so the resolver is still
hit at most once per request. `grants(...)` is `static`/pure and returns a boolean, so it can be
unit-tested without Guice or a database.

## No data migration (decision 6)

The catalog is seeded at bootstrap and this is fresh development on an unreleased platform, so no
prune, backfill or grant-rewrite code exists: the seed loop inserts the 14 level codes + `*` with
`onConflictDoNothing`, and nothing else touches the catalog. A database that predates the two levels
is reset with `SchemaTool reset` rather than upgraded.

## Execution record (2026-09-27)

- Delivered together with Phase 4 (one atomic guard-API change): `Access.java` added;
  `PermissionCatalog` rewritten; `PermissionGuard.require(...)` replaced by `requireRead` /
  `requireWrite` plus the pure `grants(...)`; `PermissionService.validate(...)` requires a level
  suffix.
- `BootstrapRunner` ended up **unchanged**, and after the 2026-09-27 follow-up it contains no prune
  code at all — fresh development, so the action-granular codes never existed in this codebase.
- Verification: `mvn -q -DskipTests compile` clean repo-wide; `mvn -pl platform/keystone-admin test`
  → 32 tests, 0 failures, 1 skipped (Testcontainers needs Docker).
- `PermissionGuard.grants(...)` is package-private specifically for `PermissionGuardTest` (Phase 7).

## Follow-ups

- The tenant-scoped codes (`tenant:*`) are seeded but unused: no tenant-plane route consumes them
  yet. Out of scope here (the tenant console is a later change) — they are seeded so the two planes
  stay symmetric.

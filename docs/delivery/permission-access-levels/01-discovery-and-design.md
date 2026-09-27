# Phase 1 — Discovery & design

**Scope** — fix the two-level permission model, its code format, its enforcement rule, and the absence
of any data migration (fresh development on an unreleased platform). No code changes in this phase.

**Artifacts** — this document (+ the decisions and open questions in `plan.md`).

**Dependencies** — none.

**Verification** — the user confirms the model and answers the five open questions in `plan.md`;
Phases 3–8 are then executed one at a time against that agreed model.

## Requirements

- Replace the four CRUD-granular codes per resource with **exactly two levels**: **read/write** and
  **read-only**.
- A read/write grant covers `create`, `update`, `delete` **and** `read`; a read-only grant covers
  `read` only.
- The wildcard `*` (platform-admin only) keeps granting everything.
- Code checks permissions, never roles (§9.3 of `docs/ARCHITECTURE.md`) — that does not change.
- No database schema change, no REST payload shape change.

## Code format

```
<scope>:<resource>:<level>          e.g. platform:tenant:read-write
     scope    ∈ { platform, tenant }   (unchanged; matches permissions.scope PLATFORM/TENANT)
     resource ∈ { tenant, role, permission, user }   (per scope; unchanged namespaces)
     level    ∈ { read-only | read-write }   (NEW — replaces create|read|update|delete|assign-role)
```

`Access` (new enum, `platform.admin.identity`, sibling of `Scope`):

```java
public enum Access {
    READ_ONLY("read-only"),
    READ_WRITE("read-write");
}
```

`PermissionCatalog` (existing class) gains:

```java
public static final String PLATFORM_TENANT    = "platform:tenant";
public static final String PLATFORM_ROLE      = "platform:role";
public static final String PLATFORM_PERMISSION = "platform:permission";
public static final String PLATFORM_USER      = "platform:user";
public static final String TENANT_ROLE        = "tenant:role";
public static final String TENANT_PERMISSION  = "tenant:permission";
public static final String TENANT_USER        = "tenant:user";

/** Codes accepted for a (resource, access) check: read-only also accepts read-write. */
static List<String> acceptedCodes(String resource, Access access);

/** The code that carries a level on a resource. */
static String code(String resource, Access access);   // resource + ":" + access.suffix()

/** Whether a code ends in read-only or read-write. */
static boolean hasAccessLevel(String code);
```

## Enforcement rule

| Route kind | Guard call | Accepted codes |
| --- | --- | --- |
| `GET` (read) | `requireRead(ctx, resource)` | `<resource>:read-only`, `<resource>:read-write`, `*` |
| `POST` / `PATCH` / `PUT` / `DELETE` | `requireWrite(ctx, resource)` | `<resource>:read-write`, `*` |

Denial stays `AccessDeniedException` → RFC 9457 `403`; the message names the required code(s) so
operators can grant precisely.

## Catalog (14 codes + wildcard)

| Resource | `:read-only` | `:read-write` |
| --- | --- | --- |
| `platform:tenant` | grants listing tenants | grants create/update/delete tenant |
| `platform:role` | grants listing roles | grants create/update/delete role (incl. its permission grants) |
| `platform:permission` | grants listing the catalog | grants create/delete permission |
| `platform:user` | grants listing users | grants create/delete user **and** `PUT /users/{id}/roles` |
| `tenant:role`, `tenant:permission`, `tenant:user` | mirrored for the tenant plane (seeded for the future tenant console; no tenant-plane routes exist yet) | mirrored |

## No data migration

The catalog is *seeded* at bootstrap and this is fresh development on an unreleased platform, so
there are no replaced codes to carry over or clean up: no prune, no backfill, no Liquibase data
changeset, and no rewrite of existing role grants. A dev/demo database that predates the two levels is
reset with `SchemaTool reset` instead.

**Why the changelog stays out of it:** the catalog is seeded at bootstrap, not in the Liquibase
changelog, and the changelog is rendered to DDL offline for jOOQ codegen — adding DML would put
non-DDL statements through the `DDLDatabase` renderer. `docs/CODING_GUIDELINES_BACKEND.md` §7
(`Liquibase changelogs`) keeps changelogs as the single source for schema and codegen, so seed data
lives where seeding already happens.

## Acceptance criteria

1. `GET /api/v1/permissions` returns exactly the 14 two-level codes + `*`; no `create`/`update`/
   `delete`/`assign-role` code remains.
2. A caller whose role grants `platform:tenant:read-only` gets `200` on `GET /api/v1/tenants` and
   `403` on `POST`/`PATCH`/`DELETE /api/v1/tenants*`.
3. A caller whose role grants `platform:tenant:read-write` gets `200` on `GET` and `201`/`200`/`204`
   on mutations of tenants.
4. The platform admin (wildcard `*`) is unaffected.
5. `POST /api/v1/permissions` rejects a code that does not end in `read-only`/`read-write` with `422`
   (a `ValidationException`, which `ProblemDetailMapper` maps to `VALIDATION -> 422`).
6. Roles created through the UI can be given either level per resource; the UI shows the level.
7. The catalog contains exactly the 14 level codes plus `*`, and no code exists to clean up or migrate
   any other code (fresh development — there is nothing to prune).

## Correction (2026-09-27)

Acceptance criterion 5 originally read `400`. That was wrong, and it was corrected while delivering
`admin-console-navigation`:

- A rejected *value* is a `ValidationException`, and the platform's single RFC 9457 mapper answers it
  with **422** — `ProblemDetailMapper.statusOf` has `case VALIDATION -> 422` (its only 422), alongside
  `NOT_FOUND -> 404`, `CONFLICT -> 409`, `ACCESS_DENIED -> 403`, `INTERNAL -> 500`. No application code
  returns 400 itself; the first draft of the new integration test asserted 400 and failed with
  "expected: 400 but was: 422" against a real server.
- The same mistake had been copied into that feature's own documents and was fixed there too (see
  `docs/delivery/admin-console-navigation/07-testing.md` and `08-delivery.md`).

**Why it happened:** `docs/CODING_GUIDELINES_BACKEND.md` claimed validation returns `400` in three
places (§8 "Validation", §9 "Validation failures", §13 "Validate on the way in") while listing `422` as
a valid failure status in §8 — the guidelines contradicted both themselves and the shipped mapper.

**Resolved (2026-09-27, on review):** the guidelines were aligned with the shipped code rather than the
code with the guidelines — a rejected value is **`422`**, and `400` is reserved for a request the
framework could not parse (malformed JSON, missing body). Updated: §8 (both bullets), §9 (validation
status + an explicit `ProblemDetailMapper` status-mapping list), §13, the §14 review checklist, and
`docs/CODING_GUIDELINES_FRONTEND.md` §9 (which now distinguishes `422` from `400` when mapping statuses
to typed failures). No code changed: `ProblemDetailMapper` already did this.

# Phase 3 — Domain & application services

**Scope** — the two rules as code that can be tested without HTTP or a widget: parsing the level, the filter it
builds, the grant condition, and the console-side mirrors.

**Artifacts**

| File | Change |
| --- | --- |
| `platform/keystone-admin/.../identity/Access.java` | `from(String)` + `optional(String)` — a suffix parse with a `ValidationException` for anything else. |
| `.../permission/PermissionService.java` | `list(…, Access access, …)` + `accessFilter(access)` (`PERMISSIONS.CODE.endsWith(":" + suffix)`). |
| `.../role/RoleService.java` | `grantableTo(owner)` replaces `ownerFilter(owner, …)` inside `grantPermissions`; the "Unknown permission" message now names the catalogue restriction for a global role. |
| `platform/keystone-admin-ui/lib/src/models/list_query.dart` | `access` field + `withAccess(...)` + the parameter in `toQueryParameters()`, and `permissionsFor` seeding a no-owner role from `Tenant.platformId`. |
| `.../lib/src/models/models.dart` | `Tenant.platformId` — the reserved platform-tenant id, beside the `isPlatform` flag it describes. |
| `.../lib/src/core/permission_selection.dart` | `retaining` applies the role-owner rule (`_grantableTo`): a global role keeps catalogue picks only. |
| `.../lib/src/core/lists.dart` | `ListQueryLocation` round-trips `access`. |

**Dependencies** — phase 1. Phase 4 wires the parameter to the routes; phase 5 renders the control.

**Verification** — `AccessTest` 5 cases (suffix parse, refusal, absent filter); `permission_selection_test` and
`list_query_test` on the console side; the full `keystone-admin` suite green (103 tests).

## Notes on the shape of each rule

- **`Access.from` parses the suffix, not the enum name** — `read-only`, not `READ_ONLY` — because the suffix is
  what a permission code ends in and what the console already mirrors (`PermissionAccess.suffix`).
- **`accessFilter` is a suffix match, not a new column**: the level is *derivable* from the code, so a column
  would be a schema change for information the row already carries. The wildcard therefore falls in neither
  level's set, which is correct: it grants everything and carries no level.
- **`grantableTo` is deliberately not `ownerFilter`**, and the javadoc says so, because the two look
  interchangeable and are not: one answers the caller's question, the other the role's.
- **`Tenant.platformId` lives on the model**, so `ListQuery` (in `models/`) can reach it — putting it in
  `core/permissions.dart` would have made the models import `core/`, which imports the models.

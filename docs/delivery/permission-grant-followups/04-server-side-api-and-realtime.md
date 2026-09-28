# Phase 4 — Server-side API & realtime

**Scope** — expose the level filter on both permission list routes. No DTO, no guard, no schema and no realtime
message changes.

**Artifacts**

| File | Change |
| --- | --- |
| `.../permission/PermissionHandler.java` | `GET /api/v1/permissions` reads `Access.optional(ctx.queryParam("access"))` and passes it. |
| `.../permission/TenantPermissionHandler.java` | `GET /api/v1/tenant/permissions` does the same — the level is a property of the code, so the tenant plane filters identically. |

**Dependencies** — phase 3 (`Access.optional`, `PermissionService.list`).

**Verification** — `AdminIntegrationTest`: `?access=read-only` returns read-only codes and no `:read-write`;
`?access=read-write` the reverse; `?access=sideways` is a `422`. `TenantSelfServiceIntegrationTest`: the same
three on the tenant route, against a permission the tenant defined itself.

## The route's contract, as it now stands

```
GET /api/v1/permissions        ?tenantId=&scope=&access=&q=&page=&size=&sort=&order=
GET /api/v1/tenant/permissions ?scope=&access=&q=&page=&size=&sort=&order=
```

Both return the page envelope (`Paged<T>`) and are behind the plane's *permission* read grant — unchanged. The
new parameter is validated like every other one (`QueryParams` handles `page/size/sort/order/q`;
`Scope.optional`/`Access.optional` handle the enum-ish filters), so an unknown value is a `422` and an absent one
is not an error.

## Why nothing else moved

- **No new route**: this is a filter on an existing list, exactly as `scope` was.
- **No DTO change**: the level is derived from the code the DTO already carries (`PermissionAccess.fromCode` on
  the client), so nothing had to be added to the JSON.
- **No index**: the filter is a `LIKE '%:read-only'` suffix match on a column already indexed by the default
  order's `code` prefix. At the catalogue's realistic size (hundreds of rows, not millions) the sequential scan
  the planner picks is not a problem; if a catalogue ever grows past that, the recorded follow-up is a
  `code text_pattern_ops` index, not a schema change.
- **No realtime**: no message shape changed and nothing is broadcast about permissions.

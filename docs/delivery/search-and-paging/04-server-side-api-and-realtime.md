# Phase 4 — Server-side API & realtime

## Scope

The REST delta: seven list routes return a page envelope and accept `page`/`size`/`sort`/`order`/`q`
(plus their existing filters), and three `/options` routes are added for pickers. Handlers stay thin
(parse → call the service → `ctx.json(...)`); **no write route changes**.

## Route-by-route delta

| Route | Plane | Change |
| --- | --- | --- |
| `GET /api/v1/tenants` | platform | paged; `q`; response becomes `Page<TenantDto>` ✅ synthetic row first (phase 3) |
| `GET /api/v1/tenants/options` | platform | **new** → `OptionList<TenantDto>` |
| `GET /api/v1/users` | platform | paged; `q`; keeps `tenantId` |
| `GET /api/v1/roles` | platform | paged; `q`; keeps `tenantId`; adds `scope` |
| `GET /api/v1/roles/options` | platform | **new** → `OptionList<RoleDto>`; keeps `tenantId` |
| `GET /api/v1/permissions` | platform | paged; `q`; keeps `tenantId`; adds `scope` |
| `GET /api/v1/tenant/users` | tenant | paged; `q` (no tenant parameter — unchanged rule) |
| `GET /api/v1/tenant/roles` | tenant | paged; `q`; adds `scope` |
| `GET /api/v1/tenant/roles/options` | tenant | **new** → `OptionList<RoleDto>` |
| `GET /api/v1/tenant/permissions` | tenant | paged; `q`; adds `scope` |
| `POST`/`PATCH`/`DELETE` routes, `PUT …/roles`, `PUT …/password`, `GET /me`, `auth/login` | both | **unchanged** |

Handler shape (identical in the four platform handlers and the three tenant handlers):

```java
routes.get("/api/v1/roles", ctx -> {
    guard.requireRead(ctx, PLATFORM_ROLE);
    UUID tenantId = Ids.optionalUuid(ctx.queryParam("tenantId"));
    Scope scope = Scope.optional(ctx.queryParam("scope"));   // "PLATFORM"|"TENANT"|absent
    ctx.json(service.list(guard.callerScope(ctx), tenantId, scope,
            QueryParams.search(ctx), QueryParams.page(ctx)));
});
```

The tenant handlers keep calling `guard.callerTenantScope(ctx)` first, so the tenant is still the
caller's and never a request field.

## Envelope and errors

```json
{ "items": [ … ], "page": 0, "size": 25, "totalElements": 142, "totalPages": 6,
  "hasNext": true, "hasPrevious": false }
```

| Input | Result |
| --- | --- |
| `page=-1`, `size=0`, `size=101`, `size=abc` | `422` `application/problem+json` (`ValidationException`) |
| `sort=nope`, `order=sideways` | `422`, the detail names the allowed sort keys |
| `scope=whatever` | `422` (the existing `Scope.from` message) |
| `q` longer than 100 chars | `422` |
| `page` beyond `totalPages` | `200` with `"items": []` and the real totals — a stale page is not an error |
| unknown/ignored query params | ignored (as today) |

`GET /api/v1/tenants/options` cannot be shadowed by a path parameter: neither `tenants` nor `roles` has a
`GET …/{id}` route, and the integration test asserts `/tenants/options` answers `200` with `items`.

## Realtime

**No change.** The admin console is request/response only — there is no Supabase Realtime channel
carrying tenants, users, roles or permissions (`docs/delivery/tenant-country-user-phone`, phase 4), so
there is no broadcast payload to version or extend, and `RealtimePublisher` is untouched.

## Backward compatibility

The list responses change shape from a JSON **array** to a JSON **object**, so this is a **breaking
contract change for list endpoints only**. It is safe here because the only client is the Flutter console
in this repository (`platform/keystone-admin-ui`, hosted by `apps/inventory/frontend`) and it ships from
the same commit as the server; phases 5 and 7 change and test it in the same delivery. The
`docs/ARCHITECTURE.md` and `CHANGELOG.md` entries (phase 8) record the break explicitly.

## Dependencies

- Phase 3 (service signatures) and phase 1 (the contract).

## Execution (2026-09-28)

Every route in the delta table above shipped. Two things are worth recording:

- **No new Guice bindings were needed.** The three `/options` routes live in the handlers that already own
  their resource (`TenantHandler`, `RoleHandler`, `TenantRoleHandler`), so `AdminModule` is unchanged —
  unlike a new handler class, they share the same service and the same read guard.
- **`/tenants/options` and `/roles/options` cannot be shadowed** by a path-parameter route: neither
  resource has a `GET …/{id}`, and the integration test asserts `/api/v1/tenants/options` answers `200` with
  an `items` envelope rather than being parsed as an id.

`QueryParams` keeps handlers thin — the whole delta at each call site is one line:

```java
ctx.json(service.list(guard.callerScope(ctx), tenantId, scope, QueryParams.search(ctx),
        QueryParams.page(ctx)));
```

## Verification

`AdminIntegrationTest` gained a second test (`should_page_search_and_filter_the_lists_on_the_server`) that
drives the real embedded server against PostgreSQL and asserts:

- the envelope on a real list (`page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`) and
  that page 2 continues page 1 with no repeat and no gap;
- **`q` finds a tenant that sorts past the first page** — the acceptance criterion of this delivery;
- the pinned platform row: counted, present for `q=key`, absent for `q=tenant-5`;
- `q=%` matching nothing (wildcards are literal);
- `422` for `sort=nope`, `order=sideways`, `size=101`, `page=-1` and a 101-character `q`;
- a stale page (`page=99`) answering `200` with `items: []` and the real totals;
- the `/options` routes on both planes, and that the `scope` filter narrows roles/permissions.

`TenantSelfServiceIntegrationTest` (tenant plane) and `PermissionGuardTest` still pass unchanged: the list
routes kept their guards and their bodies kept their meaning. **No realtime code was touched** — the
console is request/response only, so there is no channel or payload version to change.


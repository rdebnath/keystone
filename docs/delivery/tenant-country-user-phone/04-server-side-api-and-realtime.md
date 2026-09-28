# Phase 4 — Server-side API & realtime

## Scope

The REST contract delta for the two planes that already expose these resources. **No handler code
changes**: Javalin binds the request body to the record via `ctx.bodyAsClass(...)`, so a new record
component is read from the JSON body automatically, and the response is the serialized DTO.

## Contract delta

| Route | Change |
| --- | --- |
| `GET /api/v1/tenants` | response items gain `country` (string or `null`) |
| `POST /api/v1/tenants` | request gains optional `country`; response gains `country` |
| `PATCH /api/v1/tenants/{id}` | request gains optional `country` (absent/blank clears it); response gains `country` |
| `GET /api/v1/users[?tenantId=]` | response items gain `phoneNumber` (string or `null`) |
| `POST /api/v1/users` | request gains optional `phoneNumber`; response gains `phoneNumber` |
| `PATCH /api/v1/users/{id}` | request gains optional `phoneNumber`; response gains `phoneNumber` |
| `POST /api/v1/tenant/users`, `PATCH /api/v1/tenant/users/{id}` | same as the platform-plane user routes — the tenant handlers delegate to the same `UserService` and take the same records |
| `DELETE` routes, `PUT …/roles`, `PUT …/password`, roles/permissions routes | unchanged |
| `GET /api/v1/me` | **unchanged** (`MeDto` is not touched — see the plan's non-goals) |

Unknown JSON fields keep being ignored (Jackson's default here) and the new fields are optional, so
this is a **backward- and forward-compatible** change: an older client simply omits them, a newer
client against an older server gets them ignored.

## Realtime

No Supabase Realtime channel carries tenants or users — the platform admin console is
request/response only — so there is nothing to broadcast and no payload version to bump. Realtime is
out of scope for this delivery.

## Artifacts

| Artifact | Change |
| --- | --- |
| `…platform.admin.tenant.TenantHandler` | none (verified) |
| `…platform.admin.user.UserHandler` | none (verified) |
| `…platform.admin.user.TenantUserHandler` | none (verified) |
| `…platform.admin.MeHandler` / `identity.MeDto` | none (deliberate) |

## Dependencies

- Phase 3 (the records and services must carry the fields).

## Execution (2026-09-28)

Confirmed rather than changed: no `Handler`, route or broadcast file was touched. The new components
bind through `ctx.bodyAsClass(TenantRequest.class)` / `UserRequest.class` / `UserUpdateRequest.class`
and serialize through the DTO records, which the HTTP tests verify end to end — including that a
tenant created **without** a country still answers `"country":null` (the older payload shape), so the
change is backward-compatible in both directions.

## Verification

- `AdminIntegrationTest` (platform plane) and `TenantSelfServiceIntegrationTest` (tenant plane) drive
  the real embedded server: create → list → patch round trips plus the `422` rejections.
- `git diff` on the two handler packages shows no change.


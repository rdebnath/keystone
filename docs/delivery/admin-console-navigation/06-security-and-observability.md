# Phase 6 — Security & observability

**Scope** — confirm the new surface introduces no authorization hole, no secret leak, and no new
configuration, and that the failures it can produce are reported usefully.

**Artifacts**

| Concern | Treatment | Where |
| --- | --- | --- |
| Authorization on the new route | `PATCH /api/v1/users/{id}` → `guard.requireWrite(ctx, PLATFORM_USER)`; the tenant-scoped `GET /users?tenantId=` keeps `requireRead(ctx, PLATFORM_USER)` | `UserHandler` |
| Reserved platform tenant | `PATCH`/`DELETE /tenants/{id}` and tenant create/rename reject the reserved id/slug with `422` before any write | `TenantService` |
| Client-side filtering | Menu filtering and hidden buttons are UX only; the backend re-checks every call (`docs/ARCHITECTURE.md` §9.6). The shell does not *hide* a section and then still call the API — an unauthorized deep link renders a placeholder | `AdminShell`, `06` review |
| Escalation | Unchanged rules: a `TENANT` role cannot be granted to a platform user and vice versa (existing `assignRoles` validation is reused by `update`) | `UserService` |
| Identity integrity | `email` is not editable from the console, so `users.email` stays the GoTrue identity that `LoginService` authenticates with — the rename path touches only `username`, which login resolves by lookup | `UserService.update` |
| Input validation | `Username.normalize` (non-blank, no `@`), `TenantSlug.normalize` (non-blank, `[a-z0-9-]+`), `Ids.uuid` (path/query parameters) — all existing validators, no new ad-hoc parsing | Phase 3 |
| Payload typing | `UserUpdateRequest` is a record (no `Map`/`JsonNode`); the Flutter side mirrors it with `UpdateUserRequest` (`docs/CODING_GUIDELINES_BACKEND.md` §13, frontend §14) | Phases 3, 5 |
| Secrets / PII | No secret is added; the token interceptor is unchanged. The console never logs tokens, passwords or emails (existing `log.e('…', error: e)` calls stay event-only; the new `apiErrorMessage` surfaces only the server's `detail`) | Phase 5 |
| Error contract | All new failures reuse `ValidationException` → `422`, `NotFoundException` → `404`, `ConflictException` → `409`, `AccessDeniedException` → `403` through the single RFC 9457 handler — no new error type and no leaked internals | Phases 3–4 |
| Observability | No new metric/span/logger. The admin API has no custom metrics today, and this change adds no long-running or background work. Structured request logging already covers the new route through the shared web module | — |
| Configuration | None added: no env var, no yaml key, no CORS change, no deployment topology change | — |

**Review checklist before closing the phase**

1. Every new route call site sits behind a guard (grep `requireRead`/`requireWrite` in
   `keystone-admin`, count the routes vs guard calls).
2. The reserved tenant id cannot reach an `INSERT`/`UPDATE`/`DELETE`: `create` mints a new id,
   `update`/`delete` reject the reserved id, and no seed writes it.
3. No `Map<String, Object>`/`JsonNode` was introduced on either side of the wire.
4. Nothing new is logged that could carry a password, token or email address.

**Dependencies** — Phases 3–5.

**Verification** — the checklist above, plus the Phase 7 slices that exercise the `403`/`422`/`409`
paths (read-only caller, reserved tenant, tenant with users).

## Execution record (2026-09-27) — review, no code changes

- **Guard coverage:** every resource route is behind a guard — `UserHandler` 5 routes / 5 guards
  (including the new `PATCH`), `TenantHandler` 4/4, `RoleHandler` 4/4, `PermissionHandler` 3/3. The
  unguarded routes are intentional and pre-existing: `/healthz`, the three `/api/v1/me*` routes (own
  profile, authenticated by `AuthFilter`) and `POST /api/v1/auth/login`.
- **Reserved tenant is unwritable:** `TenantService.create` always mints a new id
  (`idGenerator.nextId()`), `update`/`delete` reject `PLATFORM_TENANT_ID` before touching the database,
  and `create`/`update` reject the reserved slug. `UserService.tenant(...)` normalizes the reserved id
  to the platform plane, so `users.tenant_id` can only ever be a real tenant id or `NULL`. The
  predicate itself is unit-tested (`PlatformSchemaTest`) and the reserved slug guard is
  `TenantSlug.isReserved` (`TenantSlugTest`).
- **Cross-plane escalation:** `update` reuses the same `replaceRoles` validation as `assignRoles` — a
  `TENANT` role on a platform user (and vice versa) is rejected before any insert.
- **Identity integrity:** the email is absent from `UserUpdateRequest`, so `users.email` cannot drift
  from the GoTrue account `LoginService` authenticates with; `username` remains the only mutable
  identity field, and login resolves the user by lookup rather than by re-deriving the email.
- **Payload typing:** no `Map`/`JsonNode` was introduced. `grep Map<String, Object>|JsonNode` in
  `keystone-admin/src/main/java` still matches only the two pre-existing boundary classes
  (`AdminConfigLoader`, `SupabaseHttpAdminClient`) — none of the eight files this feature touched.
- **Error contract:** the new failures reuse the existing exceptions and the single RFC 9457 handler
  (`ValidationException` 422 for the reserved id/slug and a cross-plane role, `NotFoundException` 404,
  `ConflictException` 409 for "tenant has users"); no new error type, no internals leaked. The client
  shows the server's `detail` (`apiErrorMessage`).
- **Logging/PII:** the only logger call in the module is the pre-existing bootstrap line (user id +
  sub). Nothing this feature added logs, and no password, token or email is logged on either side.
- **Configuration/observability:** no new env var, yaml key, CORS entry, metric or span. No Realtime
  publication (the module does not depend on `keystone-realtime`).


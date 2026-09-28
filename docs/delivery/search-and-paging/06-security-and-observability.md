# Phase 6 — Security & observability

## Scope

Paging and search add **inputs** to read routes, so the phase is mostly a review plus four concrete
hardenings. No new permission codes, no new guards, no new secrets, no new metrics.

## Authorization

- **Every route keeps its guard and keeps it first.** The paged handlers still call
  `guard.requireRead(ctx, …)` before touching the service, and the tenant handlers still call
  `guard.callerTenantScope(ctx)` first, so the tenant stays the caller's and never a request field.
- **The three new `/options` routes reuse the read check of the list they mirror** —
  `platform:tenant:read-only`, `platform:role:read-only`, `tenant:role:read-only`. A picker can never be
  a way around a list guard, and no new code is added to `PermissionCatalog` (which would otherwise
  change the seeded `platform-admin`/`admin` grants).
- **Paging must not widen visibility.** The window is applied to a query whose `WHERE` is exactly the
  previous `tenantFilter`/`ownerFilter` condition, combined with `AND` for search and `scope`; a test
  asserts a tenant-plane search for another tenant's username returns an empty page, not that user
  (phase 7).

## Input hardening

| Input | Hardening |
| --- | --- |
| `page` | integer, `>= 0`, and **capped** (`MAX_PAGE = 10_000`) → `422`; an unbounded `OFFSET` is a cheap way to ask PostgreSQL for an expensive scan (implemented in `PageRequest`'s canonical constructor). |
| `size` | `1…100` → `422`. Bounds both the response size and the row work per request (a `size=100000` request is rejected, not served). |
| `sort` | whitelisted per resource via `switch`; an unknown key is a `422` naming the allowed keys, so no caller-supplied text can reach `ORDER BY`. |
| `order` | enum-parsed (`ASC`/`DESC`), never interpolated. |
| `scope` | parsed by `Scope.from` (existing rule, `422` on anything else). |
| `tenantId` | parsed by the existing `Ids.optionalUuid` (`422` on a malformed UUID). |
| `q` | trimmed, capped at **100 characters**, and used **only** as a bound `LIKE` parameter: jOOQ binds it (no SQL injection) and `%`, `_` and `\` are escaped with an explicit `ESCAPE '\'`, so a user cannot turn a search into a match-everything pattern or a pathological scan. |

## Data exposure

- The **envelope adds counts, not data**: `items` carry the same DTO fields as before, so no field becomes
  visible that a caller could not already read.
- `totalElements` **is** new information (how many rows exist outside the returned window). It is
  disclosed only to a caller who already passes the read guard for that resource, i.e. it can only ever
  count rows that caller may already list — no new enumeration surface.
- Error details name the *allowed sort keys* and the rejected parameter, never SQL, table or column names.

## Logging & observability

- **Never log the raw search term at `INFO`/`WARN`/`ERROR`**: `q` can contain an email address or a
  username (`UserService` searches `email`), so it is PII by construction. If a list request needs to be
  traceable, log the resource and the window (`page`, `size`, `totalElements`) at `DEBUG` — and nothing on
  the happy path, matching the current console code, which logs only on failure.
- **No new metrics or traces.** `keystone-observability`'s `MetricsModule` is bound by the hosting app
  (`inventory`'s `Main`), and this delivery adds no meters; the existing correlation-id filter already ties
  a list request to its response.
- **No realtime channel is added**, so no new authorization path (Realtime auth/RLS) and no new payload
  version to guard.

## Dependencies

- Phases 3 and 4 (the parsing and the routes being reviewed).

## Execution (2026-09-28) — review

Phase 6 is a review phase and produced one change plus four verifications. Nothing else needed code.

| Point | Outcome |
| --- | --- |
| Authorization | **Unchanged and verified.** Every paged route still calls its guard first (`requireRead` / `requireWrite`), and the tenant routes still call `guard.callerTenantScope(ctx)` **before** the guard, so the tenant stays the caller's. `PermissionCatalog` and `AdminModule`'s bindings are untouched, so no seeded grant changed. The three `/options` routes reuse the read code of the list they mirror (`platform:tenant:read-only`, `platform:role:read-only`, `tenant:role:read-only`). |
| Input hardening | `PageRequest` rejects `page < 0`, `page > 10 000`, `size < 1`, `size > 100`; `SearchTerm` caps `q` at 100 characters; `SortOrder.parse` and `Scope.optional` reject anything unrecognised; `sort` keys resolve through a per-service whitelist, so no caller text reaches `ORDER BY`. `Search.containsIgnoreCase` binds the term and escapes `%`, `_` and `\` with an explicit `ESCAPE` — asserted on the rendered SQL in `SearchTest`. |
| Data exposure | The envelope adds counts, not fields, and the DTOs are unchanged. `totalElements` is disclosed only to a caller who already passes the resource's read guard, so it can only ever count rows that caller may list. |
| Logging | **No new log statements at all** — the only `log.*` call in the touched services is the pre-existing password-reset trail. A search term is therefore never logged, which matters because `UserService` searches `email`. |
| Observability / realtime | No new metrics (`MetricsModule` is the hosting app's, and this delivery adds no meters), and no realtime channel or payload version changed. |

**One code change came out of the review:** `size` is validated in `PageRequest`'s canonical constructor
rather than in the handler, so *every* caller of the shared type gets the cap — a handler that forgot to
validate could otherwise ask for `size=100000`.

## Verification

- `QueryParamsTest` (keystone-web): `page=two`, `page=-1`, `size=0`, `size=101`, `order=sideways`,
  `sort=1name` and a 101-character `q` all answer `422`; the defaults are applied when nothing is sent.
- `AdminIntegrationTest`: the same rejections through the real embedded server, plus `q=%` matching nothing
  (a wildcard is a character) and the tenant-plane isolation that the tenant handlers already had.
- `git diff --stat platform/keystone-admin/src/main/java/.../PermissionCatalog.java
  platform/keystone-admin/src/main/java/.../AdminModule.java` → no changes.


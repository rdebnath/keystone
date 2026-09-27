# Phase 1 — Discovery & design

**Scope** — Find every place where a JSON payload is handled without a record/class, decide
which are genuine violations of `docs/CODING_GUIDELINES_BACKEND.md` §13 /
`docs/CODING_GUIDELINES_FRONTEND.md` §14, and fix the contracts before coding.

**Artifacts** — findings (below); no code.

**Dependencies** — the guidelines themselves (written first, in the previous change).

**Verification** — each finding has a decision (fix / conforming / blocked) and the affected
call sites and tests are enumerated.

## Findings

Backend (grep for `Map<String, Object>`, `JsonNode`, `Object` payloads in `platform/`,
`apps/`):

| Location | Verdict |
| --- | --- |
| `keystone-security/…/security/Principal.java` — `Map<String, Object> claims` | **fix** (public API, raw claim map) |
| `keystone-admin/…/supabase/SupabaseHttpAdminClient.java` — 3 × `Map<String, Object>` bodies, `JsonNode` responses, `findSubInPage(JsonNode, …)` | **fix** |
| `keystone-realtime/…/SupabaseRealtimePublisher.java` — `Map.of(...)` body, `RealtimePublisher.publish(…, Object payload)` | **fix** |
| `keystone-admin/…/config/AdminConfigLoader.java` — `JsonNode` helpers | **conforming** — all `private static`, resolves into `AdminConfig` records (exception 2) |
| `apps/inventory/server/…/config/ConfigLoader.java` — `JsonNode` helpers | **conforming** — same pattern, resolves into `AppConfig` records |
| `keystone-common/…/error/ProblemDetail.java` — `Map<String, String> errors` | **conforming** — dynamic field names (RFC 9457 `errors`), not a fixed payload shape |

Frontend (`grep 'Map<String, dynamic>'`):

| Location | Verdict |
| --- | --- |
| `keystone-admin-ui/lib/src/core/api_client.dart` — inline map request bodies (login, change-password, create tenant/role/permission/user) | **fix** (typed request models) |
| `…/api_client.dart` + `…/models/models.dart` — `Map<String, dynamic>` in `fromJson` | **conforming** — the one permitted conversion at the `data` boundary |
| `…/models/models.dart` — hand-written `fromJson` instead of `freezed` | **blocked** — needs `build_runner`; no Dart/Flutter SDK installed |
| `…/test/models_test.dart` — map literals passed to `fromJson` | **conforming** — JSON-shaped test data into the permitted signature |

## Affected call sites / tests (must stay compiling)

- `new Principal(subject, Map.of())` → `AuthFilterTest`, `AdminIntegrationTest`.
- `SupabaseHttpAdminClient.findSubInPage(JsonNode, …)` → `SupabaseHttpAdminClientTest`.
- `RealtimePublisher.publish(...)` → no callers in the repo (port + one implementation).
- `principal.claims()` → no callers; only `principal.subject()` is used (`MeHandler`).
- Flutter: `createTenant`/`createRole`/`createPermission`/`createUser` → 4 screens;
  `ApiClient.login`/`changePassword` → `AuthService`.

## Resolved decisions

1. Keep a modelled `Claims` record on `Principal` (not a bare `sub`) — see `plan.md`.
2. Wire records are package-private, one type per file, in the adapter's package.
3. `RealtimeEnvelope<T>` is the publish API; the payload is never an untyped `Object`.
4. Flutter request models are hand-written (no codegen available); responses keep the single
   permitted boundary conversion.

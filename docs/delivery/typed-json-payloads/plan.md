# Delivery Plan — Typed JSON Payloads (guideline conformance)

**Feature slug:** `typed-json-payloads`
**Level:** Platform-level — touches `platform/keystone-security`,
`platform/keystone-admin`, `platform/keystone-realtime`, `platform/keystone-admin-ui`, and
`docs/`.

## Sizing decision

**Produce a plan.** This removes untyped JSON payloads (`Map`/`JsonNode` on the backend, raw
`Map<String, dynamic>` bodies on the client) from the adapters named by the new
`docs/CODING_GUIDELINES_BACKEND.md` §13 / `docs/CODING_GUIDELINES_FRONTEND.md` §14 rules. It
touches multiple modules and changes two platform API surfaces (`Principal`,
`RealtimePublisher`), so it is more than a single-file tweak.

Scope was presented to the user as a concrete fix list and **confirmed** with "apply the code
fixes"; execution therefore proceeds through all applicable phases without stopping.

## Summary

| # | Violation | Fix |
| --- | --- | --- |
| 1 | `Principal(String subject, Map<String, Object> claims)` — raw claim map is public API | typed `Claims` record; the raw `JWTClaimsSet` stays inside `JwtAuthenticator` |
| 2 | `SupabaseHttpAdminClient` — `Map<String, Object>` request bodies, `JsonNode` responses, `findSubInPage(JsonNode, …)` | per-shape request/response records + `ObjectMapper.readValue(...)` |
| 3 | `SupabaseRealtimePublisher` — `Map.of("channel", …, "event", …, "payload", …)` and an untyped `Object payload` | `RealtimeEnvelope<T>` record; `RealtimePublisher.publish(RealtimeEnvelope<?>)` |
| 4 | `keystone-admin-ui` — inline `Map` request bodies in `ApiClient` | typed request models with `toJson()` |

**Already conforming (no change):** `AdminConfigLoader` and the inventory `ConfigLoader` use
`JsonNode` only in `private static` helpers and resolve into `AppConfig`/`AdminConfig`
records — the documented config-loader exception (§13, exception 2).
`ProblemDetail.errors` is a `Map<String, String>` of dynamic field names (RFC 9457 `errors`
object), which is a keyed lookup rather than a payload shape.

**Deferred, then completed:** the `freezed`/`json_serializable` conversion of the
`keystone-admin-ui` models needs `build_runner`, and no Dart/Flutter SDK was on the PATH when this
started (`dart`/`flutter` not found), so phase 5 began with codegen-free typed request models. Once
the SDK was made available the conversion was finished (see `05-frontend-ui-flutter.md`).

## Phase list

1. Discovery & design
2. Database changes — **skipped** (no schema/Liquibase change).
3. Domain & application services — `keystone-security` `Principal` + `Claims`.
4. Server-side API & realtime — `SupabaseHttpAdminClient` wire records + `RealtimeEnvelope`.
5. Frontend / UI (Flutter) — typed request models in `keystone-admin-ui`.
6. Security & observability — **skipped** (no new surface; the `Claims` change is identity
   plumbing and is covered in phase 3).
7. Testing
8. Delivery

## Resolved decisions

1. **`Principal.claims`** — keep a modelled claim set (typed `Claims`), not a bare `sub`:
   the claims are part of what an authenticated caller is.
2. **Wire records live beside the adapter** and are package-private: they are GoTrue's shapes,
   not platform API (`§4` one top-level type per file, so one file each).
3. **Realtime API** takes the envelope record rather than three loose arguments, so the
   payload cannot be an untyped `Object` at the call site.
4. **Flutter models** are `freezed` + `json_serializable` (§1 of the frontend guidelines); the
   request models are typed classes, and responses keep the single permitted
   `Map<String, dynamic>` conversion at the `data` boundary.

## Confirmation state

- [x] Plan reviewed — scope confirmed by the user ("apply the code fixes").
- [x] Executed — phases delivered.

## Execution summary

- **Phase 3** — `Claims` record added; `Principal(String, Claims)`; `JwtAuthenticator` maps
  `sub`/`email`/`role`/`iat`/`exp` into it and no longer exposes `getClaims()`.
- **Phase 4** — 6 package-private GoTrue wire records (`CreateUserRequest`,
  `PasswordGrantRequest`, `UpdatePasswordRequest`, `GoTrueUser`, `ListUsersPage`,
  `TokenResponse`); `SupabaseHttpAdminClient` serializes/parses through them;
  `RealtimeEnvelope<T>` replaces the `Map.of(...)` body.
- **Phase 5** — `lib/src/models/requests.dart` (6 typed request models), `ApiClient` methods take
  them, 4 screen call sites updated, barrel exports updated; then completed with the
  `freezed`/`json_serializable` migration (deps added, SDK `>=3.8.0`, generated code committed) and
  `Session` moved into `models.dart`. Verified with `flutter analyze` (clean) + `flutter test` (7/7).
- **Phase 7** — `SupabaseHttpAdminClientTest` reworked onto the typed page record;
  `JwtAuthenticatorTest`, `AuthFilterTest`, `AdminIntegrationTest` updated for the new
  `Principal`. `mvn test` result recorded in `07-testing.md`.
- **Phase 8** — guidelines (§13/§14) were written first; `CHANGELOG.md` notes the change.

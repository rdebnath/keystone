# Phase 7 — Testing

**Scope** — Keep every existing test green against the new contracts and add coverage for the wire
shapes that replaced the maps.

**Artifacts**
- `platform/keystone-admin/src/test/…/supabase/SupabaseHttpAdminClientTest.java` (modify) — parses
  the GoTrue page into `ListUsersPage` and asserts `findSub(...)` returns
  `Optional` (`contains`/`isEmpty`); the fixture keeps the unknown `aud`/`next_page` fields, so the
  test also proves unknown-field tolerance.
- `platform/keystone-admin/src/test/…/supabase/GoTrueWireRecordsTest.java` (new) — 5 tests asserting
  the exact request JSON (`email_confirm`), the snake_case token response
  (`access_token`/`expires_in`), and a page with no `users` array.
- `platform/keystone-realtime/src/test/…/RealtimeEnvelopeTest.java` (new) — asserts the broadcast
  body is `{"channel":…,"event":…,"payload":…}` with a record payload.
- `platform/keystone-admin/src/test/…/auth/AuthFilterTest.java` and `…/AdminIntegrationTest.java`
  (modify) — `new Principal(subject, Claims.empty())`.
- `platform/keystone-security/src/test/…/JwtAuthenticatorTest.java` — unchanged; still covers
  subject extraction and the signature/audience/expiry rejections against the new `toClaims(...)`
  mapping.

**Dependencies** — Phases 3, 4, 5.

**Verification** — `mvn -o test` for `keystone-security`, `keystone-realtime`, `keystone-admin`.

## Results

Targeted run (`mvn -o -pl platform/keystone-security,platform/keystone-realtime,platform/keystone-admin -am … test`):

| Test class | Result |
| --- | --- |
| `JwtAuthenticatorTest` | 4 / 4 pass |
| `AuthFilterTest` | 3 / 3 pass |
| `SupabaseHttpAdminClientTest` | 2 / 2 pass |
| `GoTrueWireRecordsTest` | 5 / 5 pass |
| `RealtimeEnvelopeTest` | 1 / 1 pass |
| `SupabaseAdminClientTest`, `AdminConfigLoaderTest` | 2 / 2, 4 / 4 pass |
| **Total** | **BUILD SUCCESS**, 0 failures |

Also verified: `mvn -o … -am test-compile` for the whole platform + admin graph.

### Flutter (run once a Dart/Flutter SDK was available — Flutter 3.47.5 / Dart 3.13.4)

| Check | Result |
| --- | --- |
| `flutter analyze` (`platform/keystone-admin-ui`) | **No issues found** |
| `flutter analyze` (`apps/inventory/frontend`, the hosting app) | **No issues found** |
| `flutter test` (`platform/keystone-admin-ui`) | **7 / 7 pass** |
| `dart run build_runner build --delete-conflicting-outputs` | 6 outputs written, no warnings (after the SDK-constraint fix) |
| `dart format` on the files authored/rewritten here | clean |

`models_test.dart` passed unchanged through the freezed conversion: it exercises `Me`
(`isPlatformAdmin`), `Tenant`, `User` and `Session` through `fromJson`, which is exactly the
behaviour the previous hand-written classes provided.

**Not run:** `AdminIntegrationTest` / `InventoryIntegrationTest` (Testcontainers/Docker, excluded
from the targeted run).

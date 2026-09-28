# Phase 7 — Testing

## Scope

Unit tests for the two value classes, schema assertions against real PostgreSQL, and HTTP round-trip
coverage on both planes.

## Artifacts

| Artifact | Change |
| --- | --- |
| `…/admin/tenant/CountryTest.java` | new — normalization, canonical codes, rejected values |
| `…/admin/user/PhoneNumberTest.java` | new — E.164 boundaries, separator normalization, rejection |
| `…/admin/TenantUserProfileSchemaTest.java` | new — the two columns exist on real PostgreSQL, are nullable, and accept `NULL` |
| `…/admin/AdminIntegrationTest.java` | a tenant created/patched with a country and a user created/patched with a phone round-trip; an invalid country/phone answers `422` |
| `…/admin/TenantSelfServiceIntegrationTest.java` | the tenant plane can set its own user's phone (and cannot set a country, which has no tenant route) |
| `keystone-admin-ui/test/models_test.dart`, `test/user_editor_test.dart` | Dart cases for the two new fields (added, not run — no toolchain) |

## Test plan

**Unit — `Country`**

| Input | Expected |
| --- | --- |
| `"in"` / `" IN "` | `"IN"` |
| `"DE"` | `"DE"` |
| `null`, `""`, `"  "` | `null` (via `normalizeOptional`) |
| `"IND"`, `"I1"`, `"ZZ"`, `"1N"` | `ValidationException` |
| a value from `Locale.IsoCountryCode.PART1_ALPHA2` | valid (pins the list the rule reads) |

**Unit — `PhoneNumber`**

| Input | Expected |
| --- | --- |
| `"+919876543210"` | unchanged |
| `"+91 98765 43210"`, `"+91-98765-43210"`, `"(+91) 98765.43210"` | `"+919876543210"` |
| `null`, `""` | `null` (via `normalizeOptional`) |
| `"+123456"` (too short), `"+0123456789"` (leading zero), `"919876543210"` (no `+`), `"+919876543210123456"` (over 15 digits), `"phone"` | `ValidationException` |

**Schema (Testcontainers PostgreSQL 17)** — `tenants.country` and `users.phone_number` exist with an
insertable `NULL`, so an upgrade of a populated database cannot fail on a missing default.

**HTTP (embedded server + Testcontainers)** — the payload grows without breaking the existing calls:
a tenant created without `country` still returns `country: null`, and the two new fields survive a
create → list → patch round trip.

## Dependencies

- Phases 2–5.

## Execution (2026-09-28)

Written as planned:

- `CountryTest` (5 cases) and `PhoneNumberTest` (6 cases) — the tables above, with the E.164 length
  boundaries (`+1234567` and `+123456789012345` accepted; `+123456` and `+1234567890123456` rejected)
  and the ISO list pinned via `Locale.getISOCountries(IsoCountryCode.PART1_ALPHA2)`.
- `TenantUserProfileSchemaTest` (4 cases) — the new nullable columns, their lengths, and a
  tenant/user insert without either value plus a record/clear round trip on the phone number.
- `AdminIntegrationTest` — country on create (`in` → `IN`), the country in the list with the platform
  tenant reading `null`, a `PATCH` to `gb` → `GB`, the phone on create (`+91 98765 43210` →
  `+919876543210`), a `PATCH` to `+14155552671`, and `422` for `IND` (create and update) and for
  `12345`.
- `TenantSelfServiceIntegrationTest` — the tenant plane records a phone on its own user and rejects a
  value that is not E.164.
- Dart: 6 new cases plus the two updated payload assertions (which now include `phoneNumber`).

All of it ran with Docker available, so nothing was skipped. `resultSetId`/paging, no.

## Verification

- `mvn test` (whole reactor) → **BUILD SUCCESS**; `keystone-admin`: **88 tests, 0 failures, 0 skipped**
  (73 before, +15 here); `inventory-server`: 15 tests.
- `flutter analyze` → no issues; `flutter test` (keystone-admin-ui) → **66 passed**;
  `flutter analyze` (apps/inventory/frontend) → no issues.


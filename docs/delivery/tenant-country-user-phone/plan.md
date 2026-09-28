# Delivery Plan — Tenant country & user phone number

**Feature slug:** `tenant-country-user-phone`
**Level:** Platform-level — touches `platform/keystone-admin` (schema, domain, REST contract),
`platform/keystone-admin-ui` (Flutter models + console screens) and `docs/`.

## Sizing decision

**Produce a plan.** Two new columns on two core tables (plus the master-changelog include), two new
validated value classes, changes to three request records and two DTO records (the REST contract),
the Flutter models/requests *and* their generated `freezed`/`json_serializable` files, two console
dialogs and their tests.

## Summary

| # | Requirement | Column | Type | Null | REST contract |
| --- | --- | --- | --- | --- | --- |
| 1 | Country for a tenant | `tenants.country` | `VARCHAR(2)` | yes | `TenantDto.country`, `TenantRequest.country` |
| 2 | Phone number for a user | `users.phone_number` | `VARCHAR(32)` | yes | `UserDto.phoneNumber`, `UserRequest.phoneNumber`, `UserUpdateRequest.phoneNumber` |

Both are **optional**: every existing row stays valid (no backfill, no `NOT NULL`), and neither is
required to administer a tenant or a user. Both are **validated server-side** and normalized before
they are stored:

- `country` — an **ISO 3166-1 alpha-2** code (`IN`, `DE`, `US`), trimmed and upper-cased; blank or
  absent clears it. Validated against the JDK's `Locale.IsoCountryCode.PART1_ALPHA2` list, the same
  "small final validator class" shape as `TenantSlug` / `Username`.
- `phone_number` — **E.164** (`+919876543210`): a leading `+`, then 7–15 digits (the E.164 maximum),
  first digit non-zero. Separators (spaces, hyphens, dots, parentheses) are stripped before
  validation so pasted numbers are accepted; anything else is a `422`.

**Dropped:** the tenant `home_directory` requirement was withdrawn by the requester on 2026-09-28
before any code was written — no column, no configuration, no UI. It is not planned here.

### Non-goals

- **No `/api/v1/me` change.** `MeDto` keeps its current shape: the console renders only
  `me.username` (`admin_shell.dart`) plus permissions, so exposing the caller's own phone there
  would be speculative.
- **No index / filter / ordering** on either column — no query uses them yet, and an index that
  serves no query is speculative (`docs/delivery/tenant-scoped-rbac/phase 2` sets the precedent of
  indexing *per query the code runs*).
- **No country↔phone cross-validation.** A phone's dialling code need not match the tenant's
  country; that rule would reject legitimate international tenants.
- **No Supabase Auth metadata sync.** Phone is a platform-side attribute, not a credential — login
  stays `username@tenantid` + password, and Supabase remains unaware of it.
- **Not a tenant-admin-editable field.** Tenants and their country are managed on the platform
  plane only (the tenant self-service plane has no tenant routes); the user phone is editable on
  both planes by whoever may write the user.

## Decisions

| Question | Decision |
| --- | --- |
| `country` required or optional? | **Optional** (`NULL` = not recorded). A required field would either break every existing tenant or need a meaningless backfill. |
| `country` shape — code or free text? | **ISO 3166-1 alpha-2 code**, stored upper-case. A free-text country name is unqueryable and unbounded; the console offers the codes. |
| `phone_number` required or unique? | **Optional and non-unique.** A shared/landline number must not block provisioning, and uniqueness across tenants would leak nothing useful. |
| `phone_number` format | **E.164**, normalized on write. One canonical stored form keeps a later "send SMS" / "dial" consumer trivial. |
| Is `country` cleared by an update that omits it? | **Yes** — `PATCH /api/v1/tenants/{id}` takes the same full body as create (as it already does for `name`/`slug`), so an absent/blank `country` clears it. `phoneNumber` behaves the same way on `PATCH /api/v1/users/{id}`. |

## Phases

| Phase | File | Applies |
| --- | --- | --- |
| 1. Discovery & design | `01-discovery-and-design.md` | yes |
| 2. Database changes | `02-database-changes.md` | yes |
| 3. Domain & application services | `03-domain-and-application-services.md` | yes |
| 4. Server-side API & realtime | `04-server-side-api-and-realtime.md` | yes (contract only — no handler/realtime code) |
| 5. Frontend / UI (Flutter) | `05-frontend-ui-flutter.md` | yes |
| 6. Security & observability | `06-security-and-observability.md` | yes (review) |
| 7. Testing | `07-testing.md` | yes |
| 8. Delivery | `08-delivery.md` | yes |

## Confirmation state

- [x] `home_directory` dropped by the requester — 2026-09-28.
- [x] "Proceed with other things" — 2026-09-28 (plan written and executed in the same pass).

## Execution summary (2026-09-28)

All phases executed in one pass. Both fields ship end to end.

| Phase | Outcome |
| --- | --- |
| 2 — database | `0004-tenant-country-user-phone.xml` (2 changesets, guarded on `tableExists` + `!columnExists`) + master include; offline jOOQ codegen regenerated `TENANTS.COUNTRY` / `USERS.PHONE_NUMBER`. |
| 3 — domain & services | New `tenant.Country` and `user.PhoneNumber` value classes; `TenantDto`/`TenantRequest`/`TenantService` and `UserDto`/`UserRequest`/`UserUpdateRequest`/`UserService` carry the fields. |
| 4 — API | No handler changed (verified): `bodyAsClass` binds the new components and Jackson serializes them. |
| 5 — frontend | Models + requests edited, `build_runner` regenerated the generated code, tenant dialog and user editor gained the fields. |
| 6 — security | Review only: validation stays server-side, nothing new is logged, no new configuration. |
| 7 — testing | 15 new Java tests (5 country, 6 phone, 4 schema) plus new HTTP round-trip/`422` assertions on both planes; 6 new Dart tests and 2 updated payload assertions. |
| 8 — delivery | `CHANGELOG.md` entry; no manual migration step. |

**Verification:** `mvn test` (whole reactor) → **BUILD SUCCESS** — `keystone-admin` **88 tests, 0
failures, 0 skipped** (was 73) and `inventory-server` **15 tests** (the app-level `SchemaToolTest`
still migrates an externally-initialized schema idempotently against the new changesets). Docker was
available, so the Testcontainers suites really ran.

`flutter analyze` → no issues; `flutter test` in `platform/keystone-admin-ui` → **66 tests passed**;
`flutter analyze` in `apps/inventory/frontend` → no issues.

### Deviations and decisions

- **`home_directory` dropped** before any code was written (requester, 2026-09-28) — no column, no
  configuration, no UI.
- **Both columns are nullable with no backfill.** A `2026-09-28` decision: a required country would
  either break every existing tenant or need a meaningless backfill, and neither field is required to
  administer a tenant or a user.
- **`GET /api/v1/me` was left unchanged** (`MeDto` untouched): the console renders only `me.username`
  plus permissions, so exposing the caller's own phone there would be speculative.
- **The `Country` validator reads `Locale.getISOCountries(IsoCountryCode.PART1_ALPHA2)`** (249
  officially assigned codes) rather than a hand-maintained list, so the accepted set follows the JDK —
  pinned by `CountryTest`.
- **The country field is a two-letter text field, not a dropdown.** A dropdown would mean committing a
  hand-copied ~250-entry ISO list (or a package) to the client for a rule the server already enforces.
- **`TenantUserProfileSchemaTest` uses jOOQ `into(Class)`** rather than `Result.getValue(int, Class)`,
  which is not a jOOQ API — the same idiom `TenantScopedRbacSchemaTest` already uses for
  `information_schema`/`pg_indexes` queries.

### Follow-ups

- None blocking. The console's new fields are covered by widget tests but not by a manual walk-through
  against a live Supabase project (no environment here).


# Phase 3 — Domain & application services

## Scope

Two pure value classes that own the value rules, plus the DTO/request and service wiring that carries
them in and out of the database.

## Artifacts

| Artifact | Change |
| --- | --- |
| `…platform.admin.tenant.Country` | new — `normalize(String)`, `normalizeOptional(String)`, `isValid(String)`, `codes()` |
| `…platform.admin.user.PhoneNumber` | new — `normalize(String)`, `normalizeOptional(String)` |
| `…platform.admin.tenant.TenantRequest` | `+ String country` |
| `…platform.admin.tenant.TenantDto` | `+ String country` (after `slug`) |
| `…platform.admin.tenant.TenantService` | store/normalize `country` on create + update; read it in `list()`; `platformTenant()` reports `null` |
| `…platform.admin.user.UserRequest` | `+ String phoneNumber` (after `email`) |
| `…platform.admin.user.UserUpdateRequest` | `+ String phoneNumber` (after `username`) |
| `…platform.admin.user.UserDto` | `+ String phoneNumber` (after `email`) |
| `…platform.admin.user.UserService` | normalize `phoneNumber` on create + update; read it in `list()` |

## Design notes

- **Value classes, not inline checks.** `TenantSlug` and `Username` already put a value's rules in a
  small `final` class with a private constructor and a static `normalize` that throws
  `ValidationException`; `Country` and `PhoneNumber` follow that shape exactly, so the rule is
  testable without a Guice injector and is applied at a single call site.
- **`normalizeOptional` semantics.** `country`/`phoneNumber` are optional, so the service calls
  `normalizeOptional` — `null` or blank becomes `null` (clearing the value), anything else is
  validated. The strict `normalize` remains available for a future required usage and for tests.
- **Uppercasing is a normalizing write, not a display trick.** `in` and `IN` both store `IN`, so the
  stored form is the canonical ISO code and a later filter/join on the column is exact.
- **Separator stripping on phone numbers.** Users paste `+91 98765 43210`; rejecting that would be
  hostility rather than validation. Only the E.164 punctuation is removed; any remaining character
  outside `+`/digits is a `422`.
- Both values are written through the existing `USERS`/`TENANTS` jOOQ insert/update statements, so no
  new transaction, port or repository is introduced — the services keep single transactions at their
  own boundary.

## Dependencies

- Phase 2 (the generated `TENANTS.COUNTRY` / `USERS.PHONE_NUMBER` fields must exist).

## Execution (2026-09-28)

`tenant/Country.java` and `user/PhoneNumber.java` were added in the `TenantSlug`/`Username` shape
(`final` class, private constructor, static `normalize` / `normalizeOptional` throwing
`ValidationException`). `TenantService` gained a `validateCountry(TenantRequest)` helper next to
`validateName`/`validateSlug`, and both write paths (`create`, `update`) as well as `list()` and
`platformTenant()` carry the value; `UserService` normalizes the phone in `create`/`update` and reads
it in `list()`.

Two details worth noting:

- `Country.isValid` calls `Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2)` (the modern,
  non-deprecated overload) on every call; the JDK returns the 249 officially assigned codes, verified
  with a scratch program (`IN/DE/US/GB` present, `ZZ/IND/I1` absent).
- The nullable insert/update paths go through jOOQ's typed `values(...)`/`set(...)`, which render a
  Java `null` as SQL `NULL` — exercised for real by the integration tests (a tenant created without a
  country reads back `"country":null`).

No deviation.

## Verification

- `mvn test` → BUILD SUCCESS; `CountryTest` (5) and `PhoneNumberTest` (6) pin the rules, and the HTTP
  integration tests prove the services store and return both values.


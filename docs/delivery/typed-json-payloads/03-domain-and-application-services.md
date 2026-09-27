# Phase 3 — Domain & application services (`Principal` / `Claims`)

**Scope** — Remove the raw JWT claim map from the platform's identity API.

**Artifacts**
- `platform/keystone-security/src/main/java/com/chetana/keystone/security/Claims.java` (new) —
  `record Claims(String email, String role, Instant issuedAt, Instant expiresAt)` +
  `Claims.empty()`.
- `platform/keystone-security/src/main/java/com/chetana/keystone/security/Principal.java`
  (modify) — `record Principal(String subject, Claims claims)`.
- `platform/keystone-security/src/main/java/com/chetana/keystone/security/JwtAuthenticator.java`
  (modify) — map `sub`/`email`/`role`/`iat`/`exp` into `Claims`; stop passing
  `JWTClaimsSet.getClaims()` out.

**Dependencies** — Phase 1.

**Verification** — `mvn -pl platform/keystone-security test` green;
`JwtAuthenticatorTest` still asserts the subject and the rejection paths.

## Notes

- `JWTClaimsSet.getClaim(String)` (returns `Object`) is used for the optional string claims so
  no checked `ParseException` leaks; a missing claim maps to `null`, which is the record's
  "absent" representation (`Optional` is not allowed in record components, §3).
- `Claims` models only what the platform acts on; provider-specific metadata
  (`app_metadata`, `user_metadata`) is deliberately not surfaced.

## Results

- `Claims` added; `Principal` now carries it; `JwtAuthenticator.toClaims(...)` +
  `toInstant(Date)` added and `new Principal(claims.getSubject(), claims.getClaims())`
  replaced. `java.util.Map` import dropped from `Principal`.
- `Claims.empty()` added so token doubles in tests stay one-liners.
- Verified: `mvn -o -pl platform/keystone-security test` — see `07-testing.md`.

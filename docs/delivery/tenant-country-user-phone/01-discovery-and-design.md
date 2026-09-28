# Phase 1 — Discovery & design

## Scope

Confirm the requirements, the storage shape, and the wire contracts before building. No production
code in this phase.

## Requirements as received

1. Add a **country** field for a tenant.
2. Add a **home directory** for a tenant where the server will download and process files — *withdrawn
   by the requester on 2026-09-28 ("forget about home directory requirement, proceed with other
   things")*, so it is out of scope for this delivery.
3. Add a **phone number** field for a user.

## Design

### Storage

| Table | Column | Type | Null | Rationale |
| --- | --- | --- | --- | --- |
| `tenants` | `country` | `VARCHAR(2)` | yes | An ISO 3166-1 alpha-2 code is exactly two characters, so the column is self-documenting and a code cannot be silently truncated. |
| `users` | `phone_number` | `VARCHAR(32)` | yes | E.164 is at most 16 characters (`+` + 15 digits); 32 leaves room for a human-formatted legacy value to be stored without truncation while the API normalizes new writes. `VARCHAR` (not `CHAR`) because the value is absent for most rows. |

No index on either column: nothing filters, joins, orders or constrains on them.

### Contracts

`TenantDto` / `TenantRequest` (`platform/keystone-admin`, package `…platform.admin.tenant`):

```java
public record TenantDto(
        UUID id, String name, String slug, String country, boolean platform,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

public record TenantRequest(String name, String slug, String country) {}
```

`UserDto` / `UserRequest` / `UserUpdateRequest` (package `…platform.admin.user`):

```java
public record UserDto(
        UUID id, String sub, String username, String email, String phoneNumber, UUID tenantId,
        boolean mustChangePassword, List<String> roles,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

public record UserRequest(
        String username, UUID tenantId, String email, String phoneNumber,
        String temporaryPassword, List<String> roles) {}

public record UserUpdateRequest(String username, String phoneNumber, List<String> roles) {}
```

`country` / `phoneNumber` sit next to the other descriptive fields (`slug`; `email`) and are plain
`String` components — `null` is the "not recorded" state, and every record stays small enough that
no builder is needed (`docs/CODING_GUIDELINES_BACKEND.md` §5 exceeds seven parameters).

### Validation (server-side, authoritative)

| Value | Rule | Failure |
| --- | --- | --- |
| `country` | trimmed, upper-cased, must be in `Locale.IsoCountryCode.PART1_ALPHA2`; blank/absent → `null` | `ValidationException` → `422` |
| `phoneNumber` | separators (` -().`/tabs) stripped, then `+[1-9]\d{6,14}`; blank/absent → `null` | `ValidationException` → `422` |

A rejected value answers `422` and a value that is merely absent is not rejected — the existing
`ValidationException` → problem+json mapping (`docs/CODING_GUIDELINES_BACKEND.md` §9) needs no change.

### UI flows

- **Tenants screen** — the create/edit dialog gains a `Country` field: a two-letter text field with the
  `ISO 3166-1 alpha-2 code, e.g. IN` hint, upper-cased on the way out, plus an explicit "not set"
  (empty) state; the row subtitle shows the code when present. The **server remains the authority**
  on which codes exist (it validates against the JDK's ISO list and answers `422`), exactly as the
  console already treats every other rule — client validation here is UX, not security.
- **User editor** — a `Phone number` field on both the create and edit forms (hint `+919876543210`,
  normalized to E.164 by the server), and the row subtitle shows it when present.
- The tenant plane inherits the user-editor change for free: it renders the same `UserEditor`.

## Artifacts

| Artifact | Change |
| --- | --- |
| `docs/delivery/tenant-country-user-phone/plan.md` + phase docs | created |
| Delivery phases 2–8 | as per each phase file |

## Dependencies

- None upstream. Phases 2 → 3 → 4/5 depend on each other in that order (the generated jOOQ classes
  must carry the new columns before the services compile).

## Verification

- The design decisions in `plan.md` are stated as a table with the alternative rejected, so a
  reviewer can challenge a single row rather than the whole shape.
- Contract deltas are written out above; phase 4 asserts the handlers need no code change.
- No code exists yet, so there is nothing to run in this phase.

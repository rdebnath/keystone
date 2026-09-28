# Phase 6 — Security & observability

## Scope

Review the change against the security and observability rules; implement the (few) things it needs.
This phase adds no configuration, no logging and no metrics, so it is mostly a review with two
concrete rules the code must honour.

## Rules the change must satisfy

| Rule | How this change satisfies it |
| --- | --- |
| The **server** validates inputs; the client is never the authority | `Country` / `PhoneNumber` normalize and reject on the write path of `TenantService`/`UserService`; a `422` problem+json is returned. The console's shape check only prevents obvious typos. |
| A rejected value answers **`422`**, never `400` | `ValidationException` (already mapped by the global handler) is what both validators throw. |
| **No secrets or PII in logs** | Neither field is logged. The change adds no `log.*` call, and the existing user logging (if any) is untouched, so no phone number can reach a log line. |
| **Authorization is unchanged and enforced server-side** | The routes keep their guards (`platform:tenant:*` / `platform:user:*`, and the tenant plane's `tenant:user:*`); a new field never widens who may write a row. The tenant plane still derives the tenant from the caller. |
| **No new configuration surface** | No yaml key, no env var, no secret. |
| **Cross-tenant leakage** | Both fields travel only inside the DTO of a row the caller may already see; `UserService`'s plane checks (`tenantFilter`, `requireVisibleUser`) are untouched. |

## Explicitly considered and rejected

- **Validating the phone against the tenant's country** — a legitimate international tenant or a
  foreign mobile number would be rejected; it is a data-quality nicety with a real false-positive
  cost, and it is not a security property.
- **Free-text country** — an unbounded client-supplied string that later feeds reporting; the ISO
  code list keeps the column a closed set without a database `CHECK` that would need a migration to
  extend.
- **Exposing the phone on `/api/v1/me`** — the console does not need it, and a smaller token/profile
  payload is the safer default.

## Artifacts

| Artifact | Change |
| --- | --- |
| `Country`, `PhoneNumber` | the validation this phase relies on (phase 3) |
| everything else | none — verified rather than changed |

## Dependencies

- Phase 3.

## Execution (2026-09-28)

Review only — nothing in this phase required a code change: no `log.*` call was added, the two
validators throw rather than coerce (the single coercion is the documented case/whitespace/separator
normalization), no yaml key or environment variable was introduced, and the route guards are
untouched (`platform:tenant:*` / `platform:user:*`, `tenant:user:*`). The tenant plane continues to
derive the tenant from the caller's own user row, and `UserService`'s `tenantFilter` /
`requireVisibleUser` checks are unchanged, so the new fields cannot widen what a caller sees.

## Verification

- `AuthFilterTest`, `PermissionGuardTest` and `AccessTest` still pass unchanged inside the 88-test run,
  proving no guard was loosened by the record changes.
- The two validators' messages contain only the caller's own submitted value (never another row's
  data) and are returned to that caller as a `422` problem+json.


# Phase 6 — Security & observability

**Scope** — review the one change that touches authorization: the grant condition. The filter is read-only and
scoped by the same guards as before.

**Artifacts** — no new artifact on the server; on the console, the picker's seed and the removed owner filter
(phase 5), plus `PermissionSelection`'s role-owner rule.

**Dependencies** — phases 3–5.

**Verification** — the review below; `AdminIntegrationTest` proves the guard's two halves (a tenant-owned
permission accepted for that tenant's role, refused for a global one) against the real database and guard.

## The guard: narrower, and that is the point

| Question | Before | After |
| --- | --- | --- |
| What may a **tenant-owned** role hold? | the catalogue + its own tenant's rows | unchanged |
| What may a **global** role hold? | *anything of the right scope*, including another tenant's own permission | the catalogue only |
| Which caller is asked? | the role's owner, **via a helper built for listing** — so `owner == null` meant "no restriction" | the role's owner, explicitly (`grantableTo`) |
| Who may still set the grants? | `guard.requireWrite(<plane>:role)` + `CallerScope.requireGrantable` | unchanged |

Three properties worth stating:

1. **It closes a cross-tenant leak, not a privilege.** A global role is held by *every* tenant that has been
   assigned it; a tenant-owned permission inside it would have made tenant A's own permission visible to tenants
   B, C and D. The fix removes that path and nothing else.
2. **It cannot escalate anyone.** The condition only ever *removes* rows from the set a grant may reference; the
   caller-side guardrail (`requireGrantable`: never the wildcard, never a code the caller does not hold) is
   untouched, and the platform plane remains exempt from it as before.
3. **Nothing existing was rewritten.** No rows were migrated: a global role that already carries such a grant
   keeps it until its next update, and an update now refuses to carry it forward — recorded as an open question
   rather than silently repaired (`plan.md` → open question 3).

## The filter

`access` is a **read-only narrowing** on list routes that were already gated (`requireRead` on the plane's
permission resource) and already scoped by `effectiveOwner`, which a tenant caller cannot widen. It adds no
parameter a caller could use to see more than before: `access` can only remove rows from the page and the count.

## Observability

Unchanged by design: no new log line, metric, trace attribute, alert or realtime message. The new `422` for an
invalid level travels the existing `ProblemDetailMapper` path, so it is as visible (and as structured) as every
other validation failure.

# Phase 8 — Delivery

**Scope** — write down what shipped, the one ordering constraint this delivery *does* have, and what remains
open.

**Artifacts**

| Artifact | Change |
| --- | --- |
| `CHANGELOG.md` | `### Added` — the `access` filter on both permission lists + the console's *Level* filter. `### Security` — a global role may no longer hold a tenant-owned permission. |
| `docs/CODING_GUIDELINES_BACKEND.md` | §8's resource-filter row now names `access` among its examples. |
| `docs/UX_GUIDELINES.md` | **Unchanged.** §1.6 (filter on what the row shows) and §1.17 (a picker's seed must make every offered choice valid) already say exactly what this delivery implements. |
| `docs/ARCHITECTURE.md` | **Unchanged.** §9.5's guardrails describe the delegated-administration model; the guard change closes a leak the model already implies ("a tenant's rows are its own"), and no new rule or component appeared. |
| `docs/delivery/role-permission-picker/08-delivery.md` | Its two code follow-ups are marked **delivered**, with the superseded reasoning named. |

**Dependencies** — phases 3–7 complete and green.

**Verification** — `CHANGELOG.md` reviewed against the diff; the guideline citations re-read against the code
(§1.6: the level is on every row; §1.17: the picker's seed is the role's own owner or the catalogue).

## Deployment order (the one constraint)

Unlike the delivery this follows, this one **does** have an ordering preference — because it adds a query
parameter rather than only changing how one is sent:

| Order | Effect |
| --- | --- |
| **Server first** (recommended) | The new parameter is served; the console (old or new) is unaffected either way. |
| Client first | The old server ignores the unknown parameter (Javalin does not reject what it does not read), so the *Level* filter simply does nothing until the server is out. Nothing breaks; the control under-delivers. |
| Server only | Everything the API needs; the console's filter appears with its next release. |

Either way there is **no migration and no data change**: no Liquibase changeset, no jOOQ regeneration, no
environment variable. `platform/keystone-admin` deploys by Jib → Cloud Run as usual; the console by
`flutter build web` → Firebase Hosting (or an app-store build).

## Follow-ups (recorded, not blockers)

1. **The manual click-through** against a live deployment still needs dev credentials (a Supabase URL and
   service-role key) that this environment does not have. The Testcontainers suites are the substitute and are
   recorded in `07-testing.md` with their exact commands and results.
2. **Global roles that already carry a tenant-owned permission** may have been created before this guard. Such a
   role still works, but any update now refuses to carry that grant forward, so an operator must edit it to drop
   the code. A one-off report (or a cleanup) was not requested; the query would be a `roles ⋈ role_permissions ⋈
   permissions` join on `roles.tenant_id IS NULL AND permissions.tenant_id IS NOT NULL`.
3. **A `code text_pattern_ops` index** if a permission catalogue ever grows large enough for the suffix match to
   matter — a performance follow-up, not a correctness one (`04-server-side-api-and-realtime.md`).

# Phase 1 — Discovery & design

**Scope** — turn the three follow-ups into an unambiguous requirement, fix the contract for the new filter and
the rule for the tightened guard, and recognise what the guard does to the console that was just built.

**Artifacts** — this file; `plan.md` (the phase list, decisions and execution log).

**Dependencies** — `docs/delivery/role-permission-picker/` (the delivery that recorded these follow-ups), whose
phase-1 grantability table is *superseded in one row* by this work.

**Verification** — every criterion below maps to a test in `07-testing.md`; the contract is verified by the two
Testcontainers suites, and the console's behaviour by the Flutter suites.

## Acceptance criteria

| # | Criterion |
| --- | --- |
| A1 | `GET /api/v1/permissions` and `GET /api/v1/tenant/permissions` accept `access=read-only`/`access=read-write`, matched as the **code's last segment**; the wildcard `*` appears in neither level's set. |
| A2 | An absent `access` filters nothing; a present-but-invalid one is a `422` naming the two levels — never a silently ignored parameter. |
| A3 | `access` composes with the existing filters (`tenantId`, `scope`, `q`), the sort and the page window: count and window share one `WHERE`. |
| A4 | The console offers a *Level* filter on the Permissions screen and in the role picker, on **both** planes, and the URL carries it like `scope` (`access=…`). |
| A5 | A role may hold a permission whose owner is the role's own owner, or a global one — and a role with **no** owner may hold the catalogue only. A code a role may not hold is refused with a message that says why. |
| A6 | The console's picker mirrors A5: a global role is seeded with the catalogue (the reserved platform tenant), its owner filter is gone, and a pick that a global role may not hold is dropped rather than sent. |

## The contract added

| Parameter | Route(s) | Values | Absent | Invalid |
| --- | --- | --- | --- | --- |
| `access` | both permissions lists | `read-only`, `read-write` (the code suffix; case-insensitive, trimmed) | no filter | `422` "access must be read-only or read-write" |

## The rule changed

`RoleService.grantPermissions` previously selected the permission row with `ownerFilter(owner, …)` — the same
helper the **list** queries use, where `owner == null` means *no restriction*. For a grant that is wrong: the
question is not "what may this caller see" but "what may this role hold". It is now `grantableTo(owner)`:

```
owner == null            ->  PERMISSIONS.TENANT_ID IS NULL                 (the catalogue only)
owner == <a tenant id>   ->  PERMISSIONS.TENANT_ID IS NULL OR = owner      (the catalogue, plus its own)
```

The rationale is a leak, not a preference: a **global** role's grants are held by every tenant that holds it, so
a tenant-owned code would expose one tenant's own permission to all the others.

## What this supersedes

`role-permission-picker/plan.md`'s owner/scope table described a global `TENANT`-scope role as offering "the
global catalogue **plus** each tenant's rows — all grantable, per `ownerFilter(null)`", and decision 2 kept an
owner filter alive for that single case. Under the new rule that case disappears: the picker's seed is the
catalogue, and the owner filter is removed. The old documents are left as the historical record, with the
follow-up list in `role-permission-picker/08-delivery.md` now marking both items delivered.

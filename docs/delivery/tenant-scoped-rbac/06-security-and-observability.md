# Phase 6 — Security & observability

> **Reviewed & executed 2026-09-27.** Deny paths, escalation, fail-closed behaviour and the absence of a
> silent global fallback were reviewed as planned; no new logging, metrics or configuration was added. One
> extension found while implementing: the **wildcard permission** is refused deletion outright — removing
> it would strip the platform admin of everything until the next bootstrap, which is the same lockout
> class the seeded-role protection closes. Verified by `TenantSelfServiceIntegrationTest` (another
> tenant's rows are `404`, a global row is `403`, the platform plane is refused on tenant routes).

**Scope** — review the authorization and audit impact; add nothing that is not needed.

**Artifacts** — none expected (review only); any finding is fixed in the phase that owns it.

**Dependencies** — phases 2–5.

**Verification** — the deny/escalation matrix below is exercised by tests (phase 7) and by reading the
guard call sites.

## Authorization review

- **Two planes, two guard families, both authoritative.**
  - platform plane: `platform:role:*`, `platform:permission:*` — unchanged; it is the only plane that
    creates or edits **global** rows.
  - tenant self-service: `tenant:role:*`, `tenant:permission:*` on `/api/v1/tenant/*`.
- **The tenant is derived, never supplied.** The one thing that must hold: no request parameter, path
  segment or body field may select the tenant on the tenant plane — it comes from the authenticated
  caller's `users.tenant_id`. That is what makes cross-tenant access (IDOR) impossible rather than
  merely filtered.
- **Tenant-aware guard.** `PermissionGuard` must resolve permissions in the caller's tenant context
  (phase 4); resolving with `null` would silently deny legitimate tenant admins, and getting this
  backwards (resolving a tenant caller's set with another tenant's context) would over-grant — the
  per-request cache must be keyed on the resolved tenant, never shared across callers.
- **No escalation.** Beyond the existing "a tenant admin cannot grant a `PLATFORM` role" rule
  (§9.5): a tenant admin may only grant permissions it **holds**, never the wildcard, and only within
  its own tenant. Ownership adds that a tenant-owned row is `TENANT` scope, so a tenant can never own
  a cross-tenant capability.
- **No self-lockout / seeded admin roles are protected.** `platform-admin` and each tenant's
  `admin` must not be renameable or deletable from **either** plane. Without this, a single
  `platform:role:read-write` holder can delete the only role that grants platform access. This is a
  **pre-existing** gap for `platform-admin` (deletable today); `admin` is new.
- **A tenant admin cannot mint a peer with more power.** Assigning a role requires holding its
  permission set, so `admin` can only be handed on by someone who already holds it (or by the
  platform admin) — not by an ordinary tenant user who somehow holds `tenant:user:read-write`.
- **Read-only global rows.** A tenant mutating a global role/permission gets `403`; another tenant's
  id gets `404` (it genuinely cannot see that row). Getting the pair backwards leaks existence.
- **Fail closed.** An unresolvable owner, a foreign-tenant code, or a scope/ownership mismatch is a
  `ValidationException` (`422`) — never a silent global fallback. Resolving a role by `code` alone
  must not accidentally match the global row when a tenant-owned one was intended.
- **`PermissionResolver` is unchanged** and its `user_roles` tenant filter is the enforcement point for
  effective permissions; the new invariant (phase 1, rule 4) is what makes it sufficient.

## Observability

- No new metrics or log lines are required: role/permission CRUD is not a privileged action on the
  same footing as a password reset (`UserService.resetPassword` logs there because it is silent and at
  the account-takeover boundary).
- If the existing admin-console logging already logs mutations at `INFO`, owner ids (random UUIDs) may
  appear there — confirm no email, username or code beyond what is already logged is added.

## Configuration

No new yaml key, environment variable, secret, CORS or deployment change.

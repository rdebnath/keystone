# Phase 6 — Security & observability

**Scope** — confirm the two-level model is fail-closed, that the wildcard path is unchanged, and that
nothing outside an explicit role change can escalate privileges. No new logging or metrics are added.

**Artifacts** — `PermissionGuard` deny path (Phase 3); no new files.

**Dependencies** — Phase 3.

**Verification** — the phase-7 integration test asserts `403` for a read-only caller on mutations
and `200`/`201` for a read/write caller; a manual `curl` with a read-only role shows the RFC 9457
`application/problem+json` body; `platform-admin` (`*`) still passes every route.

## Checks

- **Fail-closed**: an empty permission set, an unknown code, or a role with no grants denies. The
  read check accepts *only* the two codes of that resource, so a `tenant:*` grant can never satisfy a
  `platform:*` check (namespaces stay distinct).
- **Level order**: `READ_WRITE` ⊃ `READ_ONLY`. There is no "implicit write" path — `:read-only` never
  satisfies `requireWrite`.
- **Wildcard**: `*` continues to satisfy `requireRead`/`requireWrite` (it is checked before the code
  set, exactly as the old `require` did) and is still granted only to `platform-admin` by
  `BootstrapRunner.seedPlatformAdminRole`.
- **No implicit grant path**: the only code that creates a grant is an explicit role create/update
  (validated against the catalog) or the bootstrap's wildcard grant to `platform-admin`; nothing
  derives a grant from a code string.
- **Deny message** names only permission codes (never user data, never a stack trace) and is rendered
  by the existing global handler — `AccessDeniedException` → `403` problem+json, unchanged.
- **No secrets/PII** introduced; nothing new is logged, and the level checks add no new
  configuration or environment variables.
- **Frontend**: still UX-only. `/api/v1/me` returns the same `permissions: string[]`, so the client
  can render (but never enforce) the two levels.

## Execution record (2026-09-27)

- Deny path unchanged: `AccessDeniedException` → `403` problem+json, message naming only the accepted
  code(s), e.g. `Missing permission: platform:tenant:read-only or platform:tenant:read-write`.
- Level order, wildcard, fail-closed and cross-resource denial are now covered by
  `PermissionGuardTest` (5 HTTP cases) — see `07-testing.md`.
- No cleanup code exists (fresh development): the only grant writers are `RoleService` (codes validated
  against the catalog) and the bootstrap's wildcard grant, so there is no escalation path to review.
- Observability: the guard and the seeding add no logs, metrics or configuration — denials go through
  the existing problem+json handler.

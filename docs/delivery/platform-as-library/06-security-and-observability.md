# Phase 6 — Security & observability

**Scope** — harden the new login endpoint and preserve existing guards.

**Artifacts**
- Rate-limiting + uniform error on `/auth/login` (no account enumeration).
- Keep OIDC resource-server JWT validation + `PermissionGuard` / `@RequirePermission` for
  admin routes; tenant-context propagation via `ScopedValue`.
- Structured logging + `login.succeeded` / `login.failed` counters; never log passwords.

**Dependencies** — Phases 3–4.

**Verification** — tests for uniform error and guard enforcement; logs redact secrets.

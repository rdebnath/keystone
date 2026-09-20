# Phase 6 — Security & observability

**Scope** — bearer-JWT auth at the boundary, permission guard, input validation, logging.
**Artifacts** — `AuthFilter`, `PermissionGuard`, structured logging in services.
**Dependencies** — Phases 3–4.
**Verification** — slice test asserting `403` for missing/insufficient permission; `401/403`
mapping to RFC 9457.

## Notes

- Service-role key and Supabase URL never ship in the client; server-only (env/Secret Manager).
- Correlation id from `CorrelationIdFilter` propagates into logs.

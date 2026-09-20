# Phase 3 — Domain & application services

**Scope** — Ports and use-case services; no HTTP here.
**Artifacts** — `SupabaseAdminClient` (port) + `SupabaseHttpAdminClient`; `TokenAuthenticator`
(port) + `JwtTokenAuthenticator`; `PermissionResolver`; `TenantService`, `RoleService`,
`PermissionService`, `UserService`; DTO records + request records.
**Dependencies** — Phase 2.
**Verification** — unit tests (permission resolution, service validation); integration via
Testcontainers.

## Notes

- Transactions at the service boundary (`TransactionRunner`).
- Validation via `ValidationException`; conflicts via `ConflictException`; not-found via
  `NotFoundException` (all → RFC 9457 through the existing `ProblemDetailMapper`).
- `PermissionResolver.resolve(sub, tenantContext)` returns effective permission codes, treating
  `*` as "all". Tenant context filters `user_roles.tenant_id IS NULL OR = context` (§9.3).

# Phase 1 — Discovery & design

**Scope** — Confirm requirements, acceptance criteria, and contracts for the read-replica
framework. No schema, API, or UI change; this is a pure data-infrastructure capability.

**Artifacts**
- `docs/delivery/read-replica/plan.md` (this plan).
- `docs/delivery/read-replica/01-discovery-and-design.md` (this file).

**Dependencies** — None.

**Verification** — Open questions in `plan.md` are resolved and the confirmation state is
checked before implementation begins.

## Decisions

1. **Routing is explicit, not automatic.** jOOQ cannot tell a read from a write at the
   `ConnectionProvider.acquire()` layer (it is called per statement but carries no statement
   type), so automatic per-SQL routing is not feasible. Instead the framework exposes
   `read()` / `write()` / `transaction()` plus a scoped `readFromPrimary(...)` override.
2. **Scoped override uses `ScopedValue`** (Java 25, stable; composes correctly with virtual
   threads — see `docs/CODING_GUIDELINES_BACKEND.md` §6/§10), not `ThreadLocal`.
3. **Replica fallback = primary.** When no replica `DatabaseConfig` is supplied, `@Replica`
   aliases `@Primary`, so single-instance/local dev keeps working unchanged.
4. **Migrations always target primary.** The unnamed `DataSource`/`DSLContext` stay bound to
   primary so `MigrationRunner`/`TransactionRunner` are unaffected.
5. **Replica pool is `readOnly=true`** (HikariCP) as a safety net against accidental writes
   to a replica.

## Open questions (carried from plan.md)

- Admin (platform) schema adoption scope.
- Facade naming.
- Replica `readOnly=true` hardening confirmation.

# Delivery Plan — Read Replica Database Architecture

**Feature slug:** `read-replica`
**Level:** Platform-level — touches `platform/keystone-data` (Guice framework), shared
architecture, `apps/inventory` (demonstration), and `docs/` guidelines.

## Sizing decision

**Produce a plan.** This introduces a new cross-cutting capability (read/write database
splitting) that:

- adds a new framework-level abstraction (a read/write `Database` facade + primary/replica
  binding annotations in Guice),
- touches multiple layers (data infrastructure, application services, config),
- affects performance and consistency semantics (read-your-writes), and
- requires config/deployment notes (a read replica URL per environment).

## Summary

Today every service injects a single `DSLContext` and issues both reads and writes against
one connection pool (see `ItemService`, and the admin services that inject `@Platform
DSLContext`). The target is a platform-level framework in `keystone-data` that:

1. Routes **writes** (and transactions) to the **read-write** (primary) instance.
2. Routes **reads** to the **read replica** instance by default.
3. Lets a caller **selectively** force a specific operation to read from the primary
   (read-your-writes) via a small, scoped override — no signature changes, no new thread
   locals.

### Proposed API (framework, in `com.chetana.keystone.data`)

Two Guice binding annotations:

- `@Primary` — the read-write `DataSource` / `DSLContext`.
- `@Replica` — the read-replica `DataSource` / `DSLContext`.

A facade port + implementation:

```java
public interface DataAccess {
    DSLContext read();                                    // replica, unless scoped to primary
    DSLContext write();                                   // always primary
    void transaction(Consumer<DSLContext> action);        // primary transaction
    <T> T transactionResult(Function<DSLContext, T> action);
    <T> T readFromPrimary(Supplier<T> action);            // read-your-writes scope
    void readFromPrimary(Runnable action);
}

@Singleton
public final class JooqDataAccess implements DataAccess {
    // read()  => READ_FROM_PRIMARY.isBound() ? primary : replica
    // write() => primary
    // readFromPrimary(...) => ScopedValue.where(READ_FROM_PRIMARY, TRUE).call/run(...)
}
```

`DataModule` gains an optional replica `DatabaseConfig`:

```java
new DataModule(primary);                 // existing callers unchanged — replica = primary
new DataModule(primary, replica);        // opt into a read replica
```

When no replica is configured (local dev, single-instance, tests), the `@Replica` bindings
alias the primary, so `read() == write()` and behavior is unchanged.

**Backward compatibility.** The unnamed `DataSource`/`DSLContext` bindings are kept and point
at the primary, so `MigrationRunner`, `TransactionRunner`, and any not-yet-migrated code
continue to compile and write to the primary (migrations must never run against a read-only
replica).

## Phase list

1. Discovery & design
2. Database changes — **no Liquibase/schema change**; this phase covers the connection-pool
   / config layer (primary + replica pools) instead.
3. Domain & application services — the `Database` facade + `JooqDatabase` + `ItemService`
   adoption.
4. Server-side API & realtime — **skipped** (no API/contract change).
5. Frontend / UI (Flutter) — **skipped** (no UI change).
6. Security & observability
7. Testing
8. Delivery

## Resolved decisions

1. **Admin (platform) schema adoption** — yes. `PlatformDataModule` gains a
   `@PlatformReplica` binding and a `@Platform DataAccess`; the ~8 admin services are
   migrated to inject `@Platform DataAccess`.
2. **Facade name** — interface `DataAccess` + impl `JooqDataAccess`.
3. **Replica read-only hardening** — accepted: HikariCP `readOnly=true` on the replica pool.

## Confirmation state

- [x] Plan reviewed — open questions resolved.
- [x] Executed — phases delivered.

## Execution summary

- **Phase 2 (data infrastructure)** — `keystone-data`: `@Primary`/`@Replica` binding
  annotations, `DataModule(primary)` + `DataModule(primary, replica)` (replica pool
  `readOnly=true`, replica aliases primary when unconfigured), unnamed `DataSource`/`DSLContext`
  kept bound to primary for migrations/back-compat.
- **Phase 3 (domain & application services)** — `DataAccess` interface + `JooqDataAccess`
  (`ScopedValue`-scoped `readFromPrimary`); `ItemService` migrated to `read()`/`write()`/
  `readFromPrimary(...)`; `AppConfig`/`ConfigLoader`/`application.yaml` gain `database.read`
  (yaml-only; required, set equal to primary for read/write on one instance).
- **Admin adoption** — `PlatformReplica` + `PlatformDataModule(primary, read)` provide
  `@Platform DataAccess`; all 9 admin services migrated to `@Platform DataAccess`
  (`read()`/`write()`/`transaction(...)`); `AdminConfig`/`AdminConfigLoader`/`admin-config`
  gain required `database.read` (yaml-only).
- **Phase 6 (security & observability)** — replica `readOnly=true` hardening; DEBUG read-target
  log; no replica password committed.
- **Phase 7 (testing)** — `JooqDataAccessTest` (3 unit tests) + `DataModuleTest` (2
  Testcontainers tests: two-instance routing + no-replica alias). Full `mvn test` green
  (24 tests incl. migrated `AdminIntegrationTest`/`InventoryIntegrationTest`).
- **Phase 8 (delivery)** — README, `docs/ARCHITECTURE.md`, `docs/CODING_GUIDELINES_BACKEND.md`
  §7 updated with the read/write-split guidance.

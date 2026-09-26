# Phase 3 — Domain & application services (facade + adoption)

**Scope** — Introduce the `DataAccess` facade (port + jOOQ implementation) in `keystone-data`,
then adopt it in the inventory application as the canonical example.

**Artifacts**
- `platform/keystone-data/src/main/java/com/chetana/keystone/data/DataAccess.java` (new) — interface.
- `platform/keystone-data/src/main/java/com/chetana/keystone/data/JooqDataAccess.java` (new) — `@Singleton` implementation using `ScopedValue`.
- `platform/keystone-data/src/main/java/com/chetana/keystone/data/DataModule.java` (modify) — `bind(DataAccess.class).to(JooqDataAccess.class)`.
- `apps/inventory/server/src/main/java/com/chetana/keystone/inventory/item/ItemService.java` (modify) — inject `DataAccess`; `list()` → `data.read()`, `create()` → `data.write()`; demonstrate read-your-writes via `data.readFromPrimary(...)`.
- `apps/inventory/server/src/main/java/com/chetana/keystone/inventory/config/AppConfig.java` (modify) — add `Database.Read` nested record.
- `apps/inventory/server/src/main/java/com/chetana/keystone/inventory/config/ConfigLoader.java` (modify) — resolve `database.read.*` (yaml only, blank falls back to primary).
- `apps/inventory/server/src/main/resources/config/application.yaml` (modify) — add `database.read` block (blank = read/write on the primary).
- `apps/inventory/server/src/main/java/com/chetana/keystone/inventory/Main.java` (modify) — build read `DatabaseConfig`; pass `DataModule(primary, read)`.

**Dependencies** — Phase 2.

**Verification** — Inventory app compiles; `ItemService` reads route through `DataAccess.read()`
and writes through `DataAccess.write()`; a write-then-read uses `readFromPrimary`.

## Notes

- Keep `TransactionRunner` as-is (still bound to the primary `DSLContext`); `DataAccess`
  provides `transaction`/`transactionResult` for new code.
- The `read()` return type is `DSLContext`; callers must use it for reads only. The replica
  pool's `readOnly=true` (Phase 2) is the enforcement backstop.

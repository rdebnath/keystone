# Phase 7 — Testing

**Scope** — Unit + slice tests for the routing framework and the inventory adoption.

**Artifacts**
- `platform/keystone-data/src/test/java/com/chetana/keystone/data/JooqDataAccessTest.java` (new) — Mockito-free unit tests (two distinct `DSL.using(SQLDialect.POSTGRES)` contexts): `read()` returns replica by default, `write()` returns primary, `readFromPrimary(...)` makes `read()` return primary inside the scope.
- `platform/keystone-data/src/test/java/com/chetana/keystone/data/DataModuleTest.java` (new) — Guice slice test (Testcontainers, two independent PostgreSQL containers — no replication needed to prove routing):
  - `should_route_reads_to_replica_and_writes_to_primary`: write via `write()` → row visible via `readFromPrimary` but not via `read()` (different physical instance).
  - `should_route_reads_and_writes_to_same_instance_when_no_replica_configured`: `DataModule(primary)` — `read()` and `write()` hit the same instance.
- `apps/inventory/server/src/test/...` (modify existing integration test if present) — wire the replica config and assert `ItemService.create()` then `readFromPrimary`-backed read.

**Dependencies** — Phases 2 & 3.

**Verification** — `mvn -pl platform/keystone-data test` and the inventory `mvn test` pass.

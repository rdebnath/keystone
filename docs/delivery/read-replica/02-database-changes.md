# Phase 2 — Data infrastructure (connection pools & config)

**Scope** — Add the primary/replica connection-pool and config plumbing in `keystone-data`.
No Liquibase/changelog change: a read replica is infrastructure (its own connection URL),
not a schema change.

**Artifacts**
- `platform/keystone-data/src/main/java/com/chetana/keystone/data/Primary.java` (new) — `@BindingAnnotation`.
- `platform/keystone-data/src/main/java/com/chetana/keystone/data/Replica.java` (new) — `@BindingAnnotation`.
- `platform/keystone-data/src/main/java/com/chetana/keystone/data/DataModule.java` (modify) — provide `@Primary`/`@Replica` `DataSource` + `DSLContext`; keep unnamed bindings = primary.
- `platform/keystone-data/pom.xml` (no change expected — HikariCP/jOOQ/Guice already present).

**Dependencies** — Phase 1 (design confirmed).

**Verification** — `keystone-data` compiles; a slice test (Phase 7) builds the `Injector` and
asserts the `@Primary`/`@Replica` bindings resolve and that the replica pool is read-only when
a replica config is supplied, and aliases primary when not.

## Changes

- `DataModule` constructors: `DataModule(DatabaseConfig primary)` and
  `DataModule(DatabaseConfig primary, DatabaseConfig replica)`.
- A private `dataSource(DatabaseConfig, boolean readOnly)` helper builds each HikariCP pool
  (replica pool sets `readOnly=true`).
- `@Provides @Primary DataSource/DSLContext`, `@Provides @Replica DataSource/DSLContext`.
- `bind(DataSource.class).to(Key.get(DataSource.class, Primary.class))` (or a `@Provides`
  delegating to `@Primary`) to keep the unnamed `DataSource` = primary; same for `DSLContext`.

# Keystone — Coding Guidelines

Enterprise-grade coding standards for the **Keystone** platform and its applications. These
rules are normative: code that deviates from them must either be fixed or carry an explicit,
reviewed justification.

## 1. Platform & Toolchain

| Concern | Choice |
| --- | --- |
| Language | **Java 25 (LTS)** — pinned; upgrade the JDK only when explicitly requested |
| Runtime | Virtual threads enabled by default (see §6) |
| Framework | **Guice** (Google dependency injection) |
| Persistence | **jOOQ** (type-safe SQL; generated from Liquibase changelogs) |
| Database | **PostgreSQL** (Supabase-managed) — each application owns its own **schema** in a shared database (see §7) |
| Communication | REST request/response + Supabase Realtime broadcast (see §8) |
| API Gateway | Google Cloud Run front-end — terminates TLS (see §8); no self-managed gateway |
| Build | **Maven** |
| IDE | IntelliJ IDEA (shared `.editorconfig` committed to the repo) |

> **JDK policy.** The Java version is pinned in the build files and the IntelliJ SDK. Do
> not bump it opportunistically; upgrade the JDK only when explicitly requested.

> **Version discipline.** Pin the Guice, Javalin, jOOQ, and JVM versions in the build files.
> Do not copy older framework idioms or deprecated auto-configuration patterns. Verify
> against the current Guice, Javalin, and jOOQ reference documentation.

> **Deprecation discipline.** Do not use deprecated (`@Deprecated`) APIs, classes,
> annotations, packages, or configuration — whether in the JDK, Guice, Javalin, jOOQ, a
> third-party library, or our own code. A deprecation is a removal warning: using it bakes
> in future breakage and compiler warnings. Migrate to the documented replacement instead of
> suppressing the warning (e.g. Testcontainers' `org.testcontainers.*` module classes, or
> jOOQ's newer generated/DSL APIs). Treat a deprecation warning in a build or an IDE
> inspection as a defect to fix, not noise to ignore.

## 2. Language: Use Java 25 Features

Prefer modern, *finalized* language features. Do not use preview features in production
code unless there is a written, reviewed justification and `--enable-preview` is
deliberately enabled.

### Use these (stable) features by default

- **Records** — for DTOs, value objects, config property holders, and immutable results.
- **Sealed interfaces/classes** — for closed hierarchies: commands, events, results, states.
- **Switch expressions + pattern matching for switch** — replace `if/else` chains and
  visitor boilerplate with exhaustive, value-returning dispatch.
- **Record patterns** — destructure records inside `switch`/`instanceof`.
- **Text blocks** — for multi-line strings (SQL, JSON, test fixtures).
- **Virtual threads** — platform threads are opt-in, not the default.
- **Sequenced collections** (`SequencedCollection`, `List.getFirst()`, `reversed()`).
- **Unnamed variables (`_`)** — for ignored lambda/pattern/loop variables.
- **Stream Gatherers** — for custom, reusable intermediate `Stream` operations.
- **Scoped Values** — prefer over `ThreadLocal` for request-scoped context (e.g. trace
  id, tenant id); they compose correctly with virtual threads.
- **Flexible constructor bodies** — statements before `super()`/`this()` where it removes
  static helper boilerplate.
- **Module import declarations (`import module java.base;`)** — acceptable in small tools
  and tests; in application code prefer explicit imports for readability.

### Do NOT use

- **String templates** — *removed* from the JDK (JEP withdrawn). Use
  `String.formatted(...)`, text blocks, or `StringBuilder`.
- **Preview/incubator features** (`Structured Concurrency`, `Primitive Types in Patterns`,
  Vector API, …) in shipped code without explicit sign-off.
- **Raw types**, `var` in non-obvious positions, and reflection where a typed API exists.

## 3. Programming Style: Functional by Default

Write in a **functional, immutable style**. Use declarative `Stream`/`Optional`
pipelines, immutable data carriers, and pure functions. Fall back to imperative code
**only** when one of the following holds — and say so in a comment:

1. The imperative version is *clearly* more readable (e.g. an early-exit loop beats a
   forced `stream().takeWhile().findFirst()`).
2. There is a real performance implication on a measured hot path.
3. The operation throws checked exceptions (streams don't handle them cleanly).
4. You need mutable local state with branching control flow.

### Rules

- **Immutability first.** Use records, `List.of/Set.of/Map.of`, `Collections.unmodifiable*`,
  and `final` fields. Prefer `Stream.toList()` over `collect(Collectors.toList())`.
- **Constructors & builders.** A constructor is a positional API; beyond about seven
  parameters, call sites get error-prone. Provide a hand-written builder when a constructor
  has **more than seven parameters**, or earlier when several parameters share a type or are
  optional — named builder methods prevent argument-order bugs. No Lombok: write the builder
  by hand (§6). For **records**, prefer **decomposing** into smaller nested records that
  group related components (as `AppConfig` does) over adding a builder: records are
  transparent data carriers and a builder usually adds ceremony. Add a record builder only
  when decomposition doesn't fit and call sites need named/optional construction.
- **`Optional` only for return values.** Never for fields, record components, or method
  parameters. Return an empty collection rather than a nullable/`Optional` collection.
- **No `null` in public APIs.** Return `Optional.empty()` or a sentinel/result type.
- **Pure functions where possible.** Keep side effects at the edges (controllers,
  listeners, repositories), not buried in the domain/service core.
- **Lambdas are pure.** No mutation of captured state, no `forEach` for side effects
  (use an actual loop instead).
- **One-liners vs clarity.** Prefer readability over cleverness; a short method is worth
  more than a dense stream chain.
- **Checked exceptions** in streams → extract a small private method that wraps/rethrows
  an unchecked exception, or refactor to an imperative loop.

### Example — functional

```java
public sealed interface PaymentResult permits Success, Failure {
    record Success(String transactionId) implements PaymentResult {}
    record Failure(String reason) implements PaymentResult {}
}

public String describe(PaymentResult result) {
    return switch (result) {
        case PaymentResult.Success s -> "Paid: " + s.transactionId();
        case PaymentResult.Failure f -> "Failed: " + f.reason();
    };
}

List<OrderSummary> summaries = orders.stream()
        .filter(Order::isPaid)
        .map(mapper::toSummary)   // pure, side-effect-free
        .toList();
```

### Example — imperative is justified

```java
// Imperative: early exit is clearer and avoids allocating a stream for a tiny hot loop.
for (Order order : orders) {
    var total = order.total();
    if (total.compareTo(threshold) > 0) return order; // early exit
}
```

## 4. Naming & Formatting

- **Packages**: all-lowercase, reverse-DNS root `com.acme.keystone`, feature-oriented
  (see §5). No underscores.
- **Classes/interfaces/records/enums**: `UpperCamelCase`. Interfaces are *not* prefixed
  with `I`. Implementation suffix only when it adds information (`JooqOrderRepository`),
  never `OrderServiceImpl`.
- **Methods/fields**: `lowerCamelCase`, verbs for methods, nouns for fields.
- **Constants**: `UPPER_SNAKE_CASE` for `static final` primitives/Strings; for complex
  constants prefer immutable collections or enums.
- **Test methods**: `should_<expectedBehavior>_when_<condition>`.
- **Indentation**: 4 spaces, no tabs. Braces on the same line (K&R). Max line length 120.
- **Fluent APIs / method chaining**: one method call per line, aligned under the receiver.
- **`.editorconfig`** is the source of truth; commit it and let IntelliJ/Spotless use it.
- **Always `@Override`** on overridden methods. **Always `final`** on classes that are not
  designed for inheritance, and on immutable fields/parameters where helpful.
- **One top-level type per file**; the filename must match the public type.

## 5. Architecture & Packaging

Package **by feature**, not by layer. Each feature owns its web, service, persistence,
and realtime concerns; cross-cutting infrastructure lives in a shared module/package.

```
com.acme.keystone
├── Main.java                         # entry point: builds the Guice Injector from modules
├── common/                           # shared DTOs, errors, util, clock, id-gen
├── order/                            # feature: orders
│   ├── api/                          #   HTTP handlers/routes + DTOs (request/response)
│   ├── application/                  #   use-case services, ports (interfaces)
│   ├── domain/                       #   records, value objects, domain rules
│   └── persistence/                  #   jOOQ DAOs/repositories + adapters
├── notification/                     # feature: notifications (pub/sub)
│   └── ...
└── config/                           # cross-cutting Guice modules, security, observability
```

### Layering rules

- **`api`** talks only to **`application`** (ports). It never reaches `persistence` directly.
- **`domain`** is framework-agnostic — no Guice, no jOOQ imports. Pure Java + tests.
- **`application`** defines **ports** (interfaces) that `persistence` implements — so the
  service layer depends on abstractions, not on jOOQ/DAO details directly.
- **DTOs are records** and are defined where they are produced (request DTOs in `api`,
  response/projection DTOs may live in `application`).
- Mapping between row/record ↔ DTO is explicit (a `*Mapper` component or a constructor);
  never expose persistence types from `api` handlers.

### Configuration

Environment-specific configuration is **typed** and resolved at **runtime**, not baked into the
image (the frontend is the exception — it uses `--dart-define` at build time).

- Each application owns its config under `<app>/server/src/main/resources/config/`:
  `application.yaml` (base defaults) and `application-{env}.yaml` (per-environment overrides).
  The platform admin console does the same under `admin-config/`.
- `APP_ENV` (default `dev`) selects the environment file.
- **Resolution order (highest precedence wins):** secret environment variable →
  `application-{env}.yaml` → `application.yaml` → code default. Non-secret values are resolved
  **from yaml only** — environment variables override *secrets*, nothing else.
- Config is one immutable `AppConfig` record with nested records per concern (database, server,
  realtime, security), resolved by a `ConfigLoader` and bound via a `ConfigModule`
  (`bind(AppConfig.class).toInstance(...)` plus each slice). Services `@Inject` the slice they
  need, not the whole config.
- **Secrets never live in files or the repo** — `database.password`, `realtime.serviceRoleKey`,
  `supabase.serviceRoleKey`, and the bootstrap admin password are supplied via environment
  variables (Cloud Run → Secret Manager).
- The same image ships to every environment; only `APP_ENV` and secrets differ per deployment.

## 6. Guice — Dependency Injection & Wiring

Guice is the DI container. Prefer **explicit, code-based wiring** over reflection or
classpath scanning so the module graph stays readable and testable.

### Modules & bindings

- One `AbstractModule` (or `@Provides` module) per feature and per cross-cutting concern
  (`OrderModule`, `PersistenceModule`, `ClockModule`, `RealtimeModule`); compose them in
  `Main` with `Guice.createInjector(Stage.PRODUCTION, ...)`.
- Bind **interface → implementation** explicitly
  (`bind(OrderRepository.class).to(JooqOrderRepository.class)`). Use `@ImplementedBy` /
  `@ProvidedBy` only as a documented default for tests or standalone use.
- Prefer `bind(Foo.class).toInstance(...)` for fixed values (clock, config record) and
  `bind(Foo.class).toProvider(...)` for lazy/one-shot construction.
- Use `@Provides` methods only for objects Guice cannot construct directly (third-party
  builders, `DSLContext`, `DataSource`, `HttpClient`); keep them small and side-effect-free.
- Bind `DSLContext`, `DataSource`, and the HTTP client once in infrastructure modules;
  features depend on those abstractions, never on connection/config details.

### Constructor injection & scopes

- **Constructor injection only** — `@Inject` on the constructor; no field/setter injection.
  JSR-330 annotations (`@Inject`, `@Named`, `@Singleton`) are the standard.
- **Scopes**: `@Singleton` for stateless services, DAOs, clients, and config. Use the default
  (unscoped) binding only for truly short-lived objects; avoid custom request scopes — use a
  `ScopedValue` for request context instead (§10).
- **Prefer instance methods on `@Singleton` beans over `static` methods.** Static utility methods
  (e.g. `SchemaTool.run(...)`) cannot be overridden or mocked, hide their collaborators, and force
  callers to hard-code a concrete class. Write a `@Singleton` with constructor-injected dependencies
  and instance methods instead. Reserve `static` for pure functions (no state, I/O, or collaborators)
  and for the JVM-mandated `main(...)` entry point, which should be a thin shell that delegates to an
  instance.
- No Lombok — write the constructor, or use compact record constructors.

### Assisted injection & factories

- For objects that mix injected dependencies with runtime values (e.g. a `RealtimePublisher`
  parameterized by channel name), use **AssistedInject** (`@AssistedInject` +
  `FactoryModuleBuilder`). Never pass the `Injector` into domain/service code.

### Multibindings

- Use **`Multibinder`**/**`MapBinder`** for plugin-style registries (e.g.
  `Map<String, ErrorMapper>`, a `Set<FeatureModule>`), so features register themselves
  without editing a central switch.
- Prefer `MapBinder<String, X>` with a named-key convention over long `switch`/`if` chains.

### Configuration & secrets

- Bind external configuration into an immutable `Config` record at startup (from env vars /
  Secret Manager); use `@Named` only for a few scalar values. Avoid stringly-typed lookups
  scattered through the code.
- Resolve secrets at startup and bind them; never store secrets in source, images, or logs.

### Startup & lifecycle

- Build the injector with **`Stage.PRODUCTION`** so missing bindings fail fast at startup
  instead of lazily at runtime.
- `Main` is thin: build the injector, start Javalin, then register a shutdown hook that
  drains in-flight requests and closes `DataSource`/`DSLContext` on SIGTERM (Cloud Run).
- No static `Injector` singleton / service-locator — pass dependencies through constructors.

### Application wiring conventions

- **HTTP handlers** are thin Javalin handlers (see §8); they depend on application services,
  never on DAOs/`DSLContext` directly.
- **Clients**: use the JDK `HttpClient` (or a thin wrapper); construct it once in a module.
- **Transactions** belong at the `application` service boundary via
  `DSLContext.transaction(...)` (§7), never in handlers or DAOs.
- **Virtual threads**: run request handling on virtual threads (Jetty/Javalin executor with
  `Thread.ofVirtual().factory()`). Keep blocking I/O (JDBC, HTTP) on virtual threads; do not
  pin them with `synchronized` hot blocks.
- **Stateless & health**: keep the process stateless (Cloud Run scales horizontally); expose
  a `/healthz` liveness/readiness endpoint.


## 7. Persistence (jOOQ)

jOOQ is a **SQL-first** library with a type-safe DSL and code generation. Write SQL
explicitly; do not fight an ORM. Keep SQL and generated types in the `persistence` layer
behind the ports defined in `application`.

- **Code generation is the source of truth**: jOOQ generates table/record/sequence types
  **offline from the Liquibase changelog** (rendered to DDL), never from a live database
  (see `docs/ARCHITECTURE.md` §6.2).
- **Type-safe DSL**: build queries with jOOQ's `DSLContext` and the generated classes — no
  hand-written SQL strings outside `persistence`, no raw JDBC.
- **Map to records**: project results into immutable records (`record.into(MyDto.class)` or
  an explicit mapper). Never hand-roll result-set loops.
- **DAOs/repositories** live in `persistence`; they are *not* returned by handlers. Return
  DTO/projection records instead.
- **Every aggregate root** has a generated `Long` or `UUID` id. Use a PostgreSQL `SEQUENCE`
  or `uuid`; avoid `IDENTITY` (it defeats JDBC batching).
- **Optimistic locking**: use an explicit `version` (or `updated_at`) column and
  `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?`; check the affected
  row count and raise `ConflictException` on `0` (jOOQ has no `@Version`).
- **Explicit joins, no N+1.** Write the joins you need (or one DAO method per query shape).
  Do not loop-and-query.
- **Transactions**: run `DSLContext.transaction(...)` at the `application` service boundary.
  Queries run read-only; write transactions only around the service method that mutates
  state. Never in handlers.
- **Value objects**: model them as records, not primitive obsession.
- **No mutable "entities".** Everything is a record/DTO; there is no ORM-managed state, so
  `equals/hashCode` are plain record semantics on the business key.
- **Migrations** with **Liquibase** — the schema is versioned via changelogs. Never
  auto-generate the schema outside local dev scratch databases.
- **Batching**: use `dslContext.batch(...)`/`batchInsert`/`batchStore` for bulk writes.
- **PostgreSQL types**: use native types — `jsonb` for JSON, `uuid` for UUID keys, and
  `timestamptz` (`timestamp with time zone`) for timestamps. Bind `jsonb` via jOOQ's JSON
  binding/forced types; never store JSON as plain text.
- **Caching/invalidation**: broadcast changes via Supabase Realtime (see §8). Do not rely on
  an ORM second-level cache.

### Liquibase changelogs

- **One master changelog** that includes versioned, ordered changelog files per change or
  feature (`db/changelog/…`). Never edit an applied changeset; add a new one.
- Each changeset is **idempotent** with a stable `id` + `author`; use `preConditions` where a
  guard is needed. Prefer forward-only (no destructive rollbacks without review).
- Changelogs are the **single source of truth** for both the database and jOOQ codegen
  (rendered to DDL offline — see `docs/ARCHITECTURE.md` §6.2). No ad-hoc DDL outside
  Liquibase.

### Connection pool (HikariCP)

Use **HikariCP** directly (configured in a Guice module) as the pool; do not swap pools
without a reason.

- **Pool sizing**: the pool is the concurrency guard for blocking JDBC on virtual threads.
  Size `maximumPoolSize` against PostgreSQL's `max_connections`, not CPU count. Start modest
  (e.g. 10–20 per service instance) and tune from metrics; a large pool does not add
  throughput and can exhaust Postgres connections across services.
- **Timeouts**: set `connectionTimeout` (fail fast — do not let threads block forever), and
  `maxLifetime` a few seconds *below* the shortest DB/proxy idle timeout so the pool
  recycles connections before infrastructure drops them.
- **Keepalive & leak detection**: set `keepaliveTime` where idle connections traverse a
  load balancer/firewall; set `leakDetectionThreshold` in dev/staging to catch unclosed
  connections early.
- **Metrics & observability**: give the pool a stable `poolName` and expose HikariCP metrics
  via Micrometer (`hikaricp.connections.*`). Alert on connection-timeout spikes and pool
  exhaustion (`hikaricp.connections.pending`).
- **No connection-test-query**: rely on JDBC 4 `Connection.isValid()` for PostgreSQL; do
  not set a legacy `connection-test-query`.

### Read/write split (read replica)

Route **writes to the primary** (read-write) instance and **reads to a read replica** by
default, using the `DataAccess` facade (`keystone-data`):

- Inject `DataAccess` (not a raw `DSLContext`) into services/DAOs.
- `data.read()` returns the replica `DSLContext`; `data.write()` returns the primary
  `DSLContext`; `data.transaction(...)` / `transactionResult(...)` run on the primary.
- **Read-your-writes**: wrap a write-then-immediate-read in
  `data.readFromPrimary(() -> …)` / `readFromPrimary(runnable)` so the read hits the primary
  regardless of replica lag.
- **Read target is required**: `database.read.url` is configured in yaml only
  (`application-{env}.yaml`; no env override) and is **required** — set it equal to
  `database.url` for read/write on one instance, or to a replica URL to enable a replica.
  `database.read.username/password/maxPoolSize` are optional and fall back to the primary's
  values. The replica pool is `readOnly=true` (PostgreSQL rejects accidental writes).
- Wire via `new DataModule(primary, read)` (when the read URL equals the primary it aliases the
  primary); the platform-admin schema mirrors this with `PlatformDataModule`,
  `@Platform`/`@PlatformReplica`, and `@Platform DataAccess`.

## 8. Communication: Request/Response & Realtime

Keystone uses two interaction styles. **Google Cloud Run's front-end** terminates TLS and
routes REST traffic to the Java service; **Supabase Realtime** provides the WebSocket
broadcast path — there is no self-managed nginx or broker.

### Request/Response (REST — Javalin)

- JSON over HTTP via **Javalin** (embedded Jetty). Route handlers + records for
  request/response bodies.
- **Route organization**: feature-scoped handler classes register their own routes under a
  versioned path group (`app.routes(...)`, `path("/api/v1", ...)`). Use nouns, not verbs, in
  resource paths.
- **Handlers are thin**: parse/validate the body, call an application service, write the DTO.
  Read typed bodies via `ctx.bodyValidator(MyDto.class)` / `ctx.bodyAsClass(...)`; write with
  `ctx.json(dto)` and the right status.
- **Cross-cutting** via Javalin `before`/`after` filters (auth, correlation id, logging,
  CORS) — not duplicated in every handler.
- **Validation**: Jakarta Validation (`@Valid`, `@NotBlank`, …) on record DTOs; return `400`
  with field-level errors.
- Return proper HTTP semantics: `201` for created (with `Location`), `204` for no content,
  `404`/`409`/`422` for the right failure.
- **Errors** map through one place (§9): a single `app.exception(...)` handler or shared
  mapper returns RFC 9457 `application/problem+json`; never leak stack traces or SQL.
- Paginate list endpoints (`page`, `size`, `sort`) and return a typed page wrapper.

### Realtime (Supabase broadcast)

**Supabase Realtime** is the single realtime fan-out mechanism:

- **Clients** (Flutter) open a WebSocket and subscribe to Realtime channels.
- **The Java service publishes** a message to Supabase Realtime (the trigger for broadcast);
  Supabase fans it out to every subscribed client. See `docs/ARCHITECTURE.md` §5.2.

Conventions:

- **In-process events**: use a small typed publisher for intra-process notifications — no
  broker hop needed within the process.
- **Channels** are namespaced and versioned (e.g. `orders.{id}`, per tenant where
  multi-tenancy requires it). Document each channel.
- **Broadcast payloads are immutable records** with an event id, timestamp, aggregate id,
  and version. Never publish persistence rows or internal domain objects directly.
- **Publishing from Java**: publish through a `RealtimePublisher` port (Supabase Realtime
  broadcast API over HTTPS, or a server-side Realtime client); never call Supabase directly
  from handlers. Use the **service-role key** server-side only.
- **Wiring**: configure the Supabase Realtime endpoint/keys; restrict channel access to
  authorized clients (Realtime auth / RLS). Publish idempotently with retry/backoff,
  correlating on the event id.

## 9. Error Handling

- **Exception hierarchy**: a single `KeystoneException` base (unchecked) with typed
  subclasses (`NotFoundException`, `ConflictException`, `ValidationException`,
  `AccessDeniedException`) carrying an error code.
- **Do not leak internals**: never return stack traces or SQL to clients.
- **Central handler**: a single global exception handler/filter maps exceptions to an RFC
  9457 problem+json body (status, type, title, detail, instance, and an app-specific
  `code`).
- **Log at the right level**: `WARN` for expected business failures, `ERROR` for
  unexpected ones (with stack trace). Re-throw as-is — don't wrap-and-hide.
- **Validation failures** → `400` with a list of field errors, not one big message.


## 10. Logging & Observability

- **SLF4J** (Logback). Structured/key-value logging for machine parsing.
- **Per-class logger**, but prefer constructor-injected services over logger-per-everything.
- **Request context**: propagate `traceId`/`tenantId` via a **`ScopedValue`** (not
  `ThreadLocal`) bound at the filter/entry point.
- **Metrics** via Micrometer: expose a Prometheus scrape endpoint, add timers/counters on
  meaningful business operations.
- **Log levels**: `DEBUG` for flow, `INFO` for lifecycle/business milestones, `WARN` for
  recoverable anomalies, `ERROR` for failures needing action.
- **Never log**: passwords, tokens, keys, PII (unless masked and legally required).

## 11. Testing

- **JUnit 5** + **AssertJ** + **Mockito**. Testcontainers for real dependency testing.
- **Test pyramid**: many fast unit tests, fewer integration/slice tests, a handful of E2E.
- **Unit tests**: pure JUnit + AssertJ, no DI container. Test the functional core
  (domain/services) heavily.
- **Slice tests** for web/data:
  - **Web**: exercise route handlers through a test HTTP client against an embedded server
    (validation and the global error handler included).
  - **Data**: test jOOQ DAOs against a PostgreSQL container (Testcontainers). Do not use
    H2: it does not faithfully reproduce PostgreSQL behavior (types, `jsonb`, sequences).
  - **Guice**: build the `Injector` from the real modules and override only what's needed via
    `Modules.override(...)` or dedicated test modules; never start a real Supabase/broker in
    unit tests.
  - **Javalin**: use `JavalinTest` (javalin-testtools) to exercise handlers + the global
    error handler without a full server.
- **Integration**: build the real Guice `Injector` + embedded server with Testcontainers for
  the full stack (PostgreSQL + Supabase Realtime) for realtime flows.
- **Naming**: `should_<behavior>_when_<condition>`. Arrange–Act–Assert, one behavior per test.
- **Fixtures**: factory methods/records, not giant shared test data. Keep tests deterministic
  and independent (no order/global-state coupling).
- **Coverage**: meaningful coverage (branch on core domain logic), not vanity percentage.

## 12. Security

- **OAuth2/OpenID Connect (resource server)** is the default: validate JWT access tokens
  with a JOSE library (e.g. Nimbus JOSE + JWT). Never roll your own crypto/auth.
- **Authorization** at the service/method level *and* at the data layer where
  multi-tenancy requires it (a guard/filter + explicit checks).
- **Validate all input**; treat every request and every pub/sub payload as untrusted.
- **Store secrets in the environment/secrets manager**, never in source or images.
- **OWASP awareness**: parameterized queries (jOOQ binds parameters — never concatenate
  SQL), no dynamic SQL string building, sanitize output, protect against mass-assignment by
  never binding directly to DTOs from request bodies without explicit mapping.

## 13. Code Review Checklist

Before merging, confirm:

- [ ] Uses Java 25 features appropriately (records, pattern matching, sealed types, …)
- [ ] Functional style used unless a justified exception exists
- [ ] No preview features, no string templates, no `null` in public APIs
- [ ] No deprecated JDK/Guice/Javalin/jOOQ/third-party APIs; build free of deprecation warnings
- [ ] Persistence rows/DAOs are not leaked into handlers; DTO records are used at the boundary
- [ ] Transactions are at the service boundary; N+1 traps avoided (explicit joins)
- [ ] Constructor injection only; no static `Injector`/service-locator; modules are explicit
- [ ] Behavior lives on `@Singleton` instance methods, not `static`; `static` only for pure functions and a thin `main`
- [ ] Constructors with >7 parameters use a hand-written builder; large records are decomposed
- [ ] jOOQ types generated from the Liquibase changelog; no ad-hoc DDL
- [ ] Pub/sub payloads are immutable, versioned records (not persistence types)
- [ ] Errors are centralized (one global handler → RFC 9457 problem+json), no leaked internals
- [ ] Tests added and passing (`mvn test`)
- [ ] No secrets logged or committed; schema changes are via a Liquibase changelog migration
- [ ] `.editorconfig`/formatter applied (no formatting churn)

## References

- Guice (Google) user guide and Javadoc
- Javalin documentation: <https://javalin.io>
- jOOQ manual and Javadoc: <https://www.jooq.org>
- Liquibase documentation: <https://docs.liquibase.com>
- Supabase Realtime: <https://supabase.com/docs/guides/realtime>
- JDK 25 JEP list: <https://openjdk.org/projects/jdk/25/>
- Architecture: `docs/ARCHITECTURE.md`


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
- **Ad-hoc JSON shapes** — `Map<String, Object>`, `JsonNode`/`ObjectNode`, or `List<Map<…>>`
  standing in for a payload. Declare a record instead (§13).

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
  response/projection DTOs may live in `application`). Every JSON payload — including
  third-party API bodies and config documents — is a record too: never a `Map` or a
  `JsonNode` (§13).
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
  **from yaml only** — environment variables override *secrets*, nothing else. The single exception is
  an **operational on/off switch** (e.g. `MIGRATE_ON_START`, `BOOTSTRAP_ON_START`): it is not a
  secret, but it is resolved env-variable-first so the same image can be deployed with a startup
  behavior on or off, and it must always have a yaml/code default that preserves the existing
  behavior.
- Config is one immutable `AppConfig` record with nested records per concern (database, server,
  realtime, security), resolved by a `ConfigLoader` and bound via a `ConfigModule`
  (`bind(AppConfig.class).toInstance(...)` plus each slice). Services `@Inject` the slice they
  need, not the whole config.
- **Secrets never live in files or the repo** — `database.password`, `realtime.serviceRoleKey`,
  `supabase.serviceRoleKey`, and the bootstrap admin password are supplied via environment
  variables (Cloud Run → Secret Manager).
- The same image ships to every environment; only `APP_ENV` and secrets differ per deployment, plus
  the operational startup switches (`MIGRATE_ON_START`, `BOOTSTRAP_ON_START`).

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
- **Schema-qualified tables**: generated types carry their schema (from the changelog's
  `defaultSchemaName`), so jOOQ renders `"schema"."table"`. Cross-schema joins work without
  special handling, and SQL never depends on the connection `search_path` (Supabase's PgBouncer
  does not reliably maintain it). `DataModule`/`PlatformDataModule` also map unqualified
  references to the module's schema as a defensive fallback.
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
  feature (`db/changelog/…`). Prefer adding a new changeset over editing an applied one; if an
  applied changeset must change, whitelist its old checksum with `<validCheckSum>`.
- Each changeset is **idempotent** with a stable `id` + `author`; use `preConditions` where a
  guard is needed. Prefer forward-only (no destructive rollbacks without review).
- Changelogs are the **single source of truth** for both the database and jOOQ codegen
  (rendered to DDL offline — see `docs/ARCHITECTURE.md` §6.2). No ad-hoc DDL outside
  Liquibase.
- **Raw `<sql>` blocks** must be schema-qualified with `${database.defaultSchemaName}` so the
  offline-rendered DDL stays consistent with the schema-qualified generated code.

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
  request/response bodies: every body is a typed record at the boundary, never a `Map` or
  `JsonNode` (§13).
- **Route organization**: feature-scoped handler classes register their own routes under a
  versioned path group (`app.routes(...)`, `path("/api/v1", ...)`). Use nouns, not verbs, in
  resource paths.
- **Handlers are thin**: parse/validate the body, call an application service, write the DTO.
  Read typed bodies via `ctx.bodyValidator(MyDto.class)` / `ctx.bodyAsClass(...)`; write with
  `ctx.json(dto)` and the right status.
- **Cross-cutting** via Javalin `before`/`after` filters (auth, correlation id, logging,
  CORS) — not duplicated in every handler.
- **Validation**: Jakarta Validation (`@Valid`, `@NotBlank`, …) on record DTOs; a rejected value
  returns `422` with field-level errors. `400` is reserved for a request that cannot be parsed at
  all (malformed JSON, missing body), which the framework rejects before a handler runs.
- Return proper HTTP semantics: `201` for created (with `Location`), `204` for no content, and the
  failure status the error deserves (§9: `404` unknown resource, `409` conflicting state, `422`
  rejected value, `403` denied).
- **Errors** map through one place (§9): a single `app.exception(...)` handler or shared
  mapper returns RFC 9457 `application/problem+json`; never leak stack traces or SQL.
- Paginate list endpoints (`page`, `size`, `sort`) and return a typed page wrapper — the frozen
  contract is **List endpoints** below, and it applies to every list route on every plane.

### List endpoints (search, filtering & paging)

**Every list route returns a page, never a whole collection**, and every search and filter is applied
by the **server** (`docs/UX_GUIDELINES.md` §1 explains why: a client that filters the page it holds
searches only that page).

| Parameter | Type | Default | Rule |
| --- | --- | --- | --- |
| `page` | int, `0…10000` | `0` | 0-based; `offset = page × size` |
| `size` | int, `1…100` | `25` | outside the range → `422` (the cap bounds the work one request can ask for) |
| `sort` | resource key | resource default | not in the resource's whitelist → `422` naming the allowed keys |
| `order` | `asc` \| `desc` | `asc` | case-insensitive |
| `q` | string ≤ 100 chars | absent | case-insensitive *contains* over the resource's documented searchable columns |
| resource filters | | | e.g. `tenantId`, `scope`, `access`, validated like any other parameter |

```json
{ "items": [ { "…": "the resource DTO" } ], "page": 0, "size": 25,
  "totalElements": 142, "totalPages": 6, "hasNext": true, "hasPrevious": false }
```

Rules:

- **Use the shared vocabulary; do not hand-roll it.** `PageRequest`, `Page`, `SortOrder`,
  `SearchTerm` and `OptionList` live in `com.chetana.keystone.common.query`; `Search` in
  `com.chetana.keystone.data`; `QueryParams` in `com.chetana.keystone.web`. A handler reads its
  parameters in one line; a service builds the `Condition`, counts it, then takes the window
  (`fetchCount` + `limit`/`offset`) — no generic paging abstraction over jOOQ.
- **Search is a bound `LIKE` with escaped wildcards** (`Search.containsIgnoreCase`): jOOQ binds the
  term (no injection) and `%`, `_` and `\` are escaped with an explicit `ESCAPE`, so a user cannot
  turn a search into a match-everything pattern.
- **Ordering must be total.** Every sort key gets a tiebreaker (`code`, `username`, `name`, `id`);
  without one, a row can appear on two pages or on none.
- **A stale page is not an error**: `page` beyond the last page returns `200` with `"items": []` and
  the real totals.
- **The sort whitelist lives in the service**, never in a handler: an unknown key is a `422`, so no
  caller-supplied text can reach `ORDER BY`.
- **Paging never widens visibility**: the window is applied to exactly the `WHERE` the unpaged list
  used — tenant/owner scoping and permission checks first, unchanged — combined with search and
  filters by `AND`.
- **Reference data for pickers uses `/options`, never a page.** A dropdown that must show every
  choice cannot be fed by a paged list, so a resource that has one exposes an unpaged
  `GET …/options` returning `OptionList<T>` (capped at `OptionList.MAX_OPTIONS`, with `truncated`
  reported), behind the same read guard as its list route.
- **A list envelope is a typed record** (§13): never assemble `{"items": …}` from a `Map`, and never
  return a bare array from one list route and an envelope from another.
- **Search terms are not loggable PII** (a term may be an email or a username): log the resource and
  the window, not the term (§10).

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
  and version (§13) — never a `Map` or an `ObjectNode`. Never publish persistence rows or
  internal domain objects directly.
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
- **Validation failures** → `422` with a list of field errors, not one big message.
- **The status mapping is fixed and shared** (`ProblemDetailMapper`): `NotFoundException` → `404`,
  `ConflictException` → `409`, `ValidationException` → `422`, `AccessDeniedException` → `403`,
  anything unexpected → `500`. Application code does not produce `400`: it comes from the framework
  when a request cannot be parsed.


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
- **One logging configuration, owned by the service**: each service keeps a single `logback.xml` in
  its own `src/main/resources` (stdout, level from `LOG_LEVEL`, default `INFO`). A library must
  never ship one — a `logback.xml` inside a library jar becomes a second candidate on the classpath
  of every service that depends on it, so which configuration wins is a classpath-ordering accident
  rather than a decision. A library that needs logging in its tests keeps a test-scoped
  `src/test/resources/logback-test.xml` (never packaged, and Logback loads it ahead of
  `logback.xml`) plus a test-scoped logging backend.

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

## 13. JSON & Typed Payloads

Every JSON document that crosses a boundary — a REST request/response body, a Realtime
broadcast payload, a config file, or a third-party API (Supabase Auth/GoTrue, the Realtime
broadcast API) — is modelled as a **record** (or a sealed interface of records) *before* the
rest of the code sees it. An ad-hoc `Map`/`JsonNode` shape is not a model: it loses the
compiler, the validator, and the documentation, and it defers every field-name typo to
runtime.

### Rules

- **One record per wire shape.** Name it for the payload (`CreateItemRequest`, `ItemDto`,
  `CreateUserRequest`) and declare it where it is produced — request/response DTOs in `api`,
  adapter-local records beside the client that consumes them (§5). Components are the actual
  fields; no "rest" map for the leftovers.
- **No untyped containers as payload types.** `Map<String, Object>`, `Map<String, String>`,
  `List<Map<…>>`, `Object`, and `JsonNode`/`ObjectNode`/`ArrayNode` are not acceptable as a
  method parameter/return type, a field, or a record component. The moment you write
  `node.path("id").asText()` or `String.valueOf(map.get("id"))`, you are missing a record.
- **JSON names live in one place.** Map them explicitly (`@JsonProperty("email_confirm")`) or
  with a naming strategy on the record
  (`@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` when the whole payload is
  snake_case) instead of repeating string keys at every call site.
- **Serialize through the injected `ObjectMapper`** (§6 — the singleton `WebModule`
  configures): `readValue(body, MyDto.class)`, `writeValueAsString(dto)`, and
  `new TypeReference<List<ItemDto>>() {}` for generic envelopes. Never concatenate JSON
  strings, and never build a `Map` only to serialize it.
- **Adapters return records, not trees.** An outbound client (`SupabaseHttpAdminClient`, a
  Realtime publisher) converts a third-party response into records before returning. If
  generic traversal is genuinely unavoidable, keep the `JsonNode` **private to the adapter**
  (a package-private helper taking `JsonNode`), say why in a comment, and convert to a record
  before the value leaves.
- **Config is typed.** A `ConfigLoader` may walk a document tree to apply per-key file/env
  precedence, but it resolves into the immutable `AppConfig` record (§5); the tree must not
  escape the loader.
- **Validate on the way in** with Jakarta Validation on the request record (§8), so a missing
  or malformed field fails as a `422` at the boundary rather than as a `ClassCastException` or
  `NPE` deeper in the service.
- **Tolerate unknown fields** on third-party payloads (`@JsonIgnoreProperties(ignoreUnknown = true)`)
  so a provider adding a field does not break parsing.
- **`jsonb` columns**: read them through jOOQ's generated record or `into(...)` and map into a
  record — do not leak `JsonNode` out of `persistence`.
- **List envelopes are records too.** A paged list is `Page<T>` and a picker set is `OptionList<T>`
  (`com.chetana.keystone.common.query`, §8): the envelope is a typed record with typed items, never a
  `Map` assembled per handler and never a `JsonNode` reshaped at the edge.
- **The same shape is mirrored on the client**: the response DTO is the contract the Flutter
  model declares (`docs/CODING_GUIDELINES_FRONTEND.md` §14).

### Allowed exceptions

1. The `ObjectMapper` itself is injected infrastructure; this rule is about payload *shapes*,
   not about Jackson.
2. Generic tree traversal kept **internal** to a config loader or a third-party adapter, as
   described above.
3. Opaque JSON the application only stores and forwards verbatim (genuinely arbitrary blobs),
   confined to one adapter and documented as such.

Anything else needs a reviewed justification in the PR, with the reason stated in a comment.

### Example — wrong

```java
// Anti-pattern: the Map *is* the DTO. Keys are unchecked strings; validation,
// documentation, and rename-safety are gone.
Map<String, Object> body = Map.of(
        "email", email,
        "password", password,
        "email_confirm", true);
objectMapper.writeValueAsString(body);

// Anti-pattern: JsonNode escapes the adapter into the service/domain.
JsonNode page = objectMapper.readTree(response.body());
for (JsonNode user : page.path("users")) {
    if (email.equalsIgnoreCase(user.path("email").asText())) {
        return user.path("id").asText();
    }
}
```

### Example — right

```java
/** GoTrue "create user" request body (Supabase Auth admin API). */
record CreateUserRequest(
        String email,
        String password,
        @JsonProperty("email_confirm") boolean emailConfirm) {
}

/** One entry of the GoTrue "list users" response body. */
record GoTrueUser(String id, String email) {
}

/** GoTrue "list users" response body: {@code { "users": [...] }}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ListUsersResponse(List<GoTrueUser> users) {
}

// Call site: named, compiler-checked components — no string keys.
objectMapper.writeValueAsString(new CreateUserRequest(email, password, true));

// Adapter boundary: parse once into records, return a plain value.
private Optional<String> findSub(String responseBody, String email) throws IOException {
    return objectMapper.readValue(responseBody, ListUsersResponse.class).users().stream()
            .filter(user -> email.equalsIgnoreCase(user.email()))
            .map(GoTrueUser::id)
            .findFirst();
}
```

## 14. Code Review Checklist

Before merging, confirm:

- [ ] Uses Java 25 features appropriately (records, pattern matching, sealed types, …)
- [ ] Functional style used unless a justified exception exists
- [ ] No preview features, no string templates, no `null` in public APIs
- [ ] No deprecated JDK/Guice/Javalin/jOOQ/third-party APIs; build free of deprecation warnings
- [ ] Persistence rows/DAOs are not leaked into handlers; DTO records are used at the boundary
- [ ] No `Map<String, Object>`/`JsonNode` payloads — every JSON body, broadcast payload, config
      value, and third-party API response is a record (§13)
- [ ] Request DTOs are validated and unknown JSON fields are tolerated on third-party payloads
- [ ] Transactions are at the service boundary; N+1 traps avoided (explicit joins)
- [ ] Constructor injection only; no static `Injector`/service-locator; modules are explicit
- [ ] Behavior lives on `@Singleton` instance methods, not `static`; `static` only for pure functions and a thin `main`
- [ ] Constructors with >7 parameters use a hand-written builder; large records are decomposed
- [ ] jOOQ types generated from the Liquibase changelog; no ad-hoc DDL
- [ ] Pub/sub payloads are immutable, versioned records (not persistence types)
- [ ] Errors are centralized (one global handler → RFC 9457 problem+json), no leaked internals; a
      rejected value answers `422` (`400` only for an unparseable request)
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


# Keystone — Coding Guidelines

Enterprise-grade coding standards for the **Keystone** application. These rules are
normative: code that deviates from them must either be fixed or carry an explicit,
reviewed justification.

## 1. Platform & Toolchain

| Concern | Choice |
| --- | --- |
| Language | **Java 25 (LTS)** — pinned; upgrade the JDK only when explicitly requested |
| Runtime | Virtual threads enabled by default (see §6) |
| Framework | **Spring Boot 4.x** (Spring Framework 7, Jakarta EE 11) |
| Persistence | **JPA (Jakarta Persistence 3.2)** with **Hibernate ORM 7** as the provider |
| Database | **PostgreSQL** — the single relational database (see §7) |
| Communication | REST request/response + STOMP (RabbitMQ) pub/sub (see §8) |
| API Gateway | **nginx** — terminates TLS and fronts all services (see §8) |
| Build | **Maven *and* Gradle** — both are maintained and kept in sync |
| IDE | IntelliJ IDEA (shared `.editorconfig` committed to the repo) |

> **JDK policy.** The Java version is pinned in the build files and the IntelliJ SDK. Do
> not bump it opportunistically; upgrade the JDK only when explicitly requested.

> **Version discipline.** Spring Boot 4 is a major release. Do not copy Spring Boot 2.x/3.x
> idioms (old `WebClient` defaults, deprecated auto-configuration, `spring.factories`,
> pre-`RestClient` code). Verify against the Spring Boot 4 / Spring Framework 7 docs.

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
  with `I`. Implementation suffix only when it adds information (`JpaOrderRepository`,
  `H2…`), never `OrderServiceImpl`.
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
and messaging concerns; cross-cutting infrastructure lives in a shared module/package.

```
com.acme.keystone
├── KeystoneApplication.java          # @SpringBootApplication
├── common/                           # shared DTOs, errors, util, clock, id-gen
├── order/                            # feature: orders
│   ├── api/                          #   REST controllers + DTOs (request/response)
│   ├── application/                  #   use-case services, ports (interfaces)
│   ├── domain/                       #   entities, value objects, domain rules
│   └── persistence/                  #   JPA repositories + adapters
├── notification/                     # feature: notifications (pub/sub)
│   └── ...
└── config/                           # cross-cutting @Configuration, security, observability
```

### Layering rules

- **`api`** talks only to **`application`** (ports). It never reaches `persistence` directly.
- **`domain`** is framework-agnostic — no Spring, no JPA imports (except annotations on
  entities when using JPA-mapped entities). Pure Java + tests.
- **`application`** defines **ports** (interfaces) that `persistence` implements — so the
  service layer depends on abstractions, not on Hibernate/Spring Data directly.
- **DTOs are records** and are defined where they are produced (request DTOs in `api`,
  response/projection DTOs may live in `application`).
- Mapping between entity ↔ DTO is explicit (a `*Mapper` component or a constructor/method);
  never expose JPA entities from `api` controllers.

## 6. Spring Boot Conventions

- **Constructor injection only.** No `@Autowired` field/setter injection. Lombok is
  discouraged — write the constructor, or use compact constructors on records.
- **Controllers**: thin `@RestController`s. Validate input via Jakarta Validation
  (`@Valid`, `@NotBlank`, …) on record DTOs. Return typed DTOs, never entities or `Map`.
- **Configuration**: bind external properties with `@ConfigurationProperties` records
  (`@Validated` where needed). Avoid `@Value` for grouped config.
- **Clients**: use `RestClient` for straightforward HTTP, and **HTTP Service Clients
  (`@HttpExchange`)** for declarative server-to-server APIs. Avoid hand-rolled
  `HttpClient`/`WebClient` boilerplate.
- **Beans**: name beans explicitly when there are multiple candidates
  (`@Qualifier` + meaningful names). Prefer `@Bean` factory methods over component-scan
  magic for infrastructure.
- **Profiles & config**: externalize everything (`application.yml`, env vars). No secrets
  in source. Use `application-{profile}.yml` for environment overrides.
- **Transactions**: annotate `application` service boundaries with `@Transactional`, not
  controllers and not repositories (§7).
- **Virtual threads**: set `spring.threads.virtual.enabled=true`. Keep blocking I/O
  (JDBC, HTTP) on virtual threads; do not pin them with `synchronized` hot blocks.


## 7. Persistence (JPA / Hibernate)

JPA entities are the **one deliberate exception** to immutability — Hibernate requires a
no-arg constructor and mutable state. Keep that exception contained to `persistence`.

- **JPA-first, vendor-neutral**: write against the standard Jakarta Persistence API — JPQL,
  the Criteria API, `EntityManager`, and Spring Data repository methods — not
  Hibernate-specific APIs (`org.hibernate.*`, `Session`, HQL-only features). This keeps the
  code portable across JPA providers.
- **Vendor specifics only when justified**: use a Hibernate-specific API only when it gives
  measurably better performance *and* there is no JPA-standard alternative. When you do,
  confine it to the `persistence` layer behind a port so the rest of the app stays
  vendor-neutral.
- **Entities** live in `domain` or `persistence`; they are *not* returned by controllers.
  Return DTO/projection records instead.
- **Every aggregate root** has `@Version` for optimistic locking and a generated `Long`
  or `UUID` id. Use a PostgreSQL `SEQUENCE` (via `@SequenceGenerator`) or `uuid`; avoid
  `IDENTITY` (it defeats JDBC batching).
- **Lazy loading is the default** (`FetchType.LAZY`). Avoid `EAGER` on `@*ToMany`.
- **Avoid the N+1 trap.** For reads, use fetch joins in explicit `@Query`, DTO projections
  (`select new com.acme…Dto(...)`), or `@EntityGraph` — not eager fetching.
- **Transactions**: `@Transactional(readOnly = true)` for queries, write transactions only
  around the service method that mutates state. Never on controllers. Prefer programmatic
  `TransactionTemplate` for multi-step, conditional work.
- **Value objects**: model them with `@Embeddable` rather than primitive obsession.
- **No `@Data`/`@EqualsAndHashCode` on entities** (Lombok discouraged anyway); implement
  `equals/hashCode` on the business key if needed.
- **Migrations** with **Liquibase** — the schema is versioned via changelogs, never
  `ddl-auto=update` outside local dev scratch databases.
- **Batching**: enable JDBC batching and `hibernate.jdbc.batch_size` for bulk writes.
- **PostgreSQL types**: use native types — `jsonb` for JSON, `uuid` for UUID keys, and
  `timestamptz` (`timestamp with time zone`) for timestamps. Map `jsonb` columns with
  `@JdbcTypeCode(SqlTypes.JSON)` or a JSON mapper; never store JSON as plain text.
- **Dialect**: let Hibernate auto-detect `PostgreSQLDialect`; do not hardcode the dialect
  unless a specific feature requires it.
- **Cross-service cache sync**: do not synchronize a shared Hibernate L2 cache across
  services. Invalidate caches via domain events over RabbitMQ (see
  `docs/adr/0001-event-driven-cache-invalidation.md`).

### Connection pool (HikariCP)

Spring Boot ships **HikariCP** as the default pool — use it; do not swap pools without a
reason.

- **Pool sizing**: the pool is the concurrency guard for blocking JDBC on virtual threads.
  Size `spring.datasource.hikari.maximum-pool-size` against PostgreSQL's `max_connections`,
  not CPU count. Start modest (e.g. 10–20 per service instance) and tune from metrics; a
  large pool does not add throughput and can exhaust Postgres connections across services.
- **Timeouts**: set `connection-timeout` (fail fast — do not let threads block forever),
  and `max-lifetime` a few seconds *below* the shortest DB/proxy idle timeout so the pool
  recycles connections before infrastructure drops them.
- **Keepalive & leak detection**: set `keepalive-time` where idle connections traverse a
  load balancer/firewall; set `leak-detection-threshold` in dev/staging to catch unclosed
  connections early.
- **Metrics & observability**: give the pool a stable `pool-name` and expose HikariCP
  metrics via Micrometer (`hikaricp.connections.*`). Alert on connection-timeout spikes and
  pool exhaustion (`hikaricp.connections.pending`).
- **No connection-test-query**: rely on JDBC 4 `Connection.isValid()` for PostgreSQL; do
  not set a legacy `connection-test-query`.

## 8. Communication: Request/Response & Pub/Sub

Keystone uses two interaction styles, both fronted by **nginx** as the API gateway.
nginx terminates TLS, sits in front of every service, and routes both REST traffic and
WebSocket upgrades.

### Request/Response (REST)

- JSON over HTTP. `@RestController` + records for request/response bodies.
- Version the API in the path (`/api/v1/…`). Use nouns, not verbs, in resource paths.
- Return proper HTTP semantics: `201` for created (with `Location`), `204` for no content,
  `404`/`409`/`422` for the right failure, `ProblemDetail` (RFC 9457) for errors.
- Paginate list endpoints (`page`, `size`, `sort`) and return a typed page wrapper.

### Pub/Sub (STOMP over RabbitMQ)

**STOMP with RabbitMQ as the broker** is the single pub/sub mechanism, used for **both**
directions:

- **UI ↔ process**: STOMP over WebSocket. Clients subscribe to destinations (e.g.
  `/topic/orders.{id}`); the server publishes typed event records.
- **Process ↔ process**: STOMP over TCP against the same RabbitMQ broker (via the STOMP
  plugin), for decoupled, durable service-to-service messaging.

Conventions:

- **In-process events**: use `ApplicationEventPublisher` + `@EventListener` (or
  `@TransactionalEventListener` for post-commit) — no broker hop for intra-process pub/sub.
- **Destinations** are namespaced and versioned: `/topic/…` for fan-out, `/queue/…` for
  point-to-point. Document each destination.
- **Event payloads are immutable records** with an event id, timestamp, aggregate id, and
  version. Never publish JPA entities or internal domain objects directly.
- **Broker wiring**: configure RabbitMQ via `spring.rabbitmq.*`; enable the STOMP broker
  relay (`enable-stomp-broker-relay`) so WebSocket and TCP STOMP share the same broker.

## 9. Error Handling

- **Exception hierarchy**: a single `KeystoneException` base (unchecked) with typed
  subclasses (`NotFoundException`, `ConflictException`, `ValidationException`,
  `AccessDeniedException`) carrying an error code.
- **Do not leak internals**: never return stack traces or SQL to clients.
- **Central handler**: a single `@RestControllerAdvice` maps exceptions to
  `ProblemDetail` (status, type, title, detail, instance, and an app-specific `code`).
- **Log at the right level**: `WARN` for expected business failures, `ERROR` for
  unexpected ones (with stack trace). Re-throw as-is — don't wrap-and-hide.
- **Validation failures** → `400` with a list of field errors, not one big message.


## 10. Logging & Observability

- **SLF4J** (Logback via Spring Boot). Structured/key-value logging for machine parsing.
- **Per-class logger**, but prefer constructor-injected services over logger-per-everything.
- **Request context**: propagate `traceId`/`tenantId` via a **`ScopedValue`** (not
  `ThreadLocal`) bound at the filter/entry point.
- **Metrics** via Micrometer: expose `/actuator/metrics`, add `@Timed`/counters on
  meaningful business operations.
- **Log levels**: `DEBUG` for flow, `INFO` for lifecycle/business milestones, `WARN` for
  recoverable anomalies, `ERROR` for failures needing action.
- **Never log**: passwords, tokens, keys, PII (unless masked and legally required).

## 11. Testing

- **JUnit 5** + **AssertJ** + **Mockito**. Testcontainers for real dependency testing.
- **Test pyramid**: many fast unit tests, fewer integration/slice tests, a handful of E2E.
- **Unit tests**: pure JUnit + AssertJ, no Spring context. Test the functional core
  (domain/services) heavily.
- **Slice tests** for web/data:
  - `@WebMvcTest` — controllers, validation, and `@RestControllerAdvice` error mapping.
  - `@DataJpaTest` — repository queries against a PostgreSQL container (Testcontainers).
    Do not use H2: it does not faithfully reproduce PostgreSQL behavior (types, `jsonb`,
    sequences).
- **Integration**: `@SpringBootTest` with Testcontainers for the full stack (PostgreSQL +
  RabbitMQ) for pub/sub flows.
- **Naming**: `should_<behavior>_when_<condition>`. Arrange–Act–Assert, one behavior per test.
- **Fixtures**: factory methods/records, not giant shared test data. Keep tests deterministic
  and independent (no order/global-state coupling).
- **Coverage**: meaningful coverage (branch on core domain logic), not vanity percentage.

## 12. Security

- **Spring Security** with OAuth2/OpenID Connect (resource server) is the default. Never
  roll your own crypto/auth.
- **Authorization** at the service/method level (`@PreAuthorize`) *and* at the data layer
  where multi-tenancy requires it.
- **Validate all input**; treat every request and every pub/sub payload as untrusted.
- **Store secrets in the environment/secrets manager**, never in source or images.
- **OWASP awareness**: parameterized queries (JPA handles it), no dynamic SQL
  concatenation, sanitize output, protect against mass-assignment by never binding
  directly to entities from DTOs.

## 13. Code Review Checklist

Before merging, confirm:

- [ ] Uses Java 25 features appropriately (records, pattern matching, sealed types, …)
- [ ] Functional style used unless a justified exception exists
- [ ] No preview features, no string templates, no `null` in public APIs
- [ ] Entities are not leaked into controllers; DTO records are used at the boundary
- [ ] Transactions are at the service boundary; N+1 and eager-loading traps avoided
- [ ] Pub/sub payloads are immutable, versioned records (not entities)
- [ ] Errors are centralized (`@RestControllerAdvice` + `ProblemDetail`), no leaked internals
- [ ] Tests added and passing (`mvn test` / `gradle test`)
- [ ] No secrets logged or committed; schema changes are via a Liquibase changelog migration
- [ ] `.editorconfig`/formatter applied (no formatting churn)

## References

- Spring Boot 4.x / Spring Framework 7 reference documentation
- Jakarta Persistence 3.2 & Hibernate ORM 7 docs
- JDK 25 JEP list: <https://openjdk.org/projects/jdk/25/>
- ADR-0001: Cross-service cache invalidation (`docs/adr/0001-event-driven-cache-invalidation.md`)


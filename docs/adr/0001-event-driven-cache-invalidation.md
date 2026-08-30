# ADR-0001: Cross-Service Cache Invalidation via Event-Driven Messages

- **Status:** Accepted
- **Date:** 2026-08-31
- **Deciders:** Keystone engineering team

## Context

Keystone is a multi-service, enterprise Spring Boot 4 application on PostgreSQL, with
RabbitMQ (STOMP) already in the stack for pub/sub. Some services cache data locally, and we
needed a strategy to keep those caches consistent across services when data changes.

We considered enabling Hibernate second-level cache (L2) on specific tables and syncing it
across services. Hibernate L2 cache is JVM-local by default; keeping it consistent across
processes requires a distributed cache provider (Hazelcast / Infinispan / Redisson). Such a
cache is only consistent for writes that go through Hibernate on a member of the same cache
cluster — it silently goes stale on direct SQL, Liquibase/job writes, or writes from a
service that does not share the cache cluster.

## Decision

Use **event-driven cache invalidation over RabbitMQ** for cross-service cache
synchronization.

- A service that mutates data publishes a domain/change event after the change commits.
- Each service that caches that data subscribes to the relevant events and invalidates (or
  refreshes) its own local cache.
- RabbitMQ is the transport, consistent with the existing pub/sub standard (STOMP over
  RabbitMQ — see `docs/CODING_GUIDELINES_BACKEND.md` §8).
- Publish events reliably with the **transactional outbox pattern** (write the event to an
  outbox table in the same DB transaction as the data change; a relay publishes it), to
  avoid the dual-write problem.
- Consumers are **idempotent** and tolerate out-of-order/duplicate events.
- **Hibernate L2 cache** is allowed only for read-only reference data *within a single
  service* (its own replica set); it is **not** used to synchronize mutable data across
  services.

## Alternatives Considered

1. **Distributed Hibernate L2 cache (Hazelcast / Infinispan / Redisson)** — rejected. Only
   consistent for Hibernate-mediated writes within a shared cluster; fragile across
   services; vendor integration compatibility with Hibernate 7 is uneven.
2. **Change Data Capture (Debezium on the PostgreSQL WAL)** — viable and writer-agnostic,
   but adds Kafka (or Debezium Server) complexity that is unnecessary while RabbitMQ is
   already present. Revisit if event coverage from application code proves insufficient.
3. **Shared read-through cache (Redis)** — not needed as a baseline; can complement the
   event-driven approach later if a shared read model is required.

## Consequences

### Positive

- Writer-agnostic: any service that publishes the right event triggers invalidation, no
  matter who wrote the row.
- Loose coupling: no shared cache cluster, no cross-service Hibernate cache dependency.
- Fits the existing stack (RabbitMQ + STOMP) and the event-payload conventions in the
  coding guidelines.
- Deterministic and auditable (events carry id, timestamp, aggregate id, version).

### Negative / Watch-outs

- Eventual consistency: there is a brief window in which a cache may serve stale data.
- Requires the transactional outbox pattern and idempotent consumers (more moving parts).
- Event schemas must be versioned and maintained (immutable record payloads).
- Caches must tolerate misses and rebuild from PostgreSQL; only add caching where there is
  a measured need (avoid premature caching).

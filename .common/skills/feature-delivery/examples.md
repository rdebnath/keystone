# Example — "Allow order cancellation"

Request: *"Allow users to cancel an order that hasn't shipped yet."*

## Sizing

Feature spanning DB, domain, API, messaging, UI, and testing → produce a plan.

## Delivery plan

### Phase 1 — Discovery & design

- Confirm rules: cancellable only when status is `PENDING` or `CONFIRMED`; publish an event
  on cancellation.
- Contracts: `POST /api/v1/orders/{id}/cancellation` → `204`; event `OrderCancelled` on
  `/topic/orders.{id}`.
- Open question: refund handling — out of scope for this change?

### Phase 2 — Database changes

- Liquibase changelog: add `orders.cancelled_at` column; adjust status constraint.
- Update `Order` entity with the new field; keep `@Version` optimistic locking.
- Add repository method `findByIdAndStatusIn(...)`.

### Phase 3 — Domain & application services

- Add a `CancelOrder` use-case service; enforce the state machine in the domain.
- Raise an `OrderCancelled` domain event via `ApplicationEventPublisher`.

### Phase 4 — Server-side API & messaging

- `OrderController.cancel(...)` maps to the service, returns `204`; failures map to
  `ProblemDetail`.
- STOMP publisher publishes the `OrderCancelled` record to `/topic/orders.{id}`.
- nginx: no change (WebSocket already routed).

### Phase 5 — Frontend / UI (Flutter)

- Add a "Cancel order" action to the order detail screen with a confirm dialog; disable it
  unless the order status is `PENDING` or `CONFIRMED`.
- Call `POST /api/v1/orders/{id}/cancellation` via `dio`; show loading / success / error.
- Subscribe to `/topic/orders.{id}` with `stomp_dart_client` and update Riverpod state from
  a `freezed` model so cancellation reflects in real time.

### Phase 6 — Security & observability

- Authorize with `@PreAuthorize` (owner or admin only).
- Structured log with traceId/tenantId; increment an `orders.cancelled` counter.

### Phase 7 — Testing

- Unit: state machine and service.
- Slice: `@WebMvcTest` for controller + error mapping; `@DataJpaTest` for the query.
- Integration: `@SpringBootTest` + Testcontainers + RabbitMQ STOMP; assert the event is
  published and received.

### Phase 8 — Delivery

- Changelog entry; migration note (new column needs a default/backfill for existing rows).

## Files generated

All of the above is written to `docs/feature/order-cancellation/`:

- `plan.md`
- `01-discovery-and-design.md`
- `02-database-changes.md`
- `03-domain-and-application-services.md`
- `04-server-side-api-and-messaging.md`
- `05-frontend-ui-flutter.md`
- `06-security-and-observability.md`
- `07-testing.md`
- `08-delivery.md`

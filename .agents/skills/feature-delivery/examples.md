# Example — "Allow order cancellation"

Request: *"Allow users to cancel an order that hasn't shipped yet."*

## Sizing

Feature spanning DB, domain, API, realtime, UI, and testing → produce a plan.

## Delivery plan

### Phase 1 — Discovery & design

- Confirm rules: cancellable only when status is `PENDING` or `CONFIRMED`; broadcast an
  event on cancellation.
- Contracts: `POST /api/v1/orders/{id}/cancellation` → `204`; broadcast `OrderCancelled` on
  Realtime channel `orders.{id}`.
- Open question: refund handling — out of scope for this change?

### Phase 2 — Database changes

- Liquibase changelog: add `orders.cancelled_at` column; adjust status constraint.
- Regenerate jOOQ types from the changelog (offline codegen).
- Add DAO method `findByIdAndStatusIn(...)`.

### Phase 3 — Domain & application services

- Add a `CancelOrder` use-case service; enforce the state machine in the domain.
- Raise an `OrderCancelled` domain event from the use-case and publish it through the
  `RealtimePublisher` port.

### Phase 4 — Server-side API & realtime

- A Javalin `OrderHandler.cancel(...)` maps to the service, returns `204`; failures map to
  an RFC 9457 problem+json body.
- Publish the `OrderCancelled` record to Supabase Realtime channel `orders.{id}` via the
  `RealtimePublisher` (service-role key server-side; idempotent publish with retry/backoff).

### Phase 5 — Frontend / UI (Flutter)

- Add a "Cancel order" action to the order detail screen with a confirm dialog; disable it
  unless the order status is `PENDING` or `CONFIRMED`.
- Call `POST /api/v1/orders/{id}/cancellation` via `dio`; show loading / success / error.
- Subscribe to the Supabase Realtime channel `orders.{id}` and update Riverpod state from a
  `freezed` model so cancellation reflects in real time.

### Phase 6 — Security & observability

- Authorize (owner or admin only) at the service boundary.
- Structured log with traceId/tenantId; increment an `orders.cancelled` counter.

### Phase 7 — Testing

- Unit: state machine and service.
- Slice: test the Javalin handler + error mapping; test the jOOQ DAO query against a
  PostgreSQL container.
- Integration: real Guice `Injector` + embedded server + Testcontainers; assert the event is
  broadcast and received (Supabase Realtime).

### Phase 8 — Delivery

- Changelog entry; migration note (new column needs a default/backfill for existing rows).

## Files generated

The change above is app-specific, so all artifacts are written under the app's docs
directory. With the current `inventory` app the path is
`apps/inventory/docs/delivery/order-cancellation/`:

- `plan.md`
- `01-discovery-and-design.md`
- `02-database-changes.md`
- `03-domain-and-application-services.md`
- `04-server-side-api-and-realtime.md`
- `05-frontend-ui-flutter.md`
- `06-security-and-observability.md`
- `07-testing.md`
- `08-delivery.md`

If the same work were a platform-level change (touching `platform/keystone-*`), it would
instead be written to `docs/delivery/order-cancellation/`.

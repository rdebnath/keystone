# Phase 4 — Server-side API & realtime (typed wire records)

**Scope** — Remove every `Map`/`JsonNode` payload from the Supabase Auth adapter and the Realtime
publisher.

**Artifacts**
- `platform/keystone-admin/src/main/java/com/chetana/keystone/platform/admin/supabase/` (new, one
  type per file): `CreateUserRequest`, `PasswordGrantRequest`, `UpdatePasswordRequest`,
  `GoTrueUser`, `ListUsersPage`, `TokenResponse`.
- `…/supabase/SupabaseHttpAdminClient.java` (modify) — serialize `new Create*Request(...)`,
  parse with `objectMapper.readValue(body, …)`; `findSubInPage(JsonNode, String)` →
  `findSub(ListUsersPage, String): Optional<String>`.
- `platform/keystone-realtime/src/main/java/com/chetana/keystone/realtime/RealtimeEnvelope.java`
  (new) — `record RealtimeEnvelope<T>(String channel, String event, T payload)` with non-null
  components.
- `…/realtime/RealtimePublisher.java` (modify) — `void publish(RealtimeEnvelope<?> envelope)`.
- `…/realtime/SupabaseRealtimePublisher.java` (modify) — `writeValueAsString(envelope)` instead of
  `Map.of("channel", …, "event", …, "payload", …)`.

**Dependencies** — Phase 1 (the `ObjectMapper` singleton already comes from `WebModule`).

**Verification** — module compiles; `GoTrueWireRecordsTest` proves the exact wire JSON (including
`email_confirm` and the snake_case token fields); `RealtimeEnvelopeTest` proves the broadcast body
shape is unchanged from the old `Map`.

## Notes

- Wire records are package-private: they are GoTrue's shapes, not platform API.
- `@JsonIgnoreProperties(ignoreUnknown = true)` is required on every response record — Jackson fails
  on unknown properties by default, and adding it is what makes a provider-side field addition
  harmless.
- `TokenResponse` uses `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` (not the
  deprecated `PropertyNamingStrategy.SNAKE_CASE`), and normalizes a missing
  `refresh_token`/`token_type` to `""` to preserve the previous response values.
- `Session` is deliberately *not* re-annotated for GoTrue: it is the REST response DTO the Flutter
  client reads as camelCase, so the wire record maps into it instead.
- A missing `id` on create now fails loudly (`IllegalStateException`) instead of returning `""`,
  and a token response with no `access_token` is rejected explicitly.

## Results

- 6 wire records + `RealtimeEnvelope` added; `JsonNode`/`Map` imports removed from both adapters
  (the only remaining `JsonNode` in the repo is inside the two conforming config loaders).
- `RealtimePublisher` has no callers yet (the endpoint shape is still an open question per
  `ARCHITECTURE.md` §12), so the signature change has no ripple.
- Verified: `mvn -o … test` — see `07-testing.md`.

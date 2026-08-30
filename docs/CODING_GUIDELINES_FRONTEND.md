# Keystone — Frontend Coding Guidelines (Flutter)

Enterprise-grade coding standards for the **Keystone client**. These rules are normative,
mirroring the backend guidelines (`docs/CODING_GUIDELINES_BACKEND.md`).

## 1. Platform & Toolchain

| Concern | Choice |
| --- | --- |
| Framework | **Flutter** — one codebase for web, iOS, Android |
| Language | **Dart 3** (sound null safety) |
| State management | **Riverpod** (codegen via `riverpod_generator`) |
| Models | **freezed** + **json_serializable** (immutable, value-typed) |
| REST client | **dio** |
| Real-time | **stomp_dart_client** (STOMP over WebSocket → RabbitMQ) |
| Routing | **go_router** (declarative, typed routes) |
| Auth | **flutter_appauth** (OAuth2/OpenID Connect, PKCE) |
| Lints / format | `flutter_lints` + `dart format`; `pubspec.lock` committed |

## 2. Language: Use Dart 3 Features

Prefer these *stable* Dart 3 features:

- **Sound null safety** — avoid `!` (bang); prefer `?.`, `??`, `?[]`, and explicit handling.
- **Sealed classes + switch expressions** — exhaustive dispatch (mirrors Java sealed types
  and pattern matching); the compiler checks exhaustiveness.
- **Records** — for small, positional, value data.
- **Pattern matching** — object, list, and logical patterns in `switch`/`if-case`.
- **Enhanced enums** — enums with fields and methods.
- **Class modifiers** (`sealed`, `final`, `base`, `interface`) — control inheritance and
  extension by design.
- **`const` constructors** everywhere possible (widgets, models, literals).

Avoid:

- `dynamic` and raw `Map<String, dynamic>` in application code — parse into typed models.
- `!` null-assertions where the null case can be handled explicitly.
- `late` fields unless truly lazy; prefer initialization at declaration.
- Mutable global singletons.

## 3. Programming Style: Functional & Immutable

Write in a functional, immutable style, matching the backend ethos.

- **Immutable state**: `freezed` data classes, `final` fields, `const` constructors,
  `List.unmodifiable`/`Set.unmodifiable`.
- **Pure functions** for business logic; keep side effects at the edges (repositories,
  clients, listeners).
- **Model results as sealed/union types** (`freezed` unions) rather than throwing for
  expected failures.
- **No shared mutable state**: use Riverpod providers for shared/derived state.
- **Collection `for`/`if`** in literals and widget trees instead of imperative building.
- **Fluent chains**: one method call per line; use cascades (`..`) for sequential setup,
  one operation per line.

## 4. Naming & Formatting

- **Files/folders**: `snake_case` (Dart convention).
- **Types/widgets**: `UpperCamelCase`; widget names convey purpose
  (`OrderDetailScreen`, `CancelOrderButton`).
- **Variables/functions**: `lowerCamelCase`; private members prefixed with `_`.
- **Constants**: `lowerCamelCase` (Dart convention) — e.g. `maxRetries`, not `MAX_RETRIES`.
- **Format**: `dart format` (enforced in CI); keep line length 120 to match the backend.
- **Lints**: `flutter_lints` as the baseline; escalate to `very_good_analysis` if desired.
- One top-level public type per file where practical; filename matches the type.

## 5. Project Structure (Feature-First)

Mirror the backend: package by feature, not by layer.

```
lib/
├── main.dart
├── app/                    # bootstrap, router (go_router), theme, providers
├── core/                   # shared: config, network, auth, error, logging
│   ├── network/            #   dio client, interceptors, stomp client
│   ├── auth/               #   appauth, secure token storage
│   └── error/              #   failure types and error mapping
└── features/
    ├── orders/
    │   ├── data/           #   models (freezed), DTOs, repositories
    │   ├── application/    #   Riverpod providers (notifiers/use-cases)
    │   └── presentation/   #   screens, widgets
    └── notifications/
        └── ...
```

Layering rules:

- `presentation` talks only to `application` (providers), never to `data` clients directly.
- `application` orchestrates `data` repositories; no I/O or HTTP here.
- `data` owns models, DTO mapping, and all I/O (`dio`, `stomp_dart_client`).
- Models are immutable; the UI emits a new state instead of mutating a model.

## 6. State Management (Riverpod)

- Use **Riverpod with codegen**: annotate `@riverpod` and generate via
  `riverpod_generator`.
- Prefer **`Notifier` / `AsyncNotifier`** (Riverpod 2+) over legacy `StateNotifier`.
- State types are **immutable `freezed`** classes; a transition returns a new state, never
  mutates in place.
- Represent async as **`AsyncValue`** (`AsyncLoading` / `AsyncData` / `AsyncError`) via
  `FutureProvider`/`StreamProvider`.
- Keep providers granular and per-feature; derive state with `Provider`/`select` instead of
  duplicating it.

## 7. Networking

- **REST** via `dio`: one shared instance with interceptors for auth (bearer), logging,
  retry, and timeouts.
- Follow **use-case → repository → client**: UI → use-case → repository → `dio`/STOMP.
- DTOs are `freezed` classes with `json_serializable` `fromJson`/`toJson`; map DTO ↔ domain
  model in `data`.
- **STOMP** via `stomp_dart_client`: subscribe to `/topic/…`, send to `/app/…` or
  `/queue/…` (same nginx-routed RabbitMQ broker as the backend).
- Implement reconnect/backoff for STOMP and handle offline gracefully.
- Never parse JSON into `dynamic` maps in the UI — always through typed models.

## 8. Error Handling

- Model expected failures as sealed/union result types (`freezed`) — do not throw for
  control flow.
- Map transport errors (Dio exceptions, STOMP failures) to domain failures at the
  repository boundary.
- Show user-facing messages from a single error-mapping layer; never leak raw Dio/HTTP
  errors or stack traces to the UI.
- Server `ProblemDetail` (RFC 9457) maps to a typed failure; validation errors surface as
  field-level messages.

## 9. Routing

- `go_router` with typed, declarative routes and typed path parameters.
- Guard authenticated routes with a `redirect` based on auth state.
- Use named route constants; no magic string paths scattered through widgets.

## 10. Security

- OAuth2/OpenID Connect **PKCE** via `flutter_appauth`; bearer token to the Spring Security
  resource server.
- Store tokens in **`flutter_secure_storage`** (Keychain/Keystore), never in
  `shared_preferences`.
- Attach the bearer token via a `dio` interceptor; handle silent token refresh.
- Never log or commit tokens, secrets, or PII.

## 11. Testing

- **Unit**: business logic and Riverpod notifiers using `ProviderContainer` (mock with
  `mocktail`).
- **Widget**: screens/components with `WidgetTester`.
- **Golden**: sparingly, for stable design-system components.
- **Integration**: `integration_test` for critical end-to-end flows (incl. STOMP).
- Naming: `should_<behavior>_when_<condition>` (matches backend). Arrange–Act–Assert;
  deterministic, no shared mutable state between tests.

## 12. Code Review Checklist

Before merging, confirm:

- [ ] Immutable `freezed` models; `const` where possible; no shared mutable state
- [ ] Riverpod `Notifier`/`AsyncNotifier` used; no global mutable singletons
- [ ] Typed models for JSON; no `dynamic` maps in UI; no avoidable `!` assertions
- [ ] `dio`/STOMP access isolated in `data`; UI never talks to clients directly
- [ ] Errors mapped to typed failures; no raw exceptions/stack traces in the UI
- [ ] Tokens in secure storage; no secrets/PII logged
- [ ] Tests added and passing; `flutter analyze` and `dart format` clean
- [ ] Feature-first structure and go_router conventions followed

## References

- Backend guidelines: `docs/CODING_GUIDELINES_BACKEND.md`
- Flutter & Dart documentation: <https://flutter.dev>, <https://dart.dev>
- Riverpod: <https://riverpod.dev>
- freezed: <https://pub.dev/packages/freezed>


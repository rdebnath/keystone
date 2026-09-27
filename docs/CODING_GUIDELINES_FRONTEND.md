# Keystone — Frontend Coding Guidelines (Flutter)

Enterprise-grade coding standards for the **Keystone client** (Flutter — web, iOS, Android).
These rules are normative and align with the system architecture
(`docs/ARCHITECTURE.md`) and mirror the backend guidelines
(`docs/CODING_GUIDELINES_BACKEND.md`). The client talks to the Java service over REST and to
Supabase Realtime over WebSocket; the web build is hosted on Firebase Hosting.

## 1. Platform & Toolchain

| Concern | Choice |
| --- | --- |
| Framework | **Flutter** — one codebase for web, iOS, Android |
| Language | **Dart 3** (sound null safety) |
| State management | **Riverpod** (codegen via `riverpod_generator`) |
| Models | **freezed** + **json_serializable** (immutable, value-typed) |
| REST client | **dio** |
| Real-time | **Supabase Realtime** client (WebSocket) |
| Routing | **go_router** (declarative, typed routes) |
| Auth | **backend-proxied OIDC login** (Supabase Auth as IdP); tokens in `flutter_secure_storage` |
| Web hosting | **Firebase Hosting** (Flutter web build) |
| Lints / format | `flutter_lints` + `dart format`; `pubspec.lock` committed |

> **Deprecation discipline.** Do not use deprecated (`@Deprecated`) APIs, classes, packages,
> or annotations — whether in the Dart SDK, Flutter, Riverpod, or a third-party package. A
> deprecation is a removal warning: migrate to the documented replacement rather than
> suppressing the analyzer warning. Treat a deprecation warning from `flutter analyze` as a
> defect to fix, not noise to ignore.

> **SDK & dependency policy.** Pin the Dart SDK, Flutter, and package versions (a committed
> `pubspec.lock`); do not bump them opportunistically. Upgrade Flutter/Dart or a package only
> when explicitly requested. Declare the Dart SDK constraint as `'>=3.8.0 <4.0.0'` in **every**
> Flutter package: `json_serializable` requires a language version of at least 3.8 (it refuses to
> run below it), and an identical constraint across packages keeps the language version — and
> therefore `dart format`'s style — uniform.

> **Version discipline.** Verify against the current Flutter, Dart, Riverpod, freezed, and
> dio reference documentation. Do not copy older idioms (e.g. `StateNotifier`,
> `ChangeNotifierProvider`) into new code.

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

- `dynamic` and raw `Map<String, dynamic>` in application code — parse into typed models
  (§14). The map is acceptable only as a generated `fromJson`/`toJson` signature and as a
  single, immediately-converted value at the `data` boundary.
- `!` null-assertions where the null case can be handled explicitly.
- `late` fields unless truly lazy; prefer initialization at declaration.
- Mutable global singletons.

## 3. Programming Style: Functional & Immutable

Write in a functional, immutable style, matching the backend ethos.

- **Immutable state**: `freezed` data classes, `final` fields, `const` constructors,
  `List.unmodifiable`/`Set.unmodifiable`.
- **Constructors & named parameters**: a long positional parameter list is order-sensitive
  and easy to mis-call. When a constructor or function has **more than seven parameters**,
  use **named parameters** (mark mandatory ones `required`) — the Dart-native replacement
  for the backend's builder rule, giving named, order-independent call sites. If the
  parameters form a cohesive group, extract a small `freezed` value class (a parameter
  object) instead of flattening them. For **models**, `freezed` classes already use named
  parameters; if one grows past ~seven fields, split related fields into smaller nested
  models rather than a mega-model.
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
│   ├── network/            #   dio client, interceptors, realtime client
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
- `data` owns models, DTO mapping, and all I/O (`dio`, Supabase Realtime).
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

The client talks to the system over two channels (see `docs/ARCHITECTURE.md` §2, §5): REST
(HTTPS/JSON) to the **Java service (Javalin)** and a WebSocket subscription to **Supabase
Realtime**.

### 7.1 REST (dio)

- One shared `dio` instance with interceptors for auth (bearer), correlation id, logging,
  retry, and timeouts.
- Follow **use-case → repository → client**: UI → use-case → repository → `dio`.
- DTOs are `freezed` classes with `json_serializable` `fromJson`/`toJson`; map DTO ↔ domain
  model in `data`.
- Server errors come back as RFC 9457 `application/problem+json`; parse them into a typed
  failure (see §9).
- Never parse JSON into `dynamic` maps in the UI — always through typed models (§14).

### 7.2 Realtime (Supabase Realtime)

- Subscribe to channels over WebSocket and receive messages broadcast by the Java service
  (see `docs/ARCHITECTURE.md` §5.2).
- Use the **anon (publishable) key** and user token on the client; **never** the
  service-role key (server-side only).
- Consume a typed, versioned message envelope (not raw JSON); ignore unknown versions, and
  decode the envelope into a `freezed` model before touching a field (§14).
- Treat received messages as **idempotent** — tolerate duplicates and re-delivery.
- Implement reconnect with exponential backoff and **resubscribe** on reconnect; handle
  offline gracefully.

## 8. Configuration & Environment

- Environment-specific values come in at build time via `--dart-define`
  (`String.fromEnvironment`) or build flavors — never hard-coded per environment.
- Required config: Java API base URL, Supabase URL + anon key, Realtime channel prefix.
- The Supabase **anon (publishable) key** is public and safe to ship; the **service-role
  key** must never appear in a client bundle (web, iOS, or Android) — it stays server-side.
- Firebase Hosting config is public too (no secrets); secrets live in the backend's
  environment / Secret Manager.
- No secrets or environment URLs committed to source; document new config in the env template.

## 9. Error Handling

- Model expected failures as sealed/union result types (`freezed`) — do not throw for
  control flow.
- Map transport errors (Dio exceptions, Realtime failures) to domain failures at the
  repository boundary.
- Map HTTP status to typed failures: 401/403 → auth, 404 → not found, 409 → conflict,
  400/422 → validation (field-level), 429 → retry/backoff, 5xx → transient server error.
- Server `application/problem+json` (RFC 9457) maps to a typed failure; validation errors
  surface as field-level messages.
- Show user-facing messages from a single error-mapping layer; never leak raw Dio/HTTP
  errors or stack traces to the UI.

## 10. Routing

- `go_router` with typed, declarative routes and typed path parameters.
- Guard authenticated routes with a `redirect` based on auth state.
- Use named route constants; no magic string paths scattered through widgets.

## 11. Security

- OAuth2/OpenID Connect **PKCE** via `flutter_appauth`; bearer token to the Java (Javalin)
  resource server.
- Store tokens in **`flutter_secure_storage`** (Keychain/Keystore), never in
  `shared_preferences`.
- Attach the bearer token via a `dio` interceptor; handle silent token refresh.
- **Realtime auth**: authenticate the Supabase channel with the user token and the anon key;
  the service-role key must never ship in a client bundle.
- Respect the backend's CORS policy for the Flutter web origin.
- Never log or commit tokens, secrets, or PII.

## 12. Logging & Observability

- Use a structured logger (e.g. `logger`) with a consistent format; no `print` in app code.
- Propagate the backend's **correlation id** (returned in REST responses) into logs and
  Realtime messages for end-to-end tracing.
- **Redact** tokens, PII, and secrets from logs; never log request/response bodies that may
  contain sensitive data.
- Report uncaught errors to an error-reporting provider (e.g. Sentry / Firebase Crashlytics)
  with a stable release/version tag.

## 13. Testing

- **Unit**: business logic and Riverpod notifiers using `ProviderContainer` (mock with
  `mocktail`).
- **Widget**: screens/components with `WidgetTester`.
- **Golden**: sparingly, for stable design-system components.
- **Integration**: `integration_test` for critical end-to-end flows (incl. Realtime
  subscription/publish and reconnect).
- **Realtime**: fake the channel/WebSocket boundary in unit tests; cover reconnect and
  duplicate-message idempotency.
- Naming: `should_<behavior>_when_<condition>` (matches backend). Arrange–Act–Assert;
  deterministic, no shared mutable state between tests.

## 14. JSON & Typed Models

The client receives JSON from exactly two places — REST (`dio`, §7.1) and Supabase Realtime
(§7.2) — and treats it the way the backend does
(`docs/CODING_GUIDELINES_BACKEND.md` §13): **every payload becomes a typed, immutable model
before any other code sees it.** A raw map is not a model — `json['tenantId']` is unchecked,
survives renames silently, and turns a typo or a server-side change into a runtime
`TypeError` in a widget.

### Rules

- **One model per payload and per message.** Models are **`freezed`** data classes (immutable,
  `copyWith`, value equality) with **`json_serializable`** generated `fromJson`/`toJson` — not
  hand-written factories that pick keys with `[]` and cast with `as`.
- **`Map<String, dynamic>` is allowed in exactly two places**: (a) the signature of generated
  `fromJson`/`toJson` (and inside `*.g.dart`), and (b) a *single, immediately-consumed* value
  at the `data` boundary where `dio` has already decoded a body
  (`final res = await dio.post<Map<String, dynamic>>(...); return Tenant.fromJson(res.data!);`).
  It must never be a function's return type, a provider/state field, a widget parameter, or
  anything that outlives that one conversion.
- **No `dynamic` and no `jsonDecode` in app code.** Decoding belongs in a `fromJson` helper or
  in a decoder at the repository boundary — never in a notifier, screen, or widget. Prefer
  having `dio` hand back the model directly (a custom `ResponseTransformer`); either way the
  map must not escape the conversion.
- **Request bodies are models too.** Build a typed request object and call `.toJson()`
  (`data: CreateTenantRequest(name: name, slug: slug).toJson()`) so the field names live in
  the model instead of in every call site.
- **Match the wire naming in one place**: set `fieldRename` (e.g. `FieldRename.snake`) in
  `build.yaml` or annotate the field (`@JsonKey(name: ...)`) rather than renaming keys by hand
  — the client mirror of the backend's `@JsonProperty`/naming strategy.
- **Be liberal in what you accept.** Extra JSON fields are ignored by default; give new fields
  a default or make them nullable so an old client survives a server-first deploy; map a
  missing required field to a typed failure (§9) instead of letting a `TypeError` escape.
- **Realtime payloads are typed too**: decode the versioned envelope (event id, timestamp,
  aggregate id, version, payload) into a `freezed` model, switch on it exhaustively, ignore
  unknown versions, and never index a `Map<String, dynamic>` inside the subscription callback
  (§7.2).
- **Keep the map in `data`.** DTO ↔ domain mapping stays in the `data` layer; `application` and
  `presentation` only ever see models.

### Example — wrong

```dart
// Anti-pattern: an inline map body plus a hand-written fromJson over an untyped map.
final res = await dio.post<Map<String, dynamic>>('/api/v1/tenants',
    data: {'name': name, 'slug': slug});
return Tenant.fromJson(res.data!);

class Tenant {
  factory Tenant.fromJson(Map<String, dynamic> json) => Tenant(
        id: json['id'] as String, // unchecked: a rename becomes a runtime TypeError
        name: json['name'] as String,
      );
}
```

### Example — right

```dart
@freezed
class Tenant with _$Tenant {
  const factory Tenant({
    required String id,
    required String name,
    required String slug,
    required DateTime createdAt,
    required DateTime updatedAt,
  }) = _Tenant;

  factory Tenant.fromJson(Map<String, dynamic> json) => _$TenantFromJson(json);
}

@freezed
class CreateTenantRequest with _$CreateTenantRequest {
  const factory CreateTenantRequest({required String name, required String slug}) =
      _CreateTenantRequest;

  factory CreateTenantRequest.fromJson(Map<String, dynamic> json) =>
      _$CreateTenantRequestFromJson(json);
}

// data layer: typed in, typed out. The only map is dio's decoded body, converted in
// the same statement and never returned.
Future<Tenant> createTenant(CreateTenantRequest request) async {
  final res = await dio.post<Map<String, dynamic>>('/api/v1/tenants',
      data: request.toJson());
  return Tenant.fromJson(res.data!);
}
```

## 15. Code Review Checklist

Before merging, confirm:

- [ ] Immutable `freezed` models; `const` where possible; no shared mutable state
- [ ] Constructors/functions with >7 parameters use named parameters; large models decomposed
- [ ] Riverpod `Notifier`/`AsyncNotifier` used; no global mutable singletons
- [ ] Typed models for JSON; no `dynamic` maps in UI; no avoidable `!` assertions
- [ ] Every payload is a `freezed` + `json_serializable` model — no raw `Map<String, dynamic>`
      outside a generated `fromJson` signature or a single `data`-boundary conversion (§14)
- [ ] `dio`/Realtime access isolated in `data`; UI never talks to clients directly
- [ ] Realtime uses the anon key only; no service-role key or secrets in any client bundle
- [ ] Config injected via `--dart-define`/flavors; no hard-coded environment URLs
- [ ] Errors mapped to typed failures; no raw exceptions/stack traces in the UI
- [ ] Logs redact tokens/PII; correlation id propagated
- [ ] Tokens in secure storage; no secrets/PII logged or committed
- [ ] Tests added and passing; `flutter analyze` and `dart format` clean
- [ ] No deprecated SDK/Flutter/package APIs; `flutter analyze` free of deprecation warnings
- [ ] Feature-first structure and go_router conventions followed

## 16. Deployment

- **Web**: build with `flutter build web` and deploy to **Firebase Hosting**
  (`firebase deploy --only hosting`) — a global CDN with TLS and atomic, previewable
  rollouts.
- **iOS / Android**: distributed through the App Store / Play Store.
- Firebase Hosting config is **public** (no secrets); it holds only backend/Supabase URLs and
  public keys. Never embed the Supabase service-role key or other secrets in the web bundle.

## References

- Architecture: `docs/ARCHITECTURE.md`
- Backend guidelines: `docs/CODING_GUIDELINES_BACKEND.md`
- Flutter & Dart documentation: <https://flutter.dev>, <https://dart.dev>
- Riverpod: <https://riverpod.dev>
- freezed: <https://pub.dev/packages/freezed>
- go_router: <https://pub.dev/packages/go_router>
- flutter_appauth: <https://pub.dev/packages/flutter_appauth>
- Supabase Realtime (client): <https://supabase.com/docs/guides/realtime>


# Phase 8 — Delivery

**Scope** — Record the change and hand over the remaining work.

**Artifacts**
- `docs/CODING_GUIDELINES_BACKEND.md` §13 / `docs/CODING_GUIDELINES_FRONTEND.md` §14 — the rule
  itself (written in the preceding change).
- `CHANGELOG.md` — `Unreleased → Changed`: the guideline entry plus what was made typed
  (adapters, `Claims`, `RealtimeEnvelope<T>`, Flutter request models).
- `docs/delivery/typed-json-payloads/` — this plan and its phase records.

**Dependencies** — Phases 3–7.

**Verification** — `git diff --stat` shows only intended files; `mvn -o test` green.

## Deployment notes

- No database schema, Liquibase changelog, config key, or REST contract changed. The only wire
  shapes touched are the ones Keystone *consumes* from Supabase (GoTrue) and *sends* to Supabase
  Realtime, and the new records reproduce the previous JSON exactly (asserted by tests).
- `Platform` / `keystone-admin` library consumers (currently `apps/inventory`) need no change:
  `Principal.subject()` and the REST DTOs are untouched.
- **Flutter client:** `platform/keystone-admin-ui/pubspec.yaml` gains
  `freezed_annotation`/`json_annotation` (dependencies), `freezed`/`json_serializable`/`build_runner`
  (dev-dependencies) and `environment.sdk: '>=3.8.0 <4.0.0'` (required by `json_serializable`
  6.14.1; the toolchain is Dart 3.13.4). Generated `*.freezed.dart`/`*.g.dart` are committed, so
  `pub get` alone is still enough to build; run `dart run build_runner build
  --delete-conflicting-outputs` after changing a model.
- Both Flutter packages — `platform/keystone-admin-ui` and `apps/inventory/frontend` — declare
  `environment.sdk: '>=3.8.0 <4.0.0'`, so the client code has a single language version and a single
  `dart format` style, and `json_serializable` runs warning-free (frontend guidelines §1).

## Follow-ups

1. ~~`dart format` drift in `platform/keystone-admin-ui`~~ — **done on request.** The 10 pre-existing
   unformatted files were normalised in one mechanical pass
   (`cd platform/keystone-admin-ui && dart format lib test` → 10 changed, re-check 0 changed), with
   `flutter analyze` clean and `flutter test` 7/7 afterwards. The drift predated this change
   (unformatted at `HEAD`) and the set grew once the SDK constraint reached 3.8 and Dart switched to
   the new "tall" formatter for this package. `apps/inventory/frontend` was already clean.
2. **`AdminConfigLoader` / `ConfigLoader`** — currently conforming via the config-loader exception;
   they could bind the YAML tree to a record (`treeToValue`) if the precedence/fallback logic is ever
   simplified.
3. **`ProblemDetail.errors`** — `Map<String, String>` of dynamic field names (RFC 9457 `errors`
   object), intentionally left as a keyed map.

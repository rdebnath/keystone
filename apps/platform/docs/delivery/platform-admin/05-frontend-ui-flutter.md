# Phase 5 — Frontend (Flutter)

**Scope** — Flutter app under `apps/platform/frontend`.
**Artifacts** — `pubspec.yaml`, Riverpod providers, freezed models, dio REST client,
go_router routes, screens (login, change-password, dashboard with tenants/roles/permissions/users).
**Dependencies** — Phase 4 (contract).
**Verification** — `flutter analyze` + `dart format`; widget/unit tests for core flows.

## Decisions

- Use `supabase_flutter` for Supabase Auth (PKCE) + password update + Realtime client; `dio` for
  backend REST; Riverpod + freezed + go_router per the frontend guidelines.
- `flutter_appauth` is listed in the guidelines, but `supabase_flutter` is the official Supabase
  client and is the pragmatic choice for Supabase Auth + `updateUser` password change.

## Result

- Flutter app created under `apps/platform/frontend`; `dart analyze lib` → no issues.
- Auth via `supabase_flutter` (PKCE + `updateUser`), REST via `dio`, state via Riverpod, routing
  via `go_router` with an auth gate (login → change-password → dashboard).
- Config injected via `--dart-define` (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `API_BASE_URL`).

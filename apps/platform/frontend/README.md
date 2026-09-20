# Platform Admin Console (frontend)

Flutter client for the Keystone platform admin console (web, iOS, Android).

## Stack

- **State** — Riverpod (`FutureProvider`/`StreamProvider`, no codegen).
- **Models** — immutable plain classes with `fromJson` (no raw maps in UI).
- **REST** — `dio` → the platform backend (`API_BASE_URL`).
- **Auth / Realtime** — `supabase_flutter` (Supabase Auth PKCE + `updateUser` password change).
- **Routing** — `go_router` with an auth gate (login → change-password → dashboard).

## Run

Build-time configuration is injected via `--dart-define`:

```bash
flutter run \
  --dart-define=SUPABASE_URL=https://<ref>.supabase.co \
  --dart-define=SUPABASE_ANON_KEY=<anon-key> \
  --dart-define=API_BASE_URL=http://localhost:8080
```

The anon key and backend URL are public (they ship in the client bundle). The Supabase
service-role key must never appear here.

## Analyze

```bash
dart analyze lib
```

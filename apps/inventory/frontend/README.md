# inventory — frontend

Flutter frontend for the Inventory application (web, iOS, Android). It **hosts** the Keystone
platform admin UI (`platform/keystone-admin-ui`) and adds the inventory UI, routing between them
by the authenticated user:

- common login (`username@tenantid`, backend-proxied),
- platform user → platform admin dashboard (from the shared package),
- tenant user → inventory UI.

See `docs/CODING_GUIDELINES_FRONTEND.md`.

## Run

Build-time configuration is injected via `--dart-define`:

```bash
flutter run --dart-define=API_BASE_URL=http://localhost:8080/inventory
```

The backend URL is public (it ships in the client bundle); no service-role key ever appears here.

## Analyze

```bash
dart analyze lib
```

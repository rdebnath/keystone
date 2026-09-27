# 04 — Server-side API & realtime

## Scope / Artifacts / Dependencies / Verification

- **Scope** — expose the reset route and adapt the existing change-password route to the new request
  body. **No realtime**: nothing in this feature broadcasts, and no Supabase Realtime channel changes.
- **Artifacts** — `user/UserHandler` (new `PUT /api/v1/users/{id}/password`), `MeHandler`
  (`POST /api/v1/me/password` passes the richer body through).
- **Dependencies** — phase 3 (`UserService.resetPassword`, `MeService.changePassword`).
- **Verification** — `AdminIntegrationTest` route assertions (phase 7) plus a manual `curl` walk-through
  in phase 8.

## Routes

### New — `PUT /api/v1/users/{id}/password`

```java
routes.put("/api/v1/users/{id}/password", ctx -> {
    guard.requireWrite(ctx, PLATFORM_USER);
    UUID id = Ids.uuid(ctx.pathParam("id"));
    ResetPasswordRequest request = ctx.bodyAsClass(ResetPasswordRequest.class);
    service.resetPassword(id, request, guard.principal(ctx).subject());
    ctx.status(204);
});
```

- `PUT` (not `POST`) mirrors the existing `PUT /api/v1/users/{id}/roles`, which also *replaces* a
  sub-resource of the user; both answer `204`.
- The path is a noun sub-resource (`…/password`), not a verb (`…/reset-password`), per
  `docs/CODING_GUIDELINES_BACKEND.md` §8.
- Authorization is the existing write level on the user resource — no new permission code, no catalog
  change, no bootstrap change.
- The caller's subject comes from the authenticated principal (`PermissionGuard.principal`), never from
  the body: the self-reset guard must not be client-controlled.
- Status mapping is the shipped one via `ProblemDetailMapper`: `403` denied, `404` unknown user,
  `422` rejected value (blank, self-target), `204` success. No `400` is produced by application code.

### Changed — `POST /api/v1/me/password`

```java
routes.post("/api/v1/me/password", ctx -> {
    Principal principal = guard.principal(ctx);
    var request = ctx.bodyAsClass(ChangePasswordRequest.class);
    meService.changePassword(principal.subject(), request);
    ctx.status(204);
});
```

Same route, same `204`, richer body: the forced first-login call (`{"password": …}`) is byte-compatible
with what the shipped client sends today, and the voluntary call adds `currentPassword`.

## Contract summary

| Route | Body | Success | Failures |
| --- | --- | --- | --- |
| `POST /api/v1/me/password` | `{password}` (forced) / `{currentPassword, password}` (voluntary) | `204` | `403` unauthenticated, `404` unknown sub, `422` blank field / wrong current password |
| `PUT /api/v1/users/{id}/password` | `{temporaryPassword}` | `204` | `403` without `platform:user:read-write`, `404` unknown user, `422` blank / self-target |

Both routes return no content, so no response DTO is added and no password can be echoed back.

## Result — executed (2026-09-27)

- `PUT /api/v1/users/{id}/password` added to `UserHandler`: `guard.requireWrite(ctx, PLATFORM_USER)`,
  `ResetPasswordRequest` from the body, and the caller taken from `guard.principal(ctx).subject()` (never
  from the body), answering `204`.
- `POST /api/v1/me/password` unchanged in shape — it now passes the richer `ChangePasswordRequest`
  through to `MeService`.
- No realtime involvement: nothing in this feature broadcasts.
- Verified end-to-end against the running dev server (phase 8, 24/24 checks): `204` for a reset of
  another user, `403` without a token / without the grant, `404` unknown user, `422` blank password and
  self-target, and the forced/voluntary change-password status codes (`204`/`422`).

# 03 — Domain & application services

## Scope / Artifacts / Dependencies / Verification

- **Scope** — the two service-level rules: prove the current password before a voluntary change, and
  reset another user's password without storing it.
- **Artifacts** — `identity/ChangePasswordRequest`, `identity/MeService`, `user/ResetPasswordRequest`
  (new), `user/UserService`.
- **Dependencies** — `SupabaseAdminClient` (existing port: `login`, `updatePassword`), `DataAccess`.
- **Verification** — unit + integration tests (phase 7): `mvn -pl platform/keystone-admin test`.

## `identity/ChangePasswordRequest`

```java
/** Change-own-password request. {@code currentPassword} is required unless the caller is in the forced
 *  first-login state ({@code users.must_change_password}), where the password was just used to sign in. */
public record ChangePasswordRequest(String currentPassword, String password) {
}
```

Adding a component is backward compatible on the wire: the shipped first-login screen sends
`{"password": "…"}` and Jackson leaves `currentPassword` null.

## `identity/MeService.changePassword(String sub, ChangePasswordRequest request)`

Order of operations (all before any GoTrue write):

1. Load the caller's row (`sub`) → `NotFoundException` when absent (today's `markPasswordChanged`
   already answers 404 for an unknown sub; the lookup moves up so the rule can read the flag).
2. `requireNewPassword(request.password())` — blank → `ValidationException("password must not be blank")`.
3. When `!mustChangePassword` → `verifyCurrentPassword(user, request.currentPassword())`:
   - blank → `ValidationException("currentPassword is required to change your password")`;
   - `supabaseAdmin.login(user.getEmail(), currentPassword)` in a `try`/`catch (AccessDeniedException)`
     → `ValidationException("Current password is incorrect.")`; the returned `Session` is discarded
     (this is a *verification*, not a login — nothing is stored).
4. `supabaseAdmin.updatePassword(sub, request.password())`.
5. `markPasswordChanged(sub)` — the existing `must_change_password = false` write, unchanged.

Rationale for 3: the endpoint is authenticated by a bearer token, and a stolen token must not be enough
to take an account over. A GoTrue password grant is the only honest proof of "the caller knows the
current password", and the backend already holds the service-role key.

When `mustChangePassword` is `true` step 3 is skipped: the caller authenticated with that exact password
moments ago, and the forced flow must not ask for it twice (the shipped screen sends `{password}` only).

## `user/ResetPasswordRequest` (new)

```java
/** Reset-another-user's-password request ({@code PUT /api/v1/users/{id}/password}): the temporary
 *  password is handed to Supabase Auth and forces a change on the target's next login. */
public record ResetPasswordRequest(String temporaryPassword) {
}
```

## `user/UserService.resetPassword(UUID id, ResetPasswordRequest request, String actorSub)`

1. Load the target row (`users.id = id`) → `NotFoundException("User not found: " + id)`.
2. `requireTemporaryPassword(request.temporaryPassword())` — blank → `ValidationException`
   (the same rule `create` applies to `temporaryPassword`; extract the shared private helper).
3. `requireNotSelf(target.getSub(), actorSub)` → `ValidationException("Use the change-password flow for
   your own account")`. Without this, anyone holding `platform:user:read-write` (which a platform admin
   does) could set their own password without proving the current one, making decision 1 optional.
4. `supabaseAdmin.updatePassword(target.getSub(), request.temporaryPassword())` — GoTrue first, so a
   GoTrue failure cannot leave the row flagged as "temporary" for a password that never changed.
5. `update users set must_change_password = true, updated_at = now() where id = id` (single write,
   no transaction needed around the GoTrue call; the flag write is idempotent).
6. Returns `void` (the route answers `204`); no DTO, so no path can echo the password back.

The temporary password is **never** selected, logged or stored: only the GoTrue admin call carries it.

## Notes

- `Username`/`Emails` are untouched; the reset never renames or re-provisions a user, and the Auth
  identity (`users.sub`/`users.email`) is unchanged.
- No new port method is needed: `SupabaseAdminClient.login` (verification) and `updatePassword` both
  exist, so the adapter contract is unchanged in this phase.
- No schema change and no new permission code (see `plan.md` → phase 2 skipped).

## Result — executed (2026-09-27)

- `identity/ChangePasswordRequest` gained `currentPassword` (nullable: the forced flow still sends
  `{password}` only).
- `MeService.changePassword(String sub, ChangePasswordRequest request)` now loads the caller's row first
  (`404` if absent), rejects a blank new password, and — when `must_change_password` is `false` — proves
  the current one with `requireCurrentPassword(email, currentPassword)` (a GoTrue password grant for the
  caller's own identity; a rejected grant becomes `422 Current password is incorrect.`). The GoTrue
  update + `markPasswordChanged` still run only after the proof.
- New `user/ResetPasswordRequest`; `UserService.resetPassword(UUID id, ResetPasswordRequest request,
  String actorSub)` loads the target (`404`), validates the temporary password, refuses the caller's own
  account (`422 Use the change-password flow for your own account`), writes to Supabase Auth first and
  then sets `must_change_password = true` + `updated_at`, logging
  `INFO Password reset for user {} by actor {}`.
- `UserService.create` and the reset now share `validateTemporaryPassword(String)` (the old
  `validatePassword(UserRequest)` is gone); `UserService` gained the SLF4J class logger it needed for
  the audit line.
- Verified: `mvn -q -pl platform/keystone-admin -am -DskipTests compile` clean; the behaviour is covered
  by `AdminIntegrationTest` (phase 7) and the live walk-through (phase 8).

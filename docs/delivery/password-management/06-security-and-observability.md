# 06 — Security & observability

## Scope / Artifacts / Dependencies / Verification

- **Scope** — make a GoTrue failure visible to an operator without logging secrets, and state the
  security rules the new password paths must satisfy.
- **Artifacts** — `supabase/GoTrueError` (new wire record), `supabase/SupabaseHttpAdminClient`
  (logging), plus the guards already specified in phase 3 (current-password verification, self-reset
  rejection) and an audit log line for the reset.
- **Dependencies** — phase 3 (guards) and phase 4 (routes that expose them).
- **Verification** — unit test for the failure-code parsing (phase 7), code review against the
  checklist below, and a live 403 + server-log check during the phase-8 walk-through.

## R1 — the login failure stops hiding

Today `SupabaseHttpAdminClient.login` maps **every** non-2xx to the same uniform 403 and logs nothing,
so a wrong password, a revoked service key, a rate limit and a Supabase outage are indistinguishable —
for the client *and* for whoever reads the server log. On 2026-09-27 that cost an hour of diagnosis.

```java
/** GoTrue's error body (`{"code":400,"error_code":"invalid_credentials","msg":"…"}`). */
@JsonIgnoreProperties(ignoreUnknown = true)
record GoTrueError(Integer code, @JsonProperty("error_code") String errorCode, String msg) {
}
```

```java
if (response.statusCode() >= 300) {
    log.warn("Supabase password grant failed (HTTP {}, {}) — answering as invalid credentials",
            response.statusCode(), failureCode(response.body()));
    throw new AccessDeniedException("Invalid username, tenant, or password.");
}
```

- **Level `WARN`** — an expected business failure (a wrong password) is WARN per
  `docs/CODING_GUIDELINES_BACKEND.md` §9/§10; an operational failure now shares that line instead of
  being invisible.
- **Only the status and the machine-readable code are logged** (`error_code`, else `code`): `msg` is
  deliberately excluded because GoTrue can echo the submitted identifier, and the request body carries
  the password. No email, no token, no key, no body (`§10 Never log: passwords, tokens, keys, PII`).
- **`failureCode(String body)` never throws**: an empty, HTML or truncated body reads as `unparsable`
  (wrapped `IOException | RuntimeException`), so the failure mode of the endpoint cannot change.
- The client-visible contract is unchanged: `403 {title: ACCESS_DENIED, detail: "Invalid username,
  tenant, or password."}` — anti-enumeration is preserved, and the uniform message is still what the
  login screen shows.
- No metric is added: the WARN line is the signal, and the login route already has latency/error
  visibility at the HTTP layer.

### Operator runbook (documented in the phase result)

| Log line | Meaning | Action |
| --- | --- | --- |
| `HTTP 400, invalid_credentials` | the password really is wrong | user problem; reset via the console (`PUT /users/{id}/password`) |
| `HTTP 400, validation_failed` / `email_not_confirmed` | Auth-side account state | fix the GoTrue identity |
| `HTTP 401/403` | service-role key rejected | check `SUPABASE_SERVICE_ROLE_KEY` |
| `HTTP 429` | GoTrue rate limit | back off / raise the plan limit |
| `HTTP 5xx` | Supabase incident | retry later |
| `unparsable` | unexpected body | inspect with `X-Correlation-Id` from the response |

## Security rules the new paths must hold

| Concern | Rule | Where |
| --- | --- | --- |
| **Token theft** | a voluntary change must prove the *current* password with a GoTrue password grant; a stolen bearer token alone cannot take the account over | `MeService.changePassword` (phase 3) |
| **Privilege self-service** | `PUT /users/{id}/password` refuses the caller's own account, so the proof above cannot be bypassed by a `platform:user:read-write` holder | `UserService.resetPassword` (phase 3) |
| **Delegation scope** | the reset needs a write on `platform:user`, i.e. the same grant that already allows renames, role changes and deletion — no new privilege is introduced | `UserHandler` (phase 4) |
| **No new secrets** | the temporary password travels UI → API → GoTrue only; it is never stored in the platform schema, never returned (`204`), never logged | phases 3–5 |
| **No PII in logs** | the new reset audit line logs the target `users.id` (a random uuid) and the actor `sub` — no email, no username, no password | phase 3 |
| **Audit trail** | `INFO` on a successful reset (`Password reset for user {} by actor {}`) — a privileged, otherwise silent, account-takeover-shaped action should be traceable | phase 3 |
| **Client is not a boundary** | the reveal toggle and the dialog gates are UX only; every rule above is enforced server-side | phases 3–5 |
| **Enumeration** | unchanged: login failures stay uniform; the reset route leaks nothing beyond "no such user id" for a caller who already holds the user-write grant | phase 4 |

## Result — executed (2026-09-27)

- **New wire record** `supabase/GoTrueError(Integer code, @JsonProperty("error_code") String errorCode,
  String msg)` with `describe()` → `error_code`, else `code`, else `unknown`; unknown fields ignored.
- **`SupabaseHttpAdminClient.login`** logs exactly one `WARN` on a non-2xx and keeps the uniform
  `AccessDeniedException`; `failureCode(String body)` never throws (a non-JSON body reads as
  `unparsable`, so the endpoint's failure mode cannot change). The class gained the SLF4J logger it was
  missing.
- **Live evidence** (dev server, wrong password for `admin@keystone`, captured from the process stdout):

  ```
  WARN com.chetana.keystone.platform.admin.supabase.SupabaseHttpAdminClient --
      Supabase password grant failed (HTTP 400, invalid_credentials) — answering as invalid credentials
  ```

  and the reset audit line:

  ```
  INFO com.chetana.keystone.platform.admin.user.UserService --
      Password reset for user 5e9d926d-…-3fecabacf386 by actor 41b6044b-…-b98a9ee02bb3
  ```

  Neither line carries an email, username, password, token or key; the client response stayed
  `403 {title: ACCESS_DENIED, detail: "Invalid username, tenant, or password."}`.
- **Follow-up spotted during the live run (pre-existing, not introduced here):** with no `logback.xml`
  the root logger is DEBUG, so jOOQ's `LoggerListener` prints every executed statement *with its bound
  parameters* — which is why `admin@keystone.com` appears in the dev console. Production should pin a
  log level/config; none of the new lines contribute to it. **Resolved later:** the service now ships its
  own `apps/inventory/server/src/main/resources/logback.xml`, with jOOQ's listener pinned at `INFO`.

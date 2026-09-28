# Phase 6 — Security & observability

**Scope** — review what this change does to authorization, validation and observability, and make the console
honest about the one gate it newly depends on.

**Artifacts** — no backend artifact. In the console: the *Choose permissions* affordance is gated on the
caller's read access to the plane's **permission** resource, and a row the caller may not grant is disabled
(phase 3's `Me.canGrant`, phase 5's row).

**Dependencies** — phases 3 and 5. Nothing here changes the server.

**Verification** — the review table below; the gating and disabled-row behaviour are asserted by
`permission_picker_test` (phase 7); no new metric, log line or realtime message is introduced, which is
itself the check that observability is unchanged.

## What is unchanged (and why that is the point)

| Concern | State |
| --- | --- |
| **Create a role** | Still `guard.requireWrite(ctx, PLATFORM_ROLE / TENANT_ROLE)`. The picker cannot create anything; it only chooses codes for a request the guard already protects. |
| **Escalation guardrail** | Still `CallerScope.requireGrantable` — a tenant caller cannot grant the wildcard or a code it does not hold (write implying read). `Me.canGrant` **mirrors** it to disable rows; it never replaces it, and the picker cannot widen a grant set. |
| **Grantability of a code** | Still decided by `RoleService.grantPermissions` (owner + scope). The picker's pinned query mirrors it, so the UI cannot offer more than the service accepts. |
| **Visibility / tenancy** | The picker reads the same two list routes as the Permissions screen: the platform route filters by `tenantId`, the tenant route derives the tenant from the caller and **refuses** a `tenantId` parameter. A tenant console sends none (`ConsoleScope.permissions`). |
| **Secrets** | Nothing new: the bearer token still comes from the shared `dio` interceptor; no service-role key, no new header, nothing logged. |
| **Customer data** | The picker displays catalogue rows (codes, scopes, owners) that the Permissions screen already displays to the same caller. No new field is exposed. |
| **Realtime / metrics / tracing** | Untouched: no channel, no broadcast, no new metric or trace attribute. Query volume is bounded by the existing 300 ms debounce and by `permissionsPageProvider` being an `autoDispose` family keyed by the whole query, so a dialog's queries are released when it closes. |

## The one new dependency: reading the catalogue

Entering the **Roles** screen is gated by the *role* resource, but feeding the picker reads the *permission*
resource (`guard.requireRead(…, PLATFORM_PERMISSION / TENANT_PERMISSION)`). So a caller can hold
`platform:role:read-write` and still be refused the catalogue. That is the backend being correct — this change
must not appear to work around it:

1. **The affordance is gated first.** *Choose permissions* is enabled only when
   `me.allowsResource(console.permissionResource)` — the console's existing client mirror
   (`docs/ARCHITECTURE.md` §9.6: **UX, not security**). When it is not held, the field says
   `You need platform:permission:read-only (or read/write) to choose permissions.` and no request is made.
   A user in that position can still create a role with no permissions — the request is valid, and the
   backend remains the only authority on whether it is allowed.
2. **If the read is refused anyway** (a grant removed mid-session, a stale token), the picker shows the
   shared error panel with `apiErrorMessage`'s server detail and a **Retry** — the same handling every other
   list has, so the failure is visible and recoverable rather than a spinner that never ends.
3. **Nothing is cached across dialogs beyond the provider's lifetime**, so a permission revoked on the server
   is not "remembered" as grantable by the console.

## Validation

Unchanged and sufficient: the request is validated by the service (code non-blank, owner/scope rules, grant
visibility) and by `PermissionRequest`/`RoleRequest` deserialization. The picker adds **client-side prediction**
of two of those rules — never a relaxation of them — which is the whole reason A5 and A8 exist as acceptance
criteria.

## Operator notes

- No new configuration, environment variable, secret, index or deployment step.
- A refusal the picker predicted wrongly surfaces as the server's own `422`/`403` detail through the existing
  `showApiError`, so nothing becomes silently unsupported.

## Execution record (2026-09-28)

**Implemented exactly as reviewed, with no backend change:**

| Concern | What the code does |
| --- | --- |
| The picker affordance reads the *permission* resource | `roles_screen.dart` computes `canReadCatalogue = me.allowsResource(console.permissionResource)` and feeds it to `_permissionsField`: the *Choose permissions* button is `onPressed: canReadCatalogue && !_saving ? _choosePermissions : null`, and the field's helper text becomes `You need <resource>:read-only (or read/write) to choose permissions`. Nothing is requested when it is missing — asserted by `should_withhold_the_picker_without_the_read_grant` (which also asserts `adapter.catalogueRequests` is empty). |
| A refused read is visible and recoverable | The picker's catalogue is a `PagedListView`, so a `403` (a grant withdrawn mid-session) renders `MessagePanel` with `apiErrorMessage`'s RFC 9457 `detail` and a Retry that re-runs the same query — asserted by `should_show_the_servers_message_with_a_retry` (the message text is the fake backend's, and Retry makes a second request). |
| Escalation guardrail mirrored, never replaced | `Me.canGrant` (phase 3) decides only the row's `grantable`; the picker never filters a row out, so nothing the server would accept is hidden, and the backend still refuses what it should. `should_disable_a_permission_the_caller_may_not_grant` asserts the disabled row *and* that write-implies-read still lets a held grant through. |
| Nothing else moved | No route, DTO, guard, database object, metric, log line, trace attribute or realtime message changed; the request bodies are the same `RoleRequest`/`CreateRoleRequest` shape the screen already sent. |

**Verification** — the review table above is the check; the two behavioural claims are covered by the named
phase-7 tests, and `flutter analyze` is clean. No security-sensitive code was added client-side beyond the two
"UX, not security" mirrors already described, which the backend duplicates authoritatively.

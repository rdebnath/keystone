# Delivery Plan — Admin console navigation, tenants & users

**Feature slug:** `admin-console-navigation`
**Level:** Platform-level — touches `platform/keystone-admin` (backend), `platform/keystone-admin-ui`
(Flutter), the hosting app's router (`apps/inventory/frontend`) and `docs/`.

## Sizing decision

**Produce a plan.** The change replaces the admin console's tab shell with a permission-driven
navigation shell, adds tenant edit/delete and user edit to the UI, adds a tenant-scoped user listing
(a new query parameter), a new `PATCH /api/v1/users/{id}` route, surfaces the platform plane as a
tenant, and updates the host app's router (deep-linkable sub-routes). It touches the web/domain/API
layers, both frontend packages, and the documented tenancy model — not a single-file tweak.

## Summary

| # | Request | Delivered by |
| --- | --- | --- |
| 1 | Left pane with menu options, hideable/showable | new `AdminShell` (platform UI): collapsible side pane (inline on wide layouts, drawer overlay on narrow ones), selection driven by the router location |
| 2 | Menu options based on the user's permissions | `AdminSection` catalog + `Me.allowsResource(...)`; sections whose resource the caller cannot read are not rendered (the backend stays the only security boundary) |
| 3 | Platform admin: **Tenants** → list the users of that tenant | `TenantsScreen` → row tap → `TenantUsersScreen` (`GET /api/v1/users?tenantId=…`) |
| 4 | Platform users live in the tenant named **Keystone** | `GET /api/v1/tenants` returns the platform plane as a synthetic first row (`Keystone`, reserved id, `platform: true`, never persisted); `tenantId=<reserved id>` lists platform users (`tenant_id IS NULL`) |
| 5 | Add / edit / delete for tenant | `POST` exists; add UI for `PATCH /api/v1/tenants/{id}` and `DELETE /api/v1/tenants/{id}` (both permission- and `platform`-row-gated) |
| 6 | Edit a user → assign roles, etc. | new `PATCH /api/v1/users/{id}` (`UserUpdateRequest(username, roles)`) + a shared user editor dialog (username + role checklist scoped to the user's plane) |

## Phase list

1. Discovery & design — `01-discovery-and-design.md`
2. Database changes — **skipped.** No DDL: the platform plane is synthetic (no row), the permission
   catalog is seeded at bootstrap, and no new column is needed. Keeping the changelog DDL-only keeps
   the offline jOOQ codegen deterministic (`platform/keystone-admin/pom.xml`).
3. Domain & application services — `03-domain-and-application-services.md`
4. Server-side API & realtime — `04-server-side-api-and-realtime.md`
5. Frontend / UI (Flutter) — `05-frontend-ui-flutter.md`
6. Security & observability — `06-security-and-observability.md`
7. Testing — `07-testing.md`
8. Delivery — `08-delivery.md`

## Design in one page

### Navigation shell

```
┌───────────────────────────────────────────────────────────────┐
│ ☰  Tenants                                      (AppBar)      │
├──────────────────┬────────────────────────────────────────────┤
│ Keystone - Inv.  │  Tenants                                   │
│ admin            │  ┌──────────────────────────────────────┐  │
│                  │  │ 🛡 Keystone  platform · platform users│  │
│ ▸ Tenants        │  │ 🏢 Acme      acme · 4 users          │  │
│ ▸ Users          │  │ 🏢 Globex    globex · 0 users        │  │
│ ▸ Roles          │  └──────────────────────────────────────┘  │
│ ▸ Permissions    │                                    ⊕ Add   │
│ ─────────────    │                                            │
│ ⏻ Sign out       │                                            │
└──────────────────┴────────────────────────────────────────────┘
```

- Menu items are `platform:<resource>:read-only` gated; a caller holding only `:read-write` also
  sees them (read/write implies read), and `*` sees all.
- The pane is toggled by the AppBar hamburger: inline width animation on wide layouts
  (>= 800 px), `Scaffold.drawer` overlay on narrow ones.
- Selection is derived from the router location (`/tenants/<id>` selects **Tenants**), so every
  section is deep-linkable on web and browser back/forward work.

### Routes (host router, `apps/inventory/frontend/lib/router.dart`)

| Path | Screen | Permission rendered from (backend enforced) |
| --- | --- | --- |
| `/tenants` | `TenantsScreen` | `platform:tenant:read-only` |
| `/tenants/:tenantId` | `TenantUsersScreen` | `platform:user:read-only` |
| `/users` | `UsersScreen` (all users, tenant filter) | `platform:user:read-only` |
| `/roles` | `RolesScreen` (unchanged) | `platform:role:read-only` |
| `/permissions` | `PermissionsScreen` (unchanged) | `platform:permission:read-only` |

Paths are exposed as `AdminRoutes` constants by the platform UI package so the host router and the
shell cannot drift. Write affordances (Add tenant, Edit/Delete tenant, Add/Edit/Delete user) require
the matching `:read-write` code and are hidden otherwise.

### The `Keystone` platform tenant

| Aspect | Value |
| --- | --- |
| id | `00000000-0000-0000-0000-000000000000` (`PlatformSchema.PLATFORM_TENANT_ID`, reserved; generated tenant ids are random v4 UUIDs, so it cannot collide) |
| name / slug | `Keystone` / `keystone` (`PlatformSchema.PLATFORM_TENANT_NAME` + existing `RESERVED_SLUG`) |
| persisted? | **Never.** Returned by `GET /api/v1/tenants` as a synthetic row with `platform: true` and null timestamps |
| users | `users.tenant_id IS NULL`; listed by `GET /api/v1/users?tenantId=00000000-…-000000000000` |
| mutable? | No — `PATCH`/`DELETE /api/v1/tenants/{id}` reject the reserved id; the UI hides the actions; the login slug `keystone` already resolves to the platform plane (`TenantResolver`) |
| slug guard | creating or renaming a tenant to the reserved slug `keystone` is rejected (it would shadow the platform plane on login) |

### API deltas

| Route | Change |
| --- | --- |
| `GET /api/v1/tenants` | returns the synthetic `Keystone` row first; `TenantDto` gains `platform: boolean` |
| `PATCH /api/v1/tenants/{id}` | unchanged contract; rejects the reserved id (422) |
| `DELETE /api/v1/tenants/{id}` | unchanged contract; rejects the reserved id (422); still 409 when the tenant has users |
| `GET /api/v1/users?tenantId=<uuid>` | **new** optional filter: absent = all users, reserved id = platform users (`tenant_id IS NULL`), otherwise that tenant's users |
| `POST /api/v1/users` | the reserved tenant id is accepted as an alias for the platform plane (`null`) |
| `PATCH /api/v1/users/{id}` | **new** — `UserUpdateRequest(username, roles)`: renames the user and replaces its role set atomically; `email` is **not** editable (it is the virtual Supabase/GoTrue identity bound to `users.sub`) |
| `GET /api/v1/me` | `MeDto` gains `username` (additive) so the shell header can name the signed-in user |

No Realtime publication is added: the admin console refreshes through Riverpod invalidation, and the
admin API publishes nothing today (`keystone-realtime` is not used by `keystone-admin`).

## Resolved decisions (proposed — confirm or change)

1. **Sidenav, not tabs.** The tab bar is replaced by the collapsible left pane; `DashboardScreen` is
   deleted and `AdminShell` takes its place (unreleased platform — no back-compat burden).
2. **Router-driven sections** (`ShellRoute` at `/`) rather than shell-internal state: deep links,
   browser back/forward, and testable per-section screens. The host router gains the sub-routes.
3. **The platform plane is listed as the `Keystone` tenant** so the console needs no client-side
   sentinel constants: the row's id comes from the API and is passed back verbatim.
4. **`PATCH /api/v1/users/{id}` carries name + roles atomically** (one dialog → one call).
   `PUT /api/v1/users/{id}/roles` stays as it is (unused by the console, not removed).
5. **`email` is read-only in the user editor** — changing it would desynchronise the row from the
   GoTrue user the login flow authenticates with (`SupabaseAdminClient` has no email-update call;
   adding one is a separate, auth-touching change).
6. **The all-users section is kept** with a tenant filter dropdown (All / Keystone / each tenant),
   sharing the row widget and editor with the tenant drill-down. Say the word and it goes, leaving a
   tenants-only console.
7. **User delete is exposed in the UI** (`DELETE /api/v1/users/{id}` already exists) alongside edit,
   with confirmation. The roles/permissions screens keep their current create + list scope.
8. **No database change and no new configuration** — no Liquibase changeset, no jOOQ codegen churn,
   no env var, no deployment topology change.

## Open questions

1. Confirm decisions 1–2 (sidenav + `ShellRoute` sub-routes), or prefer the shell to hold the section
   in local state (no host-router change, but no deep links)?
2. Confirm decisions 5–7 (email read-only, all-users section kept, user delete exposed).
3. Should the console show a tenant-plane menu (tenant-scoped users/roles) when the caller is a
   tenant admin? Proposed: **out of scope** — this feature is the platform-plane console.
4. Should Roles gain edit/delete in the UI while we are here? Proposed: **out of scope**.

## Confirmation state

- [x] Plan reviewed — approved as written (2026-09-27): sidenav + `ShellRoute`, all-users section kept,
      `email` read-only, user delete exposed; phases 3–8 delivered one at a time.
- [x] **Phases 3 & 4 executed** (2026-09-27) — see the execution records in those phase files.
- [x] **Phase 5 executed** (2026-09-27) — see `05-frontend-ui-flutter.md`.
- [x] **Phase 6 executed** (2026-09-27) — security/observability review, no code changes needed.
- [x] **Phase 7 executed** (2026-09-27) — backend 51 tests (0 failures, 0 skipped with Docker; frontend
      36 tests); `mvn clean verify` green repo-wide.
- [x] **Phase 8 executed** (2026-09-27) — CHANGELOG + ARCHITECTURE updated; README verified unchanged.
- [ ] **Follow-ups (not blockers):**
  - ~~`AdminIntegrationTest` cannot run (no Docker daemon)~~ — **done** (2026-09-27): Docker Desktop was
    started, `AdminIntegrationTest` passes, and `mvn clean verify` is green with 0 skips. That run
    corrected one expectation: a rejected value answers **422** (`ValidationException` →
    `ProblemDetailMapper`), not 400 — the test and the docs now say 422.
  - No manual click-through against a live backend (needs dev credentials): the console was verified by
    analyze, 36 widget/unit tests and review only.
  - Spotted while reading `docs/ARCHITECTURE.md` §9.4 (pre-existing, untouched): it documents
    `user_roles` with `PRIMARY KEY (user_id, role_id, tenant_id)`, while
    `0001-initial-schema.xml` adds `PRIMARY KEY (user_id, role_id)`. Worth reconciling in a docs-only
    change.
  - **Resolved (2026-09-27):** the guidelines-vs-code conflict over validation status was settled in
    favour of the shipped code — `docs/CODING_GUIDELINES_BACKEND.md` §8/§9/§13/§14 and
    `docs/CODING_GUIDELINES_FRONTEND.md` §9 now state validation → `422` (with `400` reserved for an
    unparseable request), matching `ProblemDetailMapper`. No code changed. See
    `docs/delivery/permission-access-levels/01-discovery-and-design.md` → "Correction (2026-09-27)".

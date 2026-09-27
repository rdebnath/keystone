# Phase 1 — Discovery & design

**Scope** — fix the console's navigation model, the permission-driven menu rule, the tenant→users
flow, the platform-plane ("Keystone") representation and the API deltas. No code changes in this
phase.

**Artifacts** — this document (+ the decisions and open questions in `plan.md`).

**Dependencies** — none.

**Verification** — the user confirms the shell/router decision and the answers to the four open
questions in `plan.md`; phases 3–8 then run one at a time against that model.

## Requirements (as asked)

1. A left pane with menu-style options, hideable and showable.
2. The menu options available are those the signed-in user is permitted to use.
3. For a platform admin, a **Tenants** option: selecting a tenant lists that tenant's users.
4. Platform users belong to the tenant named **Keystone**.
5. Add / edit / delete for tenants.
6. Edit a user — assign roles, etc.

## Navigation model

The console becomes a shell (`AdminShell`) plus one screen per section. The shell renders the pane and
the AppBar; the router decides which section is shown.

| Section | Route constant | Path | Read permission | Write permission (affordances only) |
| --- | --- | --- | --- | --- |
| Tenants | `AdminRoutes.tenants` | `/tenants` | `platform:tenant:read-only` | `platform:tenant:read-write` |
| Users (all, tenant filter) | `AdminRoutes.users` | `/users` | `platform:user:read-only` | `platform:user:read-write` |
| Tenant users (drill-down) | `AdminRoutes.tenantUsers(id)` | `/tenants/:tenantId` | `platform:user:read-only` | `platform:user:read-write` |
| Roles | `AdminRoutes.roles` | `/roles` | `platform:role:read-only` | `platform:role:read-write` |
| Permissions | `AdminRoutes.permissions` | `/permissions` | `platform:permission:read-only` | `platform:permission:read-write` |

Rules:

- **A section is rendered only if the caller can read its resource.** Read means
  `<resource>:read-only` **or** `<resource>:read-write` **or** the `*` wildcard — the same relation
  the backend guard uses (`PermissionCatalog.acceptedCodes(resource, READ_ONLY)`), so the client never
  offers a section the caller would be denied.
- **Affordances, not security.** Hiding a button is UX only; the backend remains the boundary
  (`docs/ARCHITECTURE.md` §9.6). A deep link into a section the caller may not read renders a
  "not authorized" placeholder rather than calling the API.
- The pane is collapsed/expanded by the AppBar hamburger. Wide layouts (>= 800 px) animate the inline
  pane's width; narrow layouts present the same pane as a `Scaffold.drawer` overlay. When a caller has
  no readable section at all the shell shows a single explanatory placeholder (no empty pane).
- Deep links change section selection; the selected item is derived from `state.matchedLocation` (a
  prefix match, so `/tenants/<id>` selects **Tenants**).

## Tenant → users flow

```
/tenants                (TenantsScreen)
   └── tap a row  ──▶   /tenants/:tenantId   (TenantUsersScreen)  ──▶ GET /api/v1/users?tenantId=<id>
         Keystone row   →  tenant_id IS NULL  (platform users)
         Acme row       →  users.tenant_id = <acme id>
```

- The tenant list stays a plain tenant list (no per-tenant user counts — that would need an aggregate
  endpoint and was not requested).
- The all-users section reuses the same row widget with a tenant dropdown filter (All / Keystone /
  each tenant) so cross-tenant lookups stay possible.

## Platform plane = the `Keystone` tenant

The platform plane is `users.tenant_id IS NULL` (already how login works: `username@keystone`).
To make it selectable like any other tenant, `GET /api/v1/tenants` returns a **synthetic first row**:

```json
{ "id": "00000000-0000-0000-0000-000000000000", "name": "Keystone", "slug": "keystone",
  "platform": true, "createdAt": null, "updatedAt": null }
```

- The id is a reserved constant (`PlatformSchema.PLATFORM_TENANT_ID`); real tenant ids come from
  `UuidIdGenerator` (random v4), so no collision is possible.
- No row is ever written, and `PATCH`/`DELETE` on the reserved id are rejected with `422`
  (`ValidationException`).
- The reserved slug is already the platform plane on login (`TenantResolver`), so tenant
  create/rename must reject it — otherwise a real tenant could shadow the platform plane.
- Because the id is server-provided, the Flutter models need **no** sentinel constants: reads pass the
  id back (`GET /users?tenantId=<id>`) and writes translate it via the row's `platform` flag
  (`tenantId: null`), which keeps the create contract compatible with the existing integration test.

## User editing

| Field | Create | Edit | Why |
| --- | --- | --- | --- |
| `username` | required | editable | The login local-part (`username@tenantid`); login resolves the user by username, so a rename is safe |
| `email` | optional (blank → `username@<slug>.com`) | **read-only** | It is the virtual Supabase/GoTrue identity bound to `users.sub`; the console has no GoTrue email-update path, so editing the row would break login |
| `temporaryPassword` | required | n/a | Password changes go through the first-login flow (`POST /me/password`) |
| `roles` | optional list | editable (replace) | Role checklist filtered by the user's plane: `PLATFORM` roles for platform users, `TENANT` roles for tenant users — exactly the rule `UserService.assignRoles` already enforces |
| `mustChangePassword` | `true` until the first password change | shown read-only | Existing behaviour |

Roles are offered from `GET /api/v1/roles` filtered by scope; a role outside the user's plane is never
offered (and would be rejected by the backend with `422` for the wrong plane).

## Acceptance criteria

1. A platform admin (wildcard) sees Tenants / Users / Roles / Permissions in the left pane; the pane
   can be hidden and shown again, and the body keeps its content.
2. A caller holding only `platform:tenant:read-only` and `platform:tenant:read-write` sees **Tenants**
   only; Add/Edit/Delete tenant are visible; no other section is listed.
3. A caller holding only `platform:tenant:read-only` sees Tenants with **no** write affordances.
4. Selecting the `Keystone` row lists exactly the platform users (`users.tenant_id IS NULL`);
   selecting a tenant lists exactly that tenant's users.
5. Tenant create, edit (name/slug) and delete work from the UI; deleting a tenant that still has users
   fails with the backend's conflict message; the `Keystone` row has no edit/delete.
6. A user can be renamed and have its roles replaced from one dialog; the role list offers only roles
   of the user's plane; the change is one `PATCH` request.
7. `GET /api/v1/users` without `tenantId` still returns every user (existing behaviour).
8. No schema change, no Liquibase changeset, no jOOQ codegen churn, no new configuration.

## Out of scope

- Tenant-plane console sections (a tenant admin managing its own users/roles).
- Realtime/broadcast of admin changes.
- GoTrue email updates (so `email` stays immutable from the console).
- Role/permission edit + delete in the UI.

# Keystone — Architecture

> **Status:** Accepted (target architecture) · **Last updated:** 2026-09-20
>
> This document defines the system-level architecture of **Keystone**. It is the point of
> reference for the coding guidelines (`docs/CODING_GUIDELINES_BACKEND.md`,
> `docs/CODING_GUIDELINES_FRONTEND.md`).

## 1. Overview

Keystone is a **platform** on which independent **applications** are built and hosted:

- **Platform** (`platform/keystone-*`) — shared, framework-level Java libraries: common
  errors/ids/time, a Guice + Javalin web base, jOOQ/Liquibase data infrastructure, a Supabase
  Realtime publisher, OIDC security, observability, and shared test support. Published as one
  versioned unit (the `keystone-bom`).
- **Applications** (`apps/<name>`) — a **holder** for that app's services and frontend. Each
  app contains a `server/` (Java backend: Guice `Main`, Javalin routes, its own schema, jOOQ
  codegen, and Cloud Run deployment) and a `frontend/` (Flutter — web, iOS, Android), and can
  grow more services under the same app over time.

An application exposes two channels to its client:

1. **Request/response** — REST calls (HTTPS/JSON) to the app's Javalin service.
2. **Realtime streaming** — a WebSocket connection to **Supabase Realtime**.

Each app owns its business logic and data. It persists to its own **schema** in a shared
**Supabase-managed PostgreSQL** database and, when it needs to push live updates, **publishes a
message through Supabase Realtime**, which fans the message out to every subscribed client.

## 2. System Context

```text
                          ┌───────────────────────────────┐
                          │         Flutter client        │
                          │     (web · iOS · Android)     │
                          └───────┬───────────────┬───────┘
                 REST (HTTPS/     │               │  Realtime (WebSocket)
                     JSON)        │               │
                                  ▼               ▼
         ┌────────────────────────────────┐  ┌───────────────────────────────┐
         │     Java backend service       │  │           Supabase            │
         │      (Google Cloud Run)        │  │  ┌─────────────────────────┐  │
         │  ┌──────────────────────────┐  │  │  │  Realtime (broadcast)   │◄─┼── publish
         │  │  Guice · Javalin · jOOQ  │  │──▶│  └─────────────────────────┘  │
         │  └──────────────────────────┘  │  │  ┌─────────────────────────┐  │
         └───────────────┬────────────────┘  │  │  Managed PostgreSQL      │  │
                         │                   │  │  (Liquibase changelogs)  │  │
                         └───── SQL (jOOQ) ──▶│  └─────────────────────────┘  │
                                             └───────────────────────────────┘
```

- **Flutter → Java**: REST (HTTPS/JSON) for request/response operations.
- **Flutter → Supabase Realtime**: WebSocket subscription for live updates.
- **Java → Supabase Realtime**: publish a message to be broadcast.
- **Java → Supabase PostgreSQL**: SQL via jOOQ against the Liquibase-managed schema.

The Flutter **web** build is served from **Firebase Hosting** (global CDN with TLS); iOS and
Android builds are distributed through their app stores.

The diagram shows **one application**. Each app is an independent instance of this shape,
hosted in its own **GCP project** (its own Cloud Run service + a schema in a shared database),
sharing the platform libraries (see §4).

## 3. Technology Stack

| Concern | Choice |
| --- | --- |
| Client | Flutter (Dart 3) — web, iOS, Android |
| Client state & models | Riverpod (codegen), freezed + json_serializable |
| Client REST | dio (HTTPS/JSON) → Java service |
| Client realtime | Supabase Realtime client (WebSocket) |
| Client hosting | Firebase Hosting (Flutter web — global CDN + TLS) |
| Backend language | Java 25 (LTS) |
| Dependency injection | Guice |
| HTTP / REST | Javalin (embedded Jetty) |
| Data access | jOOQ (type-safe, fluent SQL) |
| Schema management | Liquibase (XML changelogs) |
| Code generation | jOOQ codegen — offline, from the Liquibase changelog (no live DB) |
| Database | Supabase-managed PostgreSQL |
| Realtime | Supabase Realtime (WebSocket broadcast) |
| Backend deployment | Google Cloud Run (auto-scaling, serverless containers) |
| Container image | Docker/OCI image, built with Jib (no Docker daemon required) |
| Build | Maven (single build tool) |
| Structure | Monorepo — `platform/` (shared libraries) + `apps/` (hosted applications) |

## 4. Components

Keystone is a monorepo split into two layers:

- **Platform** (`platform/keystone-*`) — shared libraries every app reuses, depended on through
  a single versioned BOM (`keystone-bom`).
- **Applications** (`apps/<name>`) — holder modules. Each contains a `server/` (the Java
  backend service) and a `frontend/` (the Flutter frontend); more services can be added under
  the same app as it grows.

### 4.0 Platform libraries

| Module | Responsibility |
| --- | --- |
| `keystone-bom` | Versioned BOM that pins platform module versions for apps. |
| `keystone-common` | Errors, ids, time, RFC 9457 problem+json. |
| `keystone-web` | Guice + Javalin base: wiring, filters, validation, error handler. |
| `keystone-data` | jOOQ/Liquibase infrastructure: `DataAccess` read/write split (primary + read replica), `DSLContext`, transactions, DAO conventions. |
| `keystone-realtime` | Supabase Realtime publisher (`RealtimePublisher`). |
| `keystone-security` | OIDC resource-server JWT validation + authorization. |
| `keystone-observability` | Micrometer metrics, structured logging, correlation id. |
| `keystone-testing` | Shared Testcontainers/JavalinTest base and fixtures. |

### 4.1 Flutter client

- One codebase for web, iOS, Android.
- **Web hosting**: the web build is deployed to **Firebase Hosting** (global CDN + TLS);
  iOS/Android ship through their app stores.
- **REST**: `dio` calls the Java service's HTTPS/JSON endpoints.
- **Realtime**: the Supabase Realtime client subscribes to channels over WebSocket and
  receives messages broadcast by the backend (see §5.2).
- **Auth**: OAuth2 / OpenID Connect with PKCE (`flutter_appauth`); the client holds an access
  token and sends it to the Java service and Supabase Realtime as appropriate.
- State and models are immutable (`freezed`) and managed with Riverpod.

### 4.2 Application service (per app)

- **Guice** wires the application: one `Module` per feature/infrastructure concern, explicit
  `bind()`/`@Provides`, constructor injection. A `Main` entry point builds the `Injector`.
- **Javalin** provides the HTTP layer on an embedded Jetty server: routing, JSON
  serialization, request validation, and a single global exception handler that maps errors
  to RFC 9457 `application/problem+json`.
- **jOOQ** is the data-access layer. Services use the `DataAccess` facade with generated
  records: `read()` routes to a read replica, `write()` to the primary (read-write) instance,
  and `readFromPrimary(...)` scopes read-your-writes. Transactions run at the service boundary
  on the primary.
- **Liquibase** owns the schema. Changelogs are XML and are the single source of truth for
  the database shape.

### 4.3 Supabase

Supabase is used for two managed capabilities:

1. **Database** — a managed PostgreSQL instance. Apps are isolated by **per-app schema**
   (`inventory`, `platform`, …) in a shared database, or by separate databases — the choice is
   per-environment configuration (`database.url` + optional `database.schema`). The Java service is the only
   direct writer of its schema; the schema is applied via Liquibase changelogs.
2. **Realtime** — Supabase Realtime provides the WebSocket broadcast infrastructure. It is
   **not** an application data store; it is the fan-out mechanism for live updates.

### 4.4 Google Cloud Run + Artifact Registry

- Each application runs as a **serverless container** on Cloud Run, which auto-scales
  (including to zero) based on request concurrency.
- Google's front-end terminates TLS and routes traffic, so no self-managed nginx/ingress is
  required.
- Container images are pushed to **Artifact Registry** and referenced by Cloud Run services.
- **One Cloud Run service per application**, typically in its own GCP project, for isolation
  of billing, IAM, and blast radius.

## 5. Data Flows

### 5.1 Request/response (REST)

```text
Flutter client ──HTTPS/JSON──▶ Javalin (Java) ──jOOQ──▶ Supabase PostgreSQL
        ◀─────────────────────────────────────────────┘
```

1. The Flutter client calls a Javalin route with an access token.
2. Javalin authenticates/authorizes the request, validates the DTO, and delegates to an
   application service.
3. The service runs a jOOQ query/transaction against PostgreSQL and returns a typed DTO.
4. Javalin serializes the DTO to JSON and returns the appropriate HTTP status.
5. **List reads are paged, searched and filtered on the server.** A list route takes
   `page`, `size`, `sort`, `order`, `q` plus the resource's own filters (`tenantId`, `scope`) and
   answers with a typed page envelope —
   `{ items, page, size, totalElements, totalPages, hasNext, hasPrevious }` — so a search covers the
   whole collection, not the page the client happens to hold. Controls that must offer *every*
   choice (a tenant or owner dropdown, a role checklist) read the resource's unpaged `/options`
   endpoint instead of a page. The frozen contract is `docs/CODING_GUIDELINES_BACKEND.md` §8 (*List
   endpoints*); the UX rules built on it are `docs/UX_GUIDELINES.md` §1.

### 5.2 Realtime streaming (broadcast)

```text
Flutter client          Supabase Realtime              Java backend
     │                        │                              │
     │  subscribe (WebSocket) │                              │
     │───────────────────────▶│                              │
     │                        │                              │   business event occurs
     │                        │                              │   (persist via jOOQ)
     │                        │◀───── publish(message) ──────│
     │                        │                              │
     │◀──── broadcast (WS) ───│                              │
```

1. The Flutter client opens a WebSocket and subscribes to a Realtime channel (e.g. per
   resource or per tenant).
2. When the Java service needs to push a live update, it **publishes a message to Supabase
   Realtime** (the trigger for broadcast).
3. Supabase Realtime broadcasts the message to every client subscribed to that channel.
4. The Flutter client updates its local state from the broadcast payload (immutable records).

> The Java service is the **originator** of broadcasts; Supabase Realtime is the **transport**
> and fan-out. Broadcast payloads are small, immutable, versioned messages.

### 5.3 Authentication

- The Flutter client authenticates via **backend-proxied login**: it POSTs `username@tenantid` +
  password to the Java service, which authenticates against Supabase Auth (password grant) and
  returns the OIDC session (access + refresh tokens), stored in `flutter_secure_storage`.
- It sends the access token to the Java service (Authorization header) for REST calls.
- The Java service validates the token (resource-server semantics) and enforces authorization.
- Realtime channel access is restricted to authorized clients (Supabase Realtime auth / RLS
  as appropriate).
- See §9 for the full identity, tenancy, and authorization model.

## 6. Database & Schema Management

### 6.1 Liquibase as the source of truth

- The database schema is defined by **Liquibase XML changelogs**, versioned in the repo.
- Every schema change is a new changelog entry; the master changelog is applied idempotently.
- No ad-hoc DDL, and no ORM-generated schema.

### 6.2 jOOQ code generation (offline, from Liquibase)

- jOOQ generates code **from the Liquibase changelog**, **not** from a live database.
- At build time the changelog is rendered to DDL (Liquibase offline / `updateSQL`) and fed to
  jOOQ's offline code generator (`DDLDatabase`).
- Generated tables, records, and sequences are written to a generated-sources directory and
  compiled into the module.
- This keeps codegen deterministic and CI-friendly: no database connection is required to
  build.
- The changelog is rendered with the module's schema (Liquibase `defaultSchemaName` +
  `outputDefaultSchema=true`), so generated tables are **schema-qualified**
  (`"platform"."users"`, `"inventory"."items"`) and jOOQ renders `"schema"."table"` — SQL never
  depends on the connection's `search_path`.
- Schema-qualified SQL is deliberate: Supabase's PgBouncer (transaction mode, port 6543) does
  not reliably persist `SET SESSION search_path`, so relying on it breaks multi-schema access.
  Raw `<sql>` in a changelog must be schema-qualified with `${database.defaultSchemaName}` to
  keep the rendered DDL consistent.

## 7. Build, Packaging & Deployment

### 7.1 Build

- Maven is the build tool.
- The build runs Liquibase → jOOQ codegen → compile → test (unit + Testcontainers
  integration against a real PostgreSQL).
- Each application runs this pipeline against **its own** changelog and generated jOOQ types —
  codegen lives in the app, not in `keystone-data`.

### 7.2 Container image (Jib)

- The Java service is packaged as a **Docker image**.
- The image is built with **Jib** (optionally, as part of the Java build) and pushed to
  Artifact Registry. Jib builds the image without a local Docker daemon and produces
  reproducible, layered images.

### 7.3 Cloud Run deployment

- Cloud Run serves the image as a revision, auto-scaling on request concurrency.
- Deployments are immutable revisions; a new image push + service update rolls out a new
  revision with traffic splitting and rollback.
- Each application deploys independently to its own Cloud Run service (and GCP project).

### 7.4 Frontend hosting (Firebase)

- The Flutter **web** build (`flutter build web`) is deployed to **Firebase Hosting**
  (`firebase deploy --only hosting`) — a global CDN with TLS and atomic, previewable
  rollouts.
- iOS and Android builds ship through their app stores, not Firebase.

## 8. Configuration & Secrets

- All environment-specific configuration lives in per-environment yaml files
  (`application-{env}.yaml`, `admin-config/application-{env}.yaml`), selected by `APP_ENV`.
- Only **secrets** are externalized via environment variables (Google Secret Manager → Cloud Run):
  - Supabase PostgreSQL connection password (`DB_PASSWORD`),
  - Supabase service-role key (`SUPABASE_SERVICE_ROLE_KEY`),
  - Supabase Realtime service-role key (`REALTIME_SERVICE_ROLE_KEY`),
  - bootstrap admin password (`BOOTSTRAP_ADMIN_PASSWORD`).
- Non-secret values (URLs, usernames, schema names, OIDC issuer/audience/JWKS URL, port, context
  path, CORS allowed origins) live in the yaml files, not the environment. The first platform admin's
  identity is one of them: `bootstrap.adminUsername` / `bootstrap.adminEmail` are set per environment
  in `admin-config/application-{env}.yaml` with **no built-in default**, so a deployment decides who
  its bootstrap user is and a missing value fails fast when the bootstrap is enabled.
- **Operational startup switches are the one non-secret exception**: they have a yaml default and may
  be flipped per deployment through an environment variable, so the same image can start with
  automatic migrations and/or the first-user bootstrap on or off.
  - `MIGRATE_ON_START` (yaml `startup.migrateOnStart`, default `true`) — run Liquibase at startup for
    both schemas; `false` means migrations are applied out of band (`SchemaTool migrate` /
    `scripts/migrate-schema.sh`) before the revision serves traffic.
  - `BOOTSTRAP_ON_START` (yaml `bootstrap.enabled`, default `true`) — seed the permission catalog, the
    `platform-admin` role and the first admin user at startup; `false` skips the seed entirely (so the
    bootstrap admin credentials are then not required), and the seed is run out of band instead with
    `BootstrapTool` / `scripts/bootstrap-admin.sh`.
- No secrets in source, images, or logs.

## 9. Identity, Tenancy & Authorization

### 9.1 Authentication (identity)

- **OIDC / OAuth2 resource server** is the model. The Flutter client authenticates with
  **PKCE** (`flutter_appauth`); the Java service only *validates* JWTs — it never stores or
  checks a password.
- **Identity provider**: **Supabase Auth** is the default. It is already part of the stack,
  manages credential storage (bcrypt-hashed, in its own `auth.users`), and issues one JWT
  that authenticates **both** REST (validated by the Java service) and Realtime (channel
  auth). An **external IdP** (Auth0, Keycloak, Entra/Azure AD, Firebase Auth) is a drop-in
  alternative because validation is against `issuer` + `audience` + `JWKS`
  (`SecurityConfig`), not a specific vendor.
- The Java service stores only the token **`sub`** as a reference to the user; it never
  stores credentials.
- **Password operations are proxied, never stored.** The service reads no credential, but it is the only
  party holding the Supabase service-role key, so exactly two operations go through it: a user changing
  **their own** password (`POST /api/v1/me/password`) must prove the current one — verified with a
  Supabase password grant for their own Auth identity, which is why only the forced first-login flow
  (where that password was just used to sign in, `users.must_change_password`) skips the proof — and an
  administrator resetting **someone else's** password (`PUT /api/v1/users/{id}/password`) sets a
  temporary password and re-arms the forced change. Both are logged server-side with the target user id
  and the actor `sub`; no password, token or key is ever logged. Login failures stay uniform (`403`) to
  prevent account enumeration, while *why* a Supabase grant failed (HTTP status + GoTrue code) is logged
  for the operator instead of being returned to the client.

### 9.2 Tenancy

- **Two authorization planes**:
  - **Platform** — cross-tenant: create customers (tenants), manage the platform.
  - **Tenant** — within one customer: manage that customer's users and roles.
- A **platform user** has no tenant (`users.tenant_id IS NULL`); a **tenant user** belongs to
  exactly one tenant. Single-tenant membership is the default; multi-tenant membership is a
  future extension via a join table.
- The platform plane is surfaced to the admin console as a **synthetic tenant** named
  `Keystone` (`PlatformSchema.PLATFORM_TENANT_NAME`, reserved slug `keystone` and reserved id
  `00000000-0000-0000-0000-000000000000`). `GET /api/v1/tenants` returns it as the **pinned first row**
  of the **paged list**: page 0's first item, counted in `totalElements`, and searchable by `q` like any
  other row (`platform: true`, null timestamps) — so it is absent from a result whose term it does not
  match. It is never persisted (real tenant ids are random v4
  UUIDs), writes addressing it are rejected with `422`, and passing its id as `tenantId` selects
  the platform users (`users.tenant_id IS NULL`). This lets the console list and edit platform
  users exactly like a tenant's users. The reserved slug is also rejected for tenant creation
  and rename, because the login resolver reads `username@keystone` as the platform plane.

### 9.3 Authorization (RBAC)

- **Roles** group **permissions**; roles are assigned to **users**. Code checks
  **permissions only, never roles**, so the role taxonomy can change without a code change.
- **Effective permissions** = the union of permissions across all of a user's roles, resolved
  in the current tenant context.
- Permission codes are namespaced `<scope>:<resource>:<level>`, and there are exactly **two access
  levels**: `read-write` (read, create, update and delete) and `read-only` (read only). A
  read/write grant also satisfies a read-only check. Examples: `platform:tenant:read-write`,
  `tenant:user:read-only`, and domain permissions such as `inventory:item:read-write`.
- The catalog seeds both levels for every resource (`platform:<resource>:*` and
  `tenant:<resource>:*` for tenants, roles, permissions and users) plus the `*` wildcard, which only
  `platform-admin` holds. Role assignment is a write on the user resource
  (`platform:user:read-write`), not a permission of its own.
- Roles and permissions are **owned**: `tenant_id IS NULL` is the *global* catalog, applicable to the
  platform plane **and every tenant**; a tenant id is a row only that tenant (and the platform plane)
  can see and use. Ownership is orthogonal to `scope`, with one guardrail — a tenant-owned row must be
  `TENANT` scope, so a tenant can never own a cross-tenant capability. `code` is therefore unique **per
  owner**, not globally: two tenants can each have a `manager` role, and a tenant may not re-define a
  code the global catalog already carries (a code never means two things for one caller).
- The two **seeded administrative roles** mirror each other: the global `platform-admin` (the wildcard)
  and one tenant-owned `admin` per tenant, granted the **read/write** `TENANT`-scope codes only — a
  read/write grant already satisfies every read check, so the read-only codes would be redundant. Both
  are immutable (they cannot be renamed or deleted on either plane), so nobody can lock themselves out.

### 9.4 Schema

```sql
tenants          (id uuid PK, name text, slug text UNIQUE, ...)
users            (id uuid PK, sub text UNIQUE, tenant_id uuid NULL REFERENCES tenants, ...)
roles            (id uuid PK, code text, scope text CHECK (scope IN ('PLATFORM','TENANT')),
                  tenant_id uuid NULL REFERENCES tenants, ...)
permissions      (id uuid PK, code text, scope text CHECK (scope IN ('PLATFORM','TENANT')),
                  tenant_id uuid NULL REFERENCES tenants, ...)
role_permissions (role_id FK, permission_id FK, PRIMARY KEY (role_id, permission_id))
user_roles       (user_id FK, role_id FK, tenant_id uuid NULL, PRIMARY KEY (user_id, role_id))
```

- `tenant_id` is the row's **owner** on `roles` and `permissions`: `NULL` = the global catalog, a
  tenant id = owned by that tenant. `CHECK (tenant_id IS NULL OR scope = 'TENANT')` keeps a tenant from
  owning a cross-tenant capability.
- `code` is unique **per owner**: `UNIQUE (code, tenant_id)` for tenant rows plus a **partial** unique
  index `(code) WHERE tenant_id IS NULL` for the global pool — a plain `UNIQUE (code, tenant_id)`
  would let two global rows share a code, because a unique index treats `NULL`s as distinct.
- `user_roles.tenant_id` is `NULL` for platform roles and set for tenant roles; a tenant-owned role can
  only be assigned inside its own tenant, and a `PLATFORM`-scope role never goes to a tenant user. These
  plane rules are enforced in the services (they span tables, so they are not expressible as a `CHECK`).
- Effective permissions are computed as `user_roles ⋈ role_permissions`, filtered by
  `ur.tenant_id IS NULL OR ur.tenant_id = :tenantContext`.
- Indexes exist for the queries the code runs, not speculatively: `idx_roles_tenant_code` /
  `idx_permissions_tenant_code` (the owner-scoped list), `idx_role_permissions_permission`
  ("roles holding a permission"), `idx_user_roles_tenant` and `idx_users_tenant`.

### 9.5 Delegated administration

- **Platform admin** provisions tenants — which **seeds that tenant's `admin` role in the same
  transaction** — and each tenant's first admin, and assigns platform roles. Resetting a user's password
  is part of that grant: it is a write on `platform:user` (`PUT /api/v1/users/{id}/password`), it sets a
  *temporary* password that forces a change on the user's next login, and it **refuses the caller's own
  account** — your own password goes through the verified change-password flow, so holding the user-write
  grant never makes the current-password proof optional.
- **Tenant admin** — a holder of the tenant's own `admin` role — creates and manages **its own
  tenant's** users, roles and permissions, and nothing outside it.
- **Guardrails**:
  - *Scope* — a tenant admin cannot grant a `PLATFORM`-scope role or permission; everything a tenant
    owns is `TENANT` scope (the database `CHECK` is the backstop).
  - *No escalation* — a tenant admin can only grant permissions it holds and assign roles whose
    permissions are a subset of its own ("grant only what you hold"), and never the wildcard. Holding a
    read/write code counts as holding the read-only code of the same resource (write implies read).
  - *Ownership* — a global row is read-only to a tenant (`403`), another tenant's row simply does not
    exist for it (`404`), and the seeded admin roles are immutable on both planes.
  - *Catalog* — roles start as a **platform-defined catalog** (`tenant_id IS NULL`); a tenant may add
    its **own** roles and permissions, but may not re-define a code the catalog already carries.

### 9.6 Enforcement

- **Two planes, two guard families**: the platform plane (`/api/v1/…`, `platform:<resource>:*`) and the
  **tenant self-service** plane (`/api/v1/tenant/…`, `tenant:<resource>:*`). Both are authoritative.
- **The tenant is derived, never supplied.** `PermissionGuard.callerScope(ctx)` resolves the caller's
  tenant from its own `users` row and their effective permissions **in that tenant's context**. A tenant
  route has no path segment, query parameter or body field that could name a tenant — which is what makes
  cross-tenant access structurally impossible rather than merely filtered — and a platform caller is
  refused on those routes (`403`), since it has the platform plane for the same resources.
- **Backend (authoritative)**: the guard at the handler boundary plus explicit ownership checks in the
  services. Denial throws `AccessDeniedException` → RFC 9457 `403`.
- **Frontend (UX only)**: the backend exposes the effective set via `GET /me`
  (`sub`, `username`, `tenantId`, `permissions[]`). The Flutter client renders controls from it
  (Riverpod + `go_router` redirect + per-resource capability flags on DTOs). The console's
  navigation is built the same way: a section is listed only when the caller holds its
  `<resource>:read-only` or `<resource>:read-write` code (or the `*` wildcard), and write
  affordances require the `:read-write` code — an unreadable deep link renders a
  "not authorized" placeholder instead of calling the API. The backend is the only security
  boundary; a bypassed client still receives `403`.

## 10. Cross-Cutting Concerns

- **Security**: OIDC resource-server token validation; RBAC authorization at the service
  boundary (see §9).
- **Observability**: SLF4J + Logback structured logging; Micrometer metrics; trace/tenant
  correlation via `ScopedValue`.
- **Error handling**: one global exception handler → RFC 9457 `application/problem+json`.
- **Idempotency**: mutation endpoints and broadcast consumers tolerate retries/duplicates.
- **Graceful shutdown**: honor Cloud Run's SIGTERM to drain in-flight requests.
- **CORS**: configured per environment via `cors.allowedOrigins` — wildcard `*` in dev for the
  local Flutter web client, explicit origins in prod.

## 11. Key Decisions & Rationale

| Decision | Rationale |
| --- | --- |
| Guice (DI) | Lightweight, explicit, code-based wiring; no heavy container/auto-configuration. |
| Javalin (HTTP) | Minimal embedded-Jetty framework; pairs cleanly with Guice. |
| jOOQ (data access) | Type-safe, SQL-first; full control over queries vs. an ORM. |
| Liquibase + offline jOOQ codegen | Schema is versioned in the repo; codegen is deterministic with no live-DB dependency. |
| Supabase Realtime | Managed WebSocket broadcast; less infrastructure to operate. |
| Cloud Run + Jib | Serverless auto-scaling; minimal ops; reproducible container builds. |
| Firebase Hosting (Flutter web) | Global CDN + TLS; trivial static hosting with atomic rollbacks. |
| Monorepo (`platform/` + `apps/`) | Atomic cross-cutting changes now; apps can be split into separate repos later. |
| One app = one GCP project | Isolation of billing, IAM, quotas, and blast radius. |
| Supabase Auth (IdP) + RBAC | Managed credentials; code checks permissions (not roles) across platform/tenant planes with delegated administration. |

## 12. Open Questions

- Exact Supabase Realtime **publish** mechanism from Java (Realtime broadcast REST endpoint
  vs. a server-side Realtime WebSocket client).



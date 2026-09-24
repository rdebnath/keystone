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
| `keystone-data` | jOOQ/Liquibase infrastructure: `DSLContext`, transactions, DAO conventions. |
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
- **jOOQ** is the data-access layer. Services use a `DSLContext` with generated records and
  execute SQL in transactions at the service boundary.
- **Liquibase** owns the schema. Changelogs are XML and are the single source of truth for
  the database shape.

### 4.3 Supabase

Supabase is used for two managed capabilities:

1. **Database** — a managed PostgreSQL instance. Apps are isolated by **per-app schema**
   (`inventory`, `platform`, …) in a shared database, or by separate databases — the choice is
   per-environment configuration (`DB_URL` + optional `DB_SCHEMA`). The Java service is the only
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

- All environment-specific configuration is externalized via environment variables and,
  where sensitive, **Google Secret Manager**.
- Required secrets/values:
  - Supabase PostgreSQL connection string (+ per-app `DB_SCHEMA`),
  - Supabase Realtime endpoint + publish key/secret,
  - OAuth2/OIDC issuer + audience (for token validation).
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

### 9.2 Tenancy

- **Two authorization planes**:
  - **Platform** — cross-tenant: create customers (tenants), manage the platform.
  - **Tenant** — within one customer: manage that customer's users and roles.
- A **platform user** has no tenant (`users.tenant_id IS NULL`); a **tenant user** belongs to
  exactly one tenant. Single-tenant membership is the default; multi-tenant membership is a
  future extension via a join table.

### 9.3 Authorization (RBAC)

- **Roles** group **permissions**; roles are assigned to **users**. Code checks
  **permissions only, never roles**, so the role taxonomy can change without a code change.
- **Effective permissions** = the union of permissions across all of a user's roles, resolved
  in the current tenant context.
- Permission codes are namespaced: `platform:tenant:create`, `tenant:user:create`,
  `tenant:role:assign`, and domain permissions such as `inventory:item:write`.

### 9.4 Schema

```sql
tenants          (id uuid PK, name text, ...)
users            (id uuid PK, sub text UNIQUE, tenant_id uuid NULL REFERENCES tenants, ...)
roles            (id uuid PK, code text, scope text CHECK (scope IN ('PLATFORM','TENANT')))
permissions      (id uuid PK, code text, scope text CHECK (scope IN ('PLATFORM','TENANT')))
role_permissions (role_id FK, permission_id FK, PRIMARY KEY (role_id, permission_id))
user_roles       (user_id FK, role_id FK, tenant_id uuid NULL, PRIMARY KEY (user_id, role_id, tenant_id))
```

- `user_roles.tenant_id` is `NULL` for platform roles and set for tenant roles; `CHECK`
  constraints enforce that `TENANT` roles always carry a tenant and `PLATFORM` roles never do.
- Effective permissions are computed as `user_roles ⋈ role_permissions`, filtered by
  `ur.tenant_id IS NULL OR ur.tenant_id = :tenantContext`.

### 9.5 Delegated administration

- **Platform admin** provisions tenants, seeds each tenant's first admin, and assigns
  platform roles.
- **Tenant admin** creates users within their own tenant and assigns **tenant-scoped** roles
  only.
- **Guardrails**:
  - *Scope* — a tenant admin cannot grant a `PLATFORM` role.
  - *No escalation* — a tenant admin can only grant roles whose permissions are a subset of
    their own ("grant only what you hold").
  - *Catalog* — roles start as a **platform-defined catalog**; tenant-defined custom roles
    are a later option.

### 9.6 Enforcement

- **Backend (authoritative)**: a `@RequirePermission(...)` guard at the handler boundary plus
  explicit checks in services for resource-level rules (ownership). Denial throws
  `AccessDeniedException` → RFC 9457 `403`.
- **Frontend (UX only)**: the backend exposes the effective set via `GET /me`
  (`sub`, `tenantId`, `permissions[]`). The Flutter client renders controls from it (Riverpod
  + `go_router` redirect + per-resource capability flags on DTOs). The backend is the only
  security boundary; a bypassed client still receives `403`.

## 10. Cross-Cutting Concerns

- **Security**: OIDC resource-server token validation; RBAC authorization at the service
  boundary (see §9).
- **Observability**: SLF4J + Logback structured logging; Micrometer metrics; trace/tenant
  correlation via `ScopedValue`.
- **Error handling**: one global exception handler → RFC 9457 `application/problem+json`.
- **Idempotency**: mutation endpoints and broadcast consumers tolerate retries/duplicates.
- **Graceful shutdown**: honor Cloud Run's SIGTERM to drain in-flight requests.
- **CORS**: enable for the Flutter web origin.

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



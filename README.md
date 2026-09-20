# Keystone

A platform for building and hosting Keystone applications, each deployed independently to its
own GCP project.

## Layout

```
platform/     shared, framework-level libraries (versioned together via keystone-bom)
apps/         deployable applications (each a holder for its services + frontend)
docs/         architecture + coding guidelines
```

| Module | Type | Purpose |
| --- | --- | --- |
| `platform/keystone-bom` | BOM | Pins platform module versions for apps. |
| `platform/keystone-common` | library | Errors, ids, time, RFC 9457 problem+json. |
| `platform/keystone-web` | library | Guice + Javalin base (wiring, filters, validation, error handling). |
| `platform/keystone-data` | library | jOOQ/Liquibase infrastructure (DSLContext, transactions, DAO conventions). |
| `platform/keystone-realtime` | library | Supabase Realtime publisher. |
| `platform/keystone-security` | library | OIDC resource-server JWT validation + authorization. |
| `platform/keystone-observability` | library | Micrometer metrics + structured logging. |
| `platform/keystone-testing` | library | Shared Testcontainers/JavalinTest base + fixtures. |
| `apps/inventory` | application | Inventory — a holder for its `server/` and `frontend/`. |
| `apps/inventory/server` | service | Inventory backend (Guice + Javalin + jOOQ). |
| `apps/platform` | application | Platform admin console — a holder for its `server/` and `frontend/`. |
| `apps/platform/server` | service | Platform admin backend (identity/tenancy/RBAC administration). |
| `apps/platform/frontend` | client | Platform admin console (Flutter — web, iOS, Android). |

Dependency direction: `apps/* → platform/*` (via `keystone-bom`). Apps never depend on each
other.

## Requirements

- **Java 25 (LTS)** — pinned.
- Maven 3.9+.

## Build

```bash
mvn clean verify
```

## Run an application (local)

Build the jar and run the app's Guice `Main` (requires a reachable PostgreSQL):

```bash
mvn -pl apps/inventory/server -am package
mvn -pl apps/inventory/server dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "apps/inventory/server/target/classes:$(cat apps/inventory/server/target/classpath.txt)" com.chetana.keystone.inventory.Main
```

## Container image (Cloud Run)

Each app builds its own image with Jib and deploys to its own Cloud Run service:

```bash
mvn -pl apps/inventory/server -am jib:build -Djib.to.image=<registry>/inventory
```

## Runtime dependencies

Each app owns its database (Supabase-managed PostgreSQL). For local development,
`docker compose up -d` starts a local PostgreSQL (Testcontainers also needs Docker for the
integration tests).

## JDK pinning

Java 25 is pinned via Maven `maven.compiler.release`. Build with a JDK 25 on `JAVA_HOME`:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
```

## Conventions

- Platform code lives under `platform/`; application code under `apps/`.
- Backend guidelines: `docs/CODING_GUIDELINES_BACKEND.md`.
- Build configuration lives in each module's `pom.xml`.
- Per-app environment configuration lives in `<app>/server/src/main/resources/config/`
  (`application.yaml` + `application-{env}.yaml`), selected by `APP_ENV` and overridable by
  environment variables (see `docs/CODING_GUIDELINES_BACKEND.md` §5).

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
| `platform/keystone-data` | library | jOOQ/Liquibase infrastructure (`DataAccess` read/write split — primary + read replica, `DSLContext`, transactions, DAO conventions). |
| `platform/keystone-realtime` | library | Supabase Realtime publisher. |
| `platform/keystone-security` | library | OIDC resource-server JWT validation + authorization. |
| `platform/keystone-observability` | library | Micrometer metrics + structured logging. |
| `platform/keystone-testing` | library | Shared Testcontainers/JavalinTest base + fixtures. |
| `platform/keystone-admin` | library | Platform admin console backend — identity/tenancy/RBAC, backend-proxied login, bootstrap. Hosted by apps. |
| `platform/keystone-admin-ui` | library | Platform admin console UI (Flutter package — login + admin screens). Hosted by apps. |
| `apps/inventory` | application | Inventory — a holder for its `server/` and `frontend/`. |
| `apps/inventory/server` | service | Inventory backend (Guice + Javalin + jOOQ); hosts the platform admin console. |
| `apps/inventory/frontend` | client | Inventory frontend (Flutter — web, iOS, Android); hosts the platform admin UI. |

Dependency direction: `apps/* → platform/*` (via `keystone-bom`). Apps never depend on each
other.

## Requirements

- **Java 25 (LTS)** — pinned.
- Maven 3.9+.

## Build

```bash
mvn clean verify
```

## Run an application

Build the jar and run the app's Guice `Main` against the remote Supabase Postgres (selected by
`APP_ENV`, default `dev`); it requires the `DB_PASSWORD` secret (and, for the admin console,
`SUPABASE_SERVICE_ROLE_KEY`):

```bash
mvn -pl apps/inventory/server -am package
mvn -pl apps/inventory/server dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "apps/inventory/server/target/classes:$(cat apps/inventory/server/target/classpath.txt)" com.chetana.keystone.inventory.Main
```

The server listens on port `8080` and serves every route under the `/inventory` context path
(e.g. `http://localhost:8080/inventory/api/v1/...`). The Flutter web client points its
`API_BASE_URL` at `http://localhost:8080/inventory`.

Logging goes to stdout and is configured by the service's own
`apps/inventory/server/src/main/resources/logback.xml`; set `LOG_LEVEL` (default `INFO`) to raise or
lower verbosity per environment. Platform libraries ship no logging configuration — only a
test-scoped `logback-test.xml` (`docs/CODING_GUIDELINES_BACKEND.md` §10).

At startup it applies the Liquibase changelogs of both schemas and bootstraps the platform admin.
Both are optional, per deployment:

```bash
MIGRATE_ON_START=false    java ...   # skip startup Liquibase; apply it first with `SchemaTool migrate`
BOOTSTRAP_ON_START=false  java ...   # skip seeding the permission catalog, platform-admin role and admin user
```

The yaml equivalents are `startup.migrateOnStart` and `bootstrap.enabled` (both default `true`), so an
environment file can set them without any environment variable. With the automatic steps switched
off, run them as explicit steps instead — `scripts/migrate-schema.sh` and
`scripts/bootstrap-admin.sh` (see "Schema management (CLI)"); to force them back on for a local run,
use `scripts/start-server-migrate-and-bootstrap.sh` (see "Dev scripts").

## Dev scripts

Convenience wrappers in `scripts/` (default app `inventory`; pass an app name as the first
argument):

```bash
scripts/start-server.sh inventory     # build + run the Java backend service (needs DB_PASSWORD,
                                      #   SUPABASE_SERVICE_ROLE_KEY; APP_ENV defaults to dev)
scripts/start-server-migrate-and-bootstrap.sh inventory
                                      # same, with both startup steps forced on
                                      #   (MIGRATE_ON_START=true + BOOTSTRAP_ON_START=true)
scripts/start-web.sh inventory        # flutter run for the web frontend (injects API_BASE_URL)
scripts/migrate-schema.sh inventory   # apply the Liquibase changelogs of both schemas (needs DB_PASSWORD)
scripts/bootstrap-admin.sh inventory  # seed the platform admin (needs DB_PASSWORD,
                                      #   SUPABASE_SERVICE_ROLE_KEY)
```

`start-server-migrate-and-bootstrap.sh` starts the server exactly like `start-server.sh` but forces
`MIGRATE_ON_START`/`BOOTSTRAP_ON_START` on, so a local run migrates the schemas and seeds the platform
admin even when the selected environment file carries those switches off (e.g. because its release
job runs them out of band). It warns when it overrides them.

`migrate-schema.sh` and `bootstrap-admin.sh` are the out-of-band migration/bootstrap steps of a
deployment that runs the server with `MIGRATE_ON_START=false` / `BOOTSTRAP_ON_START=false`; both are
idempotent, so they are safe to run on every rollout.

Overridable via environment: `MAIN_CLASS`, `API_BASE_URL`, `DEVICE` (`chrome` default; use
`web-server` for a plain URL), `WEB_PORT` (default `3000`), `SCHEMA_TOOL_CLASS`,
`BOOTSTRAP_TOOL_CLASS`.

## Schema management (CLI)

`SchemaTool` runs Liquibase against the shared database from the command line — create/update,
drop, or reset the `inventory` + `platform` schemas:

```bash
mvn -pl apps/inventory/server -am package
mvn -pl apps/inventory/server dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "apps/inventory/server/target/classes:$(cat apps/inventory/server/target/classpath.txt)" \
  com.chetana.keystone.inventory.SchemaTool migrate   # or: drop | reset
```

It uses the same `APP_ENV`/`DB_PASSWORD` configuration as the server (default `dev`); Supabase/OIDC
configuration is not required. `drop` and `reset` are destructive (`DROP SCHEMA … CASCADE`). The tool
always migrates — it ignores `MIGRATE_ON_START`, which is exactly what an explicit
`MIGRATE_ON_START=false` deployment runs before serving traffic; `scripts/migrate-schema.sh` wraps
`SchemaTool migrate` for that.

The bootstrap has the same treatment: `BootstrapTool` seeds the permission catalog, the
`platform-admin` role and the first platform admin user (Supabase Auth, service-role key) outside the
server, and `scripts/bootstrap-admin.sh` wraps it. It is idempotent and requires the bootstrap to be
enabled, so it fails with a clear message instead of silently doing nothing when
`BOOTSTRAP_ON_START=false`/`bootstrap.enabled: false` — the wrapper forces it on for the run.

## Container image (Cloud Run)

Each app builds its own image with Jib and deploys to its own Cloud Run service:

```bash
mvn -pl apps/inventory/server -am jib:build -Djib.to.image=<registry>/inventory
```

## Runtime dependencies

Apps persist to a shared PostgreSQL database (Supabase-managed) isolated by **schema**
(`inventory`, `platform`) — or to their own database when `database.schema` is left blank. The
choice is per-environment configuration (`database.url` + optional `database.schema`). Reads are routed via a
required `database.read.url` (configured in `application-{env}.yaml`): set it equal to
`database.url` for read/write on one instance, or to a read-replica URL. The app migrates
idempotently at startup (unless `MIGRATE_ON_START=false`), and `SchemaTool migrate` works against the
same database. Testcontainers needs Docker for the integration tests.

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
  (`application.yaml` + `application-{env}.yaml`), selected by `APP_ENV`; only secrets — plus the
  operational startup switches (`MIGRATE_ON_START`, `BOOTSTRAP_ON_START`) — are overridable by
  environment variables (see `docs/CODING_GUIDELINES_BACKEND.md` §5).

#!/usr/bin/env bash
# Apply the Liquibase changelogs of an application's schemas to the shared database.
#
# Usage: scripts/migrate-schema.sh [app]   (default app: inventory)
#
# This is the explicit migration step for a deployment that starts with the automatic migrations off
# (MIGRATE_ON_START=false / startup.migrateOnStart: false). It migrates both schemas the app owns —
# its own (inventory) and the hosted platform admin schema — and is idempotent, so it is safe to run
# on every rollout when migrations are not done at startup.
#
# Requires (like the server): DB_PASSWORD.
# Optional: APP_ENV (default dev).

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

[[ $# -le 1 ]] || die "Usage: $(basename "$0") [app]"

APP="${1:-${DEFAULT_APP}}"
SERVER_DIR="${REPO_ROOT}/apps/${APP}/server"

[[ -d "${SERVER_DIR}" ]] || die "No server found at ${SERVER_DIR}"

log "Migrating schemas for '${APP}' (APP_ENV=${APP_ENV:-dev})"

require_env DB_PASSWORD "Set it (secret): export DB_PASSWORD=..."
pin_java_home
require_command mvn
require_command java

# Maven must run from the reactor root (where the parent pom.xml lives).
cd "${REPO_ROOT}"

log "Building server (mvn package)…"
mvn -pl "apps/${APP}/server" -am package

log "Building runtime classpath…"
mvn -pl "apps/${APP}/server" dependency:build-classpath -Dmdep.outputFile=target/classpath.txt

CLASSES="${SERVER_DIR}/target/classes"
CLASSPATH_FILE="${SERVER_DIR}/target/classpath.txt"
[[ -f "${CLASSPATH_FILE}" ]] || die "Classpath not found: ${CLASSPATH_FILE}"

SCHEMA_TOOL_CLASS="${SCHEMA_TOOL_CLASS:-com.chetana.keystone.${APP}.SchemaTool}"

log "Running ${SCHEMA_TOOL_CLASS} migrate"
exec java -cp "${CLASSES}:$(cat "${CLASSPATH_FILE}")" "${SCHEMA_TOOL_CLASS}" migrate

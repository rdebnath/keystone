#!/usr/bin/env bash
# Build and run a Keystone application's Java backend service.
#
# Usage: scripts/start-server.sh [app]   (default app: inventory)
#
# Requires the same secrets the server itself needs (see README "Run an application"):
#   DB_PASSWORD                (required) — Supabase PostgreSQL password
#   SUPABASE_SERVICE_ROLE_KEY  (required) — platform admin console (login, password management, bootstrap)
# Optional: APP_ENV (default dev), BOOTSTRAP_ADMIN_PASSWORD (default changeit),
#           MIGRATE_ON_START=false (skip startup Liquibase; default true),
#           BOOTSTRAP_ON_START=false (skip the first-user seed; default true),
#           LOG_LEVEL=DEBUG (service log level; default INFO),
#           REALTIME_SERVICE_ROLE_KEY (only when Realtime is enabled).

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

[[ $# -le 1 ]] || die "Usage: $(basename "$0") [app]"

APP="${1:-${DEFAULT_APP}}"
SERVER_DIR="${REPO_ROOT}/apps/${APP}/server"

[[ -d "${SERVER_DIR}" ]] || die "No server found at ${SERVER_DIR}"

log "Starting service '${APP}'"

require_env DB_PASSWORD "Set it (secret): export DB_PASSWORD=..."
require_env SUPABASE_SERVICE_ROLE_KEY "Set it (secret): export SUPABASE_SERVICE_ROLE_KEY=..."

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

MAIN_CLASS="${MAIN_CLASS:-com.chetana.keystone.${APP}.Main}"

log "Running ${MAIN_CLASS} — http://localhost:8080/${APP} (Ctrl+C to stop)"
exec java -cp "${CLASSES}:$(cat "${CLASSPATH_FILE}")" "${MAIN_CLASS}"

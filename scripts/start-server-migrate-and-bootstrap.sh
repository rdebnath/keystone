#!/usr/bin/env bash
# Build and run a Keystone application's Java backend service with BOTH startup steps forced on: it
# migrates the schemas (Liquibase) and runs the first-user bootstrap before serving traffic.
#
# Usage: scripts/start-server-migrate-and-bootstrap.sh [app]   (default app: inventory)
#
# Same as scripts/start-server.sh, except the switches that can turn those steps off
# (MIGRATE_ON_START / BOOTSTRAP_ON_START, or startup.migrateOnStart / bootstrap.enabled in the
# environment files) are forced to true for this run — for a local dev/demo run against a deployment
# environment whose config carries them off because a release job runs the steps out of band.
#
# Requires the same secrets the server itself needs (see README "Run an application"):
#   DB_PASSWORD                (required) — Supabase PostgreSQL password
#   SUPABASE_SERVICE_ROLE_KEY  (required) — platform admin console (login, password management, bootstrap)
# Optional: APP_ENV (default dev), BOOTSTRAP_ADMIN_PASSWORD (default changeit),
#           REALTIME_SERVICE_ROLE_KEY (only when Realtime is enabled).

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

[[ $# -le 1 ]] || die "Usage: $(basename "$0") [app]"

APP="${1:-${DEFAULT_APP}}"
SERVER_DIR="${REPO_ROOT}/apps/${APP}/server"

[[ -d "${SERVER_DIR}" ]] || die "No server found at ${SERVER_DIR}"

# Both startup steps are the point of this script, whatever the environment files or the shell say.
if [[ "${MIGRATE_ON_START:-true}" == "false" || "${BOOTSTRAP_ON_START:-true}" == "false" ]]; then
    warn "MIGRATE_ON_START/BOOTSTRAP_ON_START were set to false — forcing both on for this run"
fi
export MIGRATE_ON_START=true
export BOOTSTRAP_ON_START=true

log "Starting service '${APP}' with startup migration + bootstrap forced on"

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

log "Running ${MAIN_CLASS} (MIGRATE_ON_START=true, BOOTSTRAP_ON_START=true) — http://localhost:8080/${APP} (Ctrl+C to stop)"
exec java -cp "${CLASSES}:$(cat "${CLASSPATH_FILE}")" "${MAIN_CLASS}"

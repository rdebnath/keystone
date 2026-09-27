#!/usr/bin/env bash
# Seed the platform's first-user bootstrap: the permission catalog, the `platform-admin` role (granted
# the wildcard `*` permission) and the first platform admin user, provisioned in Supabase Auth.
#
# Usage: scripts/bootstrap-admin.sh [app]   (default app: inventory)
#
# This is the explicit bootstrap step for a deployment that starts with the automatic seed off
# (BOOTSTRAP_ON_START=false / bootstrap.enabled: false) — and the way to seed deliberately. It is
# idempotent, so running it again on an already-bootstrapped database changes nothing. The schemas
# must exist first: run scripts/migrate-schema.sh when MIGRATE_ON_START=false.
#
# Requires (like the server): DB_PASSWORD, SUPABASE_SERVICE_ROLE_KEY.
# Optional: APP_ENV (default dev), BOOTSTRAP_ADMIN_PASSWORD (default changeit).
#
# BOOTSTRAP_ON_START is forced to true for this run: the script exists to seed, and BootstrapTool
# refuses to be a silent no-op when the bootstrap is disabled.

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

[[ $# -le 1 ]] || die "Usage: $(basename "$0") [app]"

APP="${1:-${DEFAULT_APP}}"
SERVER_DIR="${REPO_ROOT}/apps/${APP}/server"

[[ -d "${SERVER_DIR}" ]] || die "No server found at ${SERVER_DIR}"

export BOOTSTRAP_ON_START=true

log "Bootstrapping the platform admin for '${APP}' (APP_ENV=${APP_ENV:-dev})"

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

BOOTSTRAP_TOOL_CLASS="${BOOTSTRAP_TOOL_CLASS:-com.chetana.keystone.${APP}.BootstrapTool}"

log "Running ${BOOTSTRAP_TOOL_CLASS}"
exec java -cp "${CLASSES}:$(cat "${CLASSPATH_FILE}")" "${BOOTSTRAP_TOOL_CLASS}"

#!/usr/bin/env bash
# Start a Keystone application's Flutter web frontend (dev server with hot reload).
#
# Usage: scripts/start-web.sh [app]   (default app: inventory)
#
# Config (overridable via environment):
#   API_BASE_URL  backend URL injected into the client bundle (default http://localhost:8080/<app>)
#   DEVICE        Flutter device to run on (default chrome; use web-server for a plain URL)
#   WEB_PORT      port for web devices (default 3000)

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

[[ $# -le 1 ]] || die "Usage: $(basename "$0") [app]"

APP="${1:-${DEFAULT_APP}}"
FRONTEND_DIR="${REPO_ROOT}/apps/${APP}/frontend"

[[ -d "${FRONTEND_DIR}" ]] || die "No frontend found at ${FRONTEND_DIR}"

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/${APP}}"
DEVICE="${DEVICE:-chrome}"
WEB_PORT="${WEB_PORT:-3000}"

require_command flutter

log "Starting '${APP}' web — device=${DEVICE}, API_BASE_URL=${API_BASE_URL}"

cd "${FRONTEND_DIR}"

ARGS=(run -d "${DEVICE}")
case "${DEVICE}" in
    chrome|web-server) ARGS+=(--web-port "${WEB_PORT}") ;;
esac
ARGS+=(--dart-define="API_BASE_URL=${API_BASE_URL}")

flutter "${ARGS[@]}"

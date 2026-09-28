#!/usr/bin/env bash
# Start a Keystone application's Flutter web frontend (dev server with hot reload).
#
# Usage: scripts/start-web.sh [app]   (default app: inventory)
#
# Config (overridable via environment):
#   API_BASE_URL  backend URL injected into the client bundle (default http://localhost:8080/<app>)
#   DEVICE        Flutter device to run on (default chrome; use web-server for a plain URL)
#   MODE          build mode: debug (default, hot reload) | profile | release
#   WEB_PORT      port for web devices (default 3000)
#
# MODE=release (or profile) is for a session you leave open for a long time: the debug web build is
# the heaviest way to run the client (Dart development compiler + VM service, and a browser that stops
# reclaiming memory while DevTools is attached), so an idle debug session grows. Release serves the
# compiled bundle instead and has no hot reload.

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

[[ $# -le 1 ]] || die "Usage: $(basename "$0") [app]"

APP="${1:-${DEFAULT_APP}}"
FRONTEND_DIR="${REPO_ROOT}/apps/${APP}/frontend"

[[ -d "${FRONTEND_DIR}" ]] || die "No frontend found at ${FRONTEND_DIR}"

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/${APP}}"
DEVICE="${DEVICE:-chrome}"
MODE="${MODE:-debug}"
WEB_PORT="${WEB_PORT:-3000}"

require_command flutter

log "Starting '${APP}' web — device=${DEVICE}, mode=${MODE}, API_BASE_URL=${API_BASE_URL}"

cd "${FRONTEND_DIR}"

ARGS=(run -d "${DEVICE}")
case "${DEVICE}" in
    chrome|web-server) ARGS+=(--web-port "${WEB_PORT}") ;;
esac
case "${MODE}" in
    debug) ;;
    profile) ARGS+=(--profile) ;;
    release) ARGS+=(--release) ;;
    *) die "Unknown MODE: ${MODE} (expected debug, profile or release)" ;;
esac
ARGS+=(--dart-define="API_BASE_URL=${API_BASE_URL}")

if [[ "${MODE}" != "debug" ]]; then
    warn "MODE=${MODE}: compiled build — hot reload is unavailable, restart to pick up changes"
fi

flutter "${ARGS[@]}"

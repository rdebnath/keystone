#!/usr/bin/env bash
# Shared helpers for the Keystone dev utility scripts in this directory.
#
# This file is meant to be *sourced*, not executed directly:
#   source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

set -euo pipefail

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    printf 'common.sh is meant to be sourced, not executed.\n' >&2
    exit 1
fi

# Repository root, resolved from this file's location so scripts work from any CWD.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT

# Default application; individual scripts can override with their first argument.
DEFAULT_APP="inventory"

log()  { printf '\033[1;34m[%s]\033[0m %s\n' "$(basename "${0}")" "$*"; }
warn() { printf '\033[1;33m[%s] WARN: %s\n' "$(basename "${0}")" "$*" >&2; }
die()  { printf '\033[1;31m[%s] ERROR: %s\n' "$(basename "${0}")" "$*" >&2; exit 1; }

# Fail unless <command> is on PATH.
require_command() {
    command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

# Fail unless the named environment variable is set and non-empty.
require_env() {
    local name="$1" hint="${2:-}"
    if [[ -z "${!name:-}" ]]; then
        die "Environment variable ${name} is not set.${hint:+ $hint}"
    fi
}

# Pin JAVA_HOME to JDK 25 on macOS (the project requires Java 25 LTS). Leaves an already-set
# JAVA_HOME untouched; see README "JDK pinning".
pin_java_home() {
    if [[ "$(uname -s)" != "Darwin" ]]; then
        return 0
    fi
    if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
        JAVA_HOME="$(/usr/libexec/java_home -v 25)"
        export JAVA_HOME
        log "JAVA_HOME -> ${JAVA_HOME}"
    fi
}

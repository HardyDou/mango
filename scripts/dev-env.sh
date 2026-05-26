#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.development}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Env file not found: $ENV_FILE" >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

export VITE_HOST="${VITE_HOST:-127.0.0.1}"
export VITE_PORT="${VITE_PORT:-7777}"
export VITE_OPEN="${VITE_OPEN:-false}"
export VITE_ADMIN_PROXY_PATH="${VITE_ADMIN_PROXY_PATH:-${MANGO_BACKEND_URL:-http://127.0.0.1:${MANGO_BACKEND_PORT:-5555}}}"

case "${2:-all}" in
  backend)
    cd "$ROOT_DIR/mango"
    exec mvn -pl :mango-monolith-app -am spring-boot:run
    ;;
  frontend)
    cd "$ROOT_DIR/mango-ui"
    exec pnpm -F mango-admin dev -- --host "$VITE_HOST" --port "$VITE_PORT"
    ;;
  all)
    "$0" "$ENV_FILE" backend &
    BACKEND_PID=$!
    "$0" "$ENV_FILE" frontend &
    FRONTEND_PID=$!
    trap 'kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true' INT TERM EXIT
    wait
    ;;
  *)
    echo "Usage: scripts/dev-env.sh [env-file] [backend|frontend|all]" >&2
    exit 1
    ;;
esac

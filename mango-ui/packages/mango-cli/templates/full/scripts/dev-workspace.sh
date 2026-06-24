#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
LOCAL_DIR="${REPO_ROOT}/.mango"
ENV_FILE="${LOCAL_DIR}/dev-workspace.env"
FRONTEND_ROOT="${REPO_ROOT}/frontend"

usage() {
  cat <<'EOF'
Usage: scripts/dev-workspace.sh <command>

Commands:
  init       Create .mango/dev-workspace.env if it does not exist
  print      Print current workspace configuration
  backend    Start backend app from mango.dev.json
  frontend   Start frontend app from mango.dev.json
  start      Start the default app group from mango.dev.json
  stop       Stop apps recorded in .mango/run/pids
  status     Show app process status
  logs       Print app logs, for example: scripts/dev-workspace.sh logs app-name
  doctor     Check manifest, commands and port availability
  validate   Validate mango.dev.json
  plan       Print resolved startup plan

The actual runner lives in the mango CLI. This shell file is a compatibility entry.
EOF
}

run_mango() {
  if [[ -x "${FRONTEND_ROOT}/node_modules/.bin/mango" ]] && command -v pnpm >/dev/null 2>&1; then
    cd "${FRONTEND_ROOT}"
    exec pnpm exec mango "$@"
  fi

  if command -v mango >/dev/null 2>&1; then
    exec mango "$@"
  fi

  echo "mango CLI not found in project frontend dependencies or globally."
  echo "Install project dependencies: cd frontend && pnpm install"
  echo "Or install globally: npm install -g @mango/cli@{{mangoCliVersion}} --registry {{npmRegistry}}"
  exit 1
}

command="${1:-start}"
case "${command}" in
  init)
    run_mango init-dev
    ;;
  init-dev)
    run_mango init-dev
    ;;
  print|backend|frontend|start|stop|status|logs|doctor|validate|plan)
    shift || true
    run_mango "${command}" "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac

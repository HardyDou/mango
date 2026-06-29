#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
FRONTEND_ROOT="${ROOT_DIR}/frontend"

usage() {
  cat <<'EOF'
Usage: scripts/dev-workspace.sh <command>

Deprecated compatibility entry. Use Mango CLI directly:
  mango workspace init
  mango dev start
  mango dev stop
  mango dev status
  mango dev doctor
  mango frontend prepare
EOF
}

warn_deprecated() {
  echo "scripts/dev-workspace.sh is deprecated. Use Mango CLI workspace/dev commands." >&2
}

run_mango() {
  if [[ -x "${FRONTEND_ROOT}/node_modules/.bin/mango" ]] && command -v pnpm >/dev/null 2>&1; then
    cd "${FRONTEND_ROOT}"
    exec pnpm exec mango "$@"
  fi
  if command -v mango >/dev/null 2>&1; then
    exec mango "$@"
  fi
  echo "mango CLI not found in project frontend dependencies or globally." >&2
  echo "Install project dependencies: cd frontend && pnpm install" >&2
  echo "Or install globally: npm install -g @mango/cli@1.0.51 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/" >&2
  exit 1
}

command="${1:-start}"
case "${command}" in
  init|init-dev)
    warn_deprecated
    shift || true
    run_mango workspace init "$@"
    ;;
  print)
    warn_deprecated
    shift || true
    run_mango workspace status "$@"
    ;;
  validate)
    warn_deprecated
    shift || true
    run_mango workspace doctor "$@"
    ;;
  doctor)
    warn_deprecated
    shift || true
    run_mango dev doctor "$@"
    ;;
  plan)
    warn_deprecated
    shift || true
    run_mango workspace status "$@"
    ;;
  backend)
    warn_deprecated
    shift || true
    run_mango dev start backend "$@"
    ;;
  frontend)
    warn_deprecated
    shift || true
    run_mango dev start frontend "$@"
    ;;
  start|stop|status|logs)
    warn_deprecated
    shift || true
    run_mango dev "${command}" "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac

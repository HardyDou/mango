#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
CLI_ENTRY="${REPO_ROOT}/mango-ui/packages/mango-cli/src/index.mjs"

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

Compatibility commands:
  init, print, validate, doctor, plan, start, backend, frontend, stop, status, logs
  install-hooks
  worktree-remove <path> [--drop-db] [--force]
EOF
}

warn_deprecated() {
  echo "scripts/dev-workspace.sh is deprecated. Use Mango CLI workspace/dev commands." >&2
}

run_mango() {
  if [[ -f "${CLI_ENTRY}" ]]; then
    exec node "${CLI_ENTRY}" "$@"
  fi
  if command -v mango >/dev/null 2>&1; then
    exec mango "$@"
  fi
  echo "mango CLI not found. Install @mango/cli or run from a Mango source checkout." >&2
  exit 1
}

install_hooks() {
  cd "${REPO_ROOT}"
  git config core.hooksPath .githooks
  echo "Configured Git hooksPath=.githooks"
  echo "New git worktree add checkouts should run: mango workspace init"
}

load_env_for_drop() {
  local env_file="$1"
  if [[ ! -f "${env_file}" ]]; then
    echo "Workspace env not found: ${env_file}" >&2
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  . "${env_file}"
  set +a
}

drop_workspace_db() {
  local target_root="$1"
  load_env_for_drop "${target_root}/.mango/dev-workspace.env"
  case "${MANGO_DB_NAME:-}" in
    mango_dev_*)
      ;;
    *)
      echo "Refuse to drop non-workspace database: ${MANGO_DB_NAME:-<empty>}" >&2
      exit 1
      ;;
  esac
  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found; cannot drop ${MANGO_DB_NAME}." >&2
    exit 1
  fi
  local mysql_args
  mysql_args=(--protocol=TCP -h "${MANGO_DB_HOST:-127.0.0.1}" -P "${MANGO_DB_PORT:-3306}" -u "${MANGO_DB_USERNAME:-root}")
  if [[ -n "${MANGO_DB_PASSWORD:-}" ]]; then
    MYSQL_PWD="${MANGO_DB_PASSWORD}" mysql "${mysql_args[@]}" -e "DROP DATABASE IF EXISTS \`${MANGO_DB_NAME}\`;"
  else
    mysql "${mysql_args[@]}" -e "DROP DATABASE IF EXISTS \`${MANGO_DB_NAME}\`;"
  fi
  echo "Dropped database: ${MANGO_DB_NAME}"
}

remove_worktree() {
  local target="${1:-}"
  if [[ -z "${target}" ]]; then
    usage
    exit 1
  fi
  shift || true

  local drop_db=false
  local force=false
  while (($# > 0)); do
    case "$1" in
      --drop-db)
        drop_db=true
        ;;
      --force|-f)
        force=true
        ;;
      *)
        echo "Unknown option for worktree-remove: $1" >&2
        usage
        exit 1
        ;;
    esac
    shift
  done

  local target_root
  target_root="$(cd "${target}" && pwd -P)"
  (cd "${target_root}" && run_mango dev stop || true)
  if [[ "${drop_db}" == "true" ]]; then
    drop_workspace_db "${target_root}"
  else
    echo "Database kept. Pass --drop-db to delete the workspace database."
  fi

  local remove_args
  remove_args=(worktree remove)
  if [[ "${force}" == "true" ]]; then
    remove_args+=(--force)
  fi
  remove_args+=("${target_root}")
  git -C "${REPO_ROOT}" "${remove_args[@]}"
  run_mango workspace release --workspace "${target_root}"
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
    run_mango validate "$@"
    ;;
  doctor)
    warn_deprecated
    shift || true
    run_mango dev doctor "$@"
    ;;
  plan)
    warn_deprecated
    shift || true
    run_mango dev plan "$@"
    ;;
  backend)
    warn_deprecated
    shift || true
    run_mango dev backend "$@"
    ;;
  frontend)
    warn_deprecated
    shift || true
    run_mango dev frontend "$@"
    ;;
  start|stop|status|logs)
    warn_deprecated
    shift || true
    run_mango dev "${command}" "$@"
    ;;
  install-hooks)
    install_hooks
    ;;
  worktree-remove|remove-worktree)
    warn_deprecated
    shift || true
    remove_worktree "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
LOCAL_DIR="${REPO_ROOT}/.mango"
ENV_FILE="${LOCAL_DIR}/dev-workspace.env"
FRONTEND_ROOT="${REPO_ROOT}/frontend"
DEFAULT_DB_NAME="{{projectKebabSnake}}"

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

generate_sm4_key() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
    return
  fi
  cksum <<<"${REPO_ROOT}:$(date +%s):$$" | awk '{printf "%032x\n", $1}'
}

ensure_sm4_key_env() {
  if [[ -f "${ENV_FILE}" ]] && ! grep -q '^MANGO_CRYPTO_SM4_SECRET_KEY=' "${ENV_FILE}"; then
    printf '\nMANGO_CRYPTO_SM4_SECRET_KEY=%s\n' "$(generate_sm4_key)" >>"${ENV_FILE}"
    echo "Added MANGO_CRYPTO_SM4_SECRET_KEY to existing workspace env: ${ENV_FILE}"
  fi
}

write_default_env() {
  mkdir -p "${LOCAL_DIR}"
  if [[ -f "${ENV_FILE}" ]]; then
    ensure_sm4_key_env
    echo "Workspace env already exists: ${ENV_FILE}"
    return
  fi

  cat >"${ENV_FILE}" <<EOF
# Mango business project local workspace configuration.
# This file is generated once per workspace and must not be committed.
MANGO_CRYPTO_SM4_SECRET_KEY=$(generate_sm4_key)
MANGO_BACKEND_PORT=5555
MANGO_FRONTEND_PORT=5176
MANGO_FRONTEND_HOST=127.0.0.1
MANGO_FRONTEND_OPEN=false
MANGO_FRONTEND_AUTO_INSTALL=true
MANGO_DB_HOST=127.0.0.1
MANGO_DB_PORT=3306
MANGO_DB_NAME=${DEFAULT_DB_NAME}
MANGO_DB_USERNAME=root
MANGO_DB_PASSWORD=''
MANGO_DB_AUTO_CREATE=true
MANGO_OFFICE_PLUGIN_ENABLED=false
MANGO_BACKEND_ADDITIONAL_ARGS=''
EOF

  echo "Created workspace env: ${ENV_FILE}"
}

run_mango() {
  if command -v mango >/dev/null 2>&1; then
    exec mango "$@"
  fi

  if [[ -f "${FRONTEND_ROOT}/package.json" ]] && command -v pnpm >/dev/null 2>&1; then
    cd "${FRONTEND_ROOT}"
    exec pnpm exec mango "$@"
  fi

  echo "mango CLI not found globally or in project frontend dependencies."
  echo "Install project dependencies: cd frontend && pnpm install"
  echo "Or install globally: npm install -g @mango/cli@{{mangoCliVersion}} --registry {{npmRegistry}}"
  exit 1
}

command="${1:-start}"
case "${command}" in
  init)
    write_default_env
    run_mango print
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

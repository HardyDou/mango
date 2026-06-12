#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
LOCAL_DIR="${REPO_ROOT}/.mango"
ENV_FILE="${LOCAL_DIR}/dev-workspace.env"
LOG_DIR="${LOCAL_DIR}/logs"
REGISTRY_FILE="${MANGO_WORKSPACE_REGISTRY:-${HOME:-${LOCAL_DIR}}/.mango/workspaces.tsv}"
BACKEND_ROOT="${REPO_ROOT}/mango"
FRONTEND_ROOT="${REPO_ROOT}/mango-ui"
START_BACKEND_PID=""

usage() {
  cat <<'EOF'
Usage: scripts/dev-workspace.sh <command>

Commands:
  init       Create .mango/dev-workspace.env if it does not exist
  install-hooks
             Configure Git hooks so new worktrees auto-run init
  print      Delegate to mango print
  backend    Delegate to mango backend
  frontend   Delegate to mango frontend
  start      Delegate to mango start
  stop       Delegate to mango stop
  status     Delegate to mango status
  logs       Delegate to mango logs <app>
  doctor     Delegate to mango doctor
  validate   Delegate to mango validate
  plan       Delegate to mango plan
  worktree-remove <path> [--drop-db] [--force]
             Stop a worktree's services, optionally drop its local DB, then remove it

The script keeps each workspace stable by reusing .mango/dev-workspace.env.
EOF
}

hash_seed() {
  cksum <<<"${REPO_ROOT}" | awk '{print $1}'
}

registered_workspace() {
  [[ -f "${REGISTRY_FILE}" ]] || return 1
  awk -F '\t' -v root="${REPO_ROOT}" '$1 == root { print $2 "\t" $3 "\t" $4; found = 1; exit } END { exit found ? 0 : 1 }' "${REGISTRY_FILE}"
}

candidate_registered() {
  local backend_port="$1"
  local frontend_port="$2"
  local db_name="$3"
  [[ -f "${REGISTRY_FILE}" ]] || return 1
  awk -F '\t' \
    -v root="${REPO_ROOT}" \
    -v backend="${backend_port}" \
    -v frontend="${frontend_port}" \
    -v db="${db_name}" \
    '$1 != root && ($2 == backend || $3 == frontend || $4 == db) { found = 1; exit } END { exit found ? 0 : 1 }' \
    "${REGISTRY_FILE}"
}

database_exists() {
  local db_name="$1"
  if ! command -v mysql >/dev/null 2>&1; then
    return 1
  fi

  local db_host="${MANGO_DB_HOST:-127.0.0.1}"
  local db_port="${MANGO_DB_PORT:-3306}"
  local db_username="${MANGO_DB_USERNAME:-root}"
  local db_password="${MANGO_DB_PASSWORD:-}"
  local query="SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${db_name}' LIMIT 1;"
  local mysql_args
  mysql_args=(--batch --skip-column-names --protocol=TCP -h "${db_host}" -P "${db_port}" -u "${db_username}")

  local result
  if [[ -n "${db_password}" ]]; then
    result="$(MYSQL_PWD="${db_password}" mysql "${mysql_args[@]}" -e "${query}" 2>/dev/null || true)"
  else
    result="$(mysql "${mysql_args[@]}" -e "${query}" 2>/dev/null || true)"
  fi
  [[ "${result}" == "${db_name}" ]]
}

register_workspace() {
  local backend_port="$1"
  local frontend_port="$2"
  local db_name="$3"
  mkdir -p "$(dirname "${REGISTRY_FILE}")"
  printf '%s\t%s\t%s\t%s\n' "${REPO_ROOT}" "${backend_port}" "${frontend_port}" "${db_name}" >>"${REGISTRY_FILE}"
}

unregister_workspace() {
  local root="$1"
  [[ -f "${REGISTRY_FILE}" ]] || return
  local tmp_file
  tmp_file="$(mktemp)"
  awk -F '\t' -v root="${root}" '$1 != root' "${REGISTRY_FILE}" >"${tmp_file}"
  mv "${tmp_file}" "${REGISTRY_FILE}"
}

write_default_env() {
  mkdir -p "${LOCAL_DIR}" "${LOG_DIR}"
  if [[ -f "${ENV_FILE}" ]]; then
    echo "Workspace env already exists: ${ENV_FILE}"
    return
  fi

  local seed suffix backend_port frontend_port db_suffix db_name registered
  seed="$(hash_seed)"
  if registered="$(registered_workspace)"; then
    IFS=$'\t' read -r backend_port frontend_port db_name <<<"${registered}"
    db_suffix="${db_name#mango_dev_}"
  else
    local offset selected
    selected=false
    for offset in $(seq 0 999); do
      suffix=$(((seed + offset) % 1000))
      backend_port=$((18080 + suffix))
      frontend_port=$((7770 + suffix))
      db_suffix="$(printf "%06x" $(((seed + offset * 7919) % 16777215)))"
      db_name="mango_dev_${db_suffix}"
      if ! candidate_registered "${backend_port}" "${frontend_port}" "${db_name}" \
        && ! database_exists "${db_name}" \
        && ! port_in_use "${backend_port}" \
        && ! port_in_use "${frontend_port}"; then
        register_workspace "${backend_port}" "${frontend_port}" "${db_name}"
        selected=true
        break
      fi
    done
    if [[ "${selected}" != "true" ]]; then
      echo "No available workspace port pair found. Edit ${REGISTRY_FILE} or override .mango/dev-workspace.env manually."
      exit 1
    fi
  fi

  cat >"${ENV_FILE}" <<EOF
# Mango local workspace configuration.
# This file is generated once per workspace and must not be committed.
MANGO_WORKSPACE_ID=mango_${db_suffix}
MANGO_BACKEND_PORT=${backend_port}
MANGO_FRONTEND_PORT=${frontend_port}
MANGO_FRONTEND_HOST=127.0.0.1
MANGO_FRONTEND_OPEN=false
MANGO_FRONTEND_AUTO_INSTALL=true
MANGO_FRONTEND_MODE=source
MANGO_DB_HOST=127.0.0.1
MANGO_DB_PORT=3306
MANGO_DB_NAME=${db_name}
MANGO_DB_USERNAME=root
MANGO_DB_PASSWORD=''
MANGO_DB_AUTO_CREATE=true
MANGO_OFFICE_PLUGIN_ENABLED=false
MANGO_BACKEND_ADDITIONAL_ARGS=''
EOF

  echo "Created workspace env: ${ENV_FILE}"
}

load_workspace_env() {
  if [[ ! -f "${ENV_FILE}" ]]; then
    write_default_env
  fi
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a

  : "${MANGO_BACKEND_PORT:?Missing MANGO_BACKEND_PORT in ${ENV_FILE}}"
  : "${MANGO_FRONTEND_PORT:?Missing MANGO_FRONTEND_PORT in ${ENV_FILE}}"
  : "${MANGO_FRONTEND_HOST:=127.0.0.1}"
  : "${MANGO_FRONTEND_OPEN:=false}"
  : "${MANGO_FRONTEND_AUTO_INSTALL:=true}"
  : "${MANGO_FRONTEND_MODE:=source}"
  : "${MANGO_DB_HOST:?Missing MANGO_DB_HOST in ${ENV_FILE}}"
  : "${MANGO_DB_PORT:?Missing MANGO_DB_PORT in ${ENV_FILE}}"
  : "${MANGO_DB_NAME:?Missing MANGO_DB_NAME in ${ENV_FILE}}"
  : "${MANGO_DB_USERNAME:?Missing MANGO_DB_USERNAME in ${ENV_FILE}}"
  : "${MANGO_DB_PASSWORD:=}"
  : "${MANGO_DB_AUTO_CREATE:=true}"
  : "${MANGO_OFFICE_PLUGIN_ENABLED:=false}"
  : "${MANGO_BACKEND_ADDITIONAL_ARGS:=}"
}

print_config() {
  load_workspace_env
  echo "Workspace: ${REPO_ROOT}"
  echo "Env file:  ${ENV_FILE}"
  echo "Backend:   http://127.0.0.1:${MANGO_BACKEND_PORT}"
  echo "Frontend:  http://${MANGO_FRONTEND_HOST}:${MANGO_FRONTEND_PORT}"
  echo "Frontend mode: ${MANGO_FRONTEND_MODE}"
  echo "Database:  ${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_DB_NAME}"
}

install_hooks() {
  cd "${REPO_ROOT}"
  git config core.hooksPath .githooks
  echo "Configured Git hooksPath=.githooks"
  echo "New git worktree add checkouts will initialize .mango/dev-workspace.env."
}

port_in_use() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
    return
  fi
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "${port}" >/dev/null 2>&1
    return
  fi
  return 1
}

require_port_free() {
  local name="$1"
  local port="$2"
  if port_in_use "${port}"; then
    echo "${name} port ${port} is already in use."
    echo "Edit ${ENV_FILE} and choose a free ${name} port, then retry."
    exit 1
  fi
}

validate_db_name() {
  case "${MANGO_DB_NAME}" in
    ''|*[!A-Za-z0-9_]*)
      echo "Invalid MANGO_DB_NAME: ${MANGO_DB_NAME}"
      echo "Only letters, numbers and underscore are allowed."
      exit 1
      ;;
  esac
}

create_database_if_enabled() {
  load_workspace_env
  validate_db_name
  if [[ "${MANGO_DB_AUTO_CREATE}" != "true" ]]; then
    return
  fi
  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found; skip database auto-create for ${MANGO_DB_NAME}."
    return
  fi

  local sql="CREATE DATABASE IF NOT EXISTS \`${MANGO_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  mysql_exec "${sql}"
}

mysql_exec() {
  local sql="$1"
  local mysql_args
  mysql_args=(--protocol=TCP -h "${MANGO_DB_HOST}" -P "${MANGO_DB_PORT}" -u "${MANGO_DB_USERNAME}")
  if [[ -n "${MANGO_DB_PASSWORD}" ]]; then
    MYSQL_PWD="${MANGO_DB_PASSWORD}" mysql "${mysql_args[@]}" -e "${sql}"
  else
    mysql "${mysql_args[@]}" -e "${sql}"
  fi
}

drop_database() {
  validate_db_name
  case "${MANGO_DB_NAME}" in
    mango_dev_*)
      ;;
    *)
      echo "Refuse to drop non-workspace database: ${MANGO_DB_NAME}"
      echo "Only mango_dev_* databases can be dropped by this script."
      exit 1
      ;;
  esac

  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found; cannot drop ${MANGO_DB_NAME}."
    exit 1
  fi

  mysql_exec "DROP DATABASE IF EXISTS \`${MANGO_DB_NAME}\`;"
  echo "Dropped database: ${MANGO_DB_NAME}"
}

backend_arguments() {
  local db_url
  db_url="jdbc:mysql://${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
  printf '%s %s %s %s' \
    "--server.port=${MANGO_BACKEND_PORT}" \
    "--spring.datasource.url=${db_url}" \
    "--spring.datasource.username=${MANGO_DB_USERNAME}" \
    "--spring.datasource.password=${MANGO_DB_PASSWORD} --office.plugin.enabled=${MANGO_OFFICE_PLUGIN_ENABLED}"
  if [[ -n "${MANGO_BACKEND_ADDITIONAL_ARGS}" ]]; then
    printf ' %s' "${MANGO_BACKEND_ADDITIONAL_ARGS}"
  fi
}

run_backend() {
  load_workspace_env
  require_port_free "backend" "${MANGO_BACKEND_PORT}"
  create_database_if_enabled
  echo "Starting backend on http://127.0.0.1:${MANGO_BACKEND_PORT}"
  echo "Using database ${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_DB_NAME}"
  cd "${BACKEND_ROOT}"
  mvn -pl :mango-monolith-app -am org.springframework.boot:spring-boot-maven-plugin:3.5.14:run \
    "-Dspring-boot.run.arguments=$(backend_arguments)"
}

ensure_frontend_dependencies() {
  if [[ -x "${FRONTEND_ROOT}/node_modules/.bin/vite" ]]; then
    return
  fi

  if [[ "${MANGO_FRONTEND_AUTO_INSTALL}" != "true" ]]; then
    echo "Frontend dependencies are missing: ${FRONTEND_ROOT}/node_modules"
    echo "Run 'pnpm install' under ${FRONTEND_ROOT}, or set MANGO_FRONTEND_AUTO_INSTALL=true."
    exit 1
  fi

  if ! command -v pnpm >/dev/null 2>&1; then
    echo "pnpm not found; cannot install frontend dependencies."
    exit 1
  fi

  echo "Frontend dependencies are missing; running pnpm install --frozen-lockfile."
  cd "${FRONTEND_ROOT}"
  pnpm install --frozen-lockfile
}

validate_frontend_mode() {
  case "${MANGO_FRONTEND_MODE}" in
    source|package)
      ;;
    *)
      echo "Invalid MANGO_FRONTEND_MODE: ${MANGO_FRONTEND_MODE}"
      echo "Use 'source' for Mango framework development or 'package' for package-consumption validation."
      exit 1
      ;;
  esac
}

require_package_artifact() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "Missing package-mode artifact: ${path}"
    echo "MANGO_FRONTEND_MODE=package validates published package outputs; build packages first or use MANGO_FRONTEND_MODE=source for framework development."
    exit 1
  fi
}

require_frontend_package_artifacts() {
  if [[ "${MANGO_FRONTEND_MODE}" != "package" ]]; then
    return
  fi

  local package
  for package in admin admin-shell admin-pages auth rbac system calendar file notice numgen template workflow workflow-business-example common; do
    require_package_artifact "${FRONTEND_ROOT}/packages/${package}/dist/index.js"
  done

  for package in common auth rbac system calendar file notice numgen template workflow workflow-business-example; do
    require_package_artifact "${FRONTEND_ROOT}/packages/${package}/dist/style.css"
  done
}

run_frontend() {
  load_workspace_env
  validate_frontend_mode
  require_port_free "frontend" "${MANGO_FRONTEND_PORT}"
  ensure_frontend_dependencies
  require_frontend_package_artifacts
  echo "Starting frontend on http://${MANGO_FRONTEND_HOST}:${MANGO_FRONTEND_PORT}"
  echo "Proxy target http://127.0.0.1:${MANGO_BACKEND_PORT}"
  echo "Frontend mode ${MANGO_FRONTEND_MODE}"
  cd "${FRONTEND_ROOT}"
  MANGO_FRONTEND_MODE="${MANGO_FRONTEND_MODE}" \
    VITE_ADMIN_PROXY_PATH="http://127.0.0.1:${MANGO_BACKEND_PORT}" \
    VITE_PORT="${MANGO_FRONTEND_PORT}" \
    VITE_HOST="${MANGO_FRONTEND_HOST}" \
    VITE_OPEN="${MANGO_FRONTEND_OPEN}" \
    pnpm -F mango-admin exec vite --force --host "${MANGO_FRONTEND_HOST}" --port "${MANGO_FRONTEND_PORT}"
}

stop_port() {
  local name="$1"
  local port="$2"
  if ! command -v lsof >/dev/null 2>&1; then
    echo "lsof not found; cannot stop ${name} on port ${port}."
    exit 1
  fi

  local pids
  pids="$(lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null | sort -u || true)"
  if [[ -z "${pids}" ]]; then
    echo "No ${name} process listening on port ${port}."
    return
  fi

  echo "Stopping ${name} process(es) on port ${port}: ${pids//$'\n'/ }"
  # shellcheck disable=SC2086
  kill ${pids} >/dev/null 2>&1 || true
  sleep 2

  local alive=()
  local pid
  for pid in ${pids}; do
    if kill -0 "${pid}" >/dev/null 2>&1; then
      alive+=("${pid}")
    fi
  done
  if (( ${#alive[@]} > 0 )); then
    echo "Force stopping ${name} process(es): ${alive[*]}"
    kill -9 "${alive[@]}" >/dev/null 2>&1 || true
  fi
}

stop_workspace() {
  load_workspace_env
  stop_port "frontend" "${MANGO_FRONTEND_PORT}"
  stop_port "backend" "${MANGO_BACKEND_PORT}"
}

wait_for_backend() {
  local pid="$1"
  local log_file="$2"
  local deadline=$((SECONDS + 120))
  while (( SECONDS < deadline )); do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      echo "Backend exited before it became ready. Last log lines:"
      tail -80 "${log_file}" || true
      wait "${pid}" || exit $?
      exit 1
    fi
    if command -v curl >/dev/null 2>&1; then
      if curl -fsS "http://127.0.0.1:${MANGO_BACKEND_PORT}/actuator/health" >/dev/null 2>&1; then
        echo "Backend is ready: http://127.0.0.1:${MANGO_BACKEND_PORT}"
        return
      fi
    elif port_in_use "${MANGO_BACKEND_PORT}"; then
      echo "Backend port is listening: ${MANGO_BACKEND_PORT}"
      return
    fi
    sleep 2
  done

  echo "Timed out waiting for backend. Last log lines:"
  tail -120 "${log_file}" || true
  exit 1
}

start_all() {
  load_workspace_env
  require_port_free "backend" "${MANGO_BACKEND_PORT}"
  require_port_free "frontend" "${MANGO_FRONTEND_PORT}"
  mkdir -p "${LOG_DIR}"

  local backend_log backend_pid
  backend_log="${LOG_DIR}/backend.log"
  echo "Backend log: ${backend_log}"
  (
    run_backend
  ) >"${backend_log}" 2>&1 &
  backend_pid=$!
  START_BACKEND_PID="${backend_pid}"

  cleanup() {
    if [[ -n "${START_BACKEND_PID:-}" ]] && kill -0 "${START_BACKEND_PID}" >/dev/null 2>&1; then
      kill "${START_BACKEND_PID}" >/dev/null 2>&1 || true
      wait "${START_BACKEND_PID}" >/dev/null 2>&1 || true
    fi
  }
  trap cleanup EXIT INT TERM

  wait_for_backend "${backend_pid}" "${backend_log}"
  run_frontend
}

load_target_workspace_env() {
  local target_root="$1"
  local target_env="${target_root}/.mango/dev-workspace.env"
  if [[ ! -f "${target_env}" ]]; then
    echo "Target workspace env not found: ${target_env}"
    echo "Run init in that worktree first, or remove without cleanup manually."
    exit 1
  fi

  set -a
  # shellcheck disable=SC1090
  . "${target_env}"
  set +a

  : "${MANGO_BACKEND_PORT:?Missing MANGO_BACKEND_PORT in ${target_env}}"
  : "${MANGO_FRONTEND_PORT:?Missing MANGO_FRONTEND_PORT in ${target_env}}"
  : "${MANGO_DB_HOST:?Missing MANGO_DB_HOST in ${target_env}}"
  : "${MANGO_DB_PORT:?Missing MANGO_DB_PORT in ${target_env}}"
  : "${MANGO_DB_NAME:?Missing MANGO_DB_NAME in ${target_env}}"
  : "${MANGO_DB_USERNAME:?Missing MANGO_DB_USERNAME in ${target_env}}"
  : "${MANGO_DB_PASSWORD:=}"
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
        echo "Unknown option for worktree-remove: $1"
        usage
        exit 1
        ;;
    esac
    shift
  done

  local target_root
  target_root="$(cd "${target}" && pwd -P)"
  load_target_workspace_env "${target_root}"
  stop_port "frontend" "${MANGO_FRONTEND_PORT}"
  stop_port "backend" "${MANGO_BACKEND_PORT}"

  if [[ "${drop_db}" == "true" ]]; then
    drop_database
  else
    echo "Database kept: ${MANGO_DB_NAME}"
    echo "Pass --drop-db to delete it."
  fi

  cd "${REPO_ROOT}"
  local remove_args
  remove_args=(worktree remove)
  if [[ "${force}" == "true" ]]; then
    remove_args+=(--force)
  fi
  remove_args+=("${target_root}")
  git "${remove_args[@]}"
  unregister_workspace "${target_root}"
  echo "Removed worktree: ${target_root}"
}

run_mango() {
  local repo_cli="${REPO_ROOT}/mango-ui/packages/mango-cli/src/index.mjs"
  if [[ -f "${repo_cli}" ]]; then
    if ! command -v node >/dev/null 2>&1; then
      echo "node not found; cannot run repository mango CLI: ${repo_cli}"
      exit 1
    fi
    exec node "${repo_cli}" "$@"
  fi

  if command -v mango >/dev/null 2>&1; then
    exec mango "$@"
  fi

  echo "mango CLI not found."
  echo "Install @mango/cli globally or run from a Mango source checkout that contains mango-ui/packages/mango-cli/src/index.mjs."
  echo "Example: npm install -g @mango/cli"
  exit 1
}

command="${1:-start}"
case "${command}" in
  init)
    write_default_env
    print_config
    ;;
  install-hooks)
    install_hooks
    ;;
  print)
    shift || true
    run_mango print "$@"
    ;;
  backend|frontend|start|stop|status|logs|doctor|validate|plan)
    shift || true
    run_mango "${command}" "$@"
    ;;
  worktree-remove|remove-worktree)
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

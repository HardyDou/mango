#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
LOCAL_DIR="${REPO_ROOT}/.mango"
ENV_FILE="${LOCAL_DIR}/dev-workspace.env"
BACKEND_ROOT="${REPO_ROOT}/backend"
APP_POM="${BACKEND_ROOT}/app/pom.xml"
FRONTEND_ROOT="${REPO_ROOT}/frontend"
LOG_DIR="${LOCAL_DIR}/logs"
START_BACKEND_PID=""
SPRING_BOOT_PLUGIN="org.springframework.boot:spring-boot-maven-plugin:{{springBootVersion}}:run"
DEFAULT_DB_NAME="{{projectKebabSnake}}"

usage() {
  cat <<'EOF'
Usage: scripts/dev-workspace.sh <command>

Commands:
  init       Create .mango/dev-workspace.env if it does not exist
  print      Print current workspace configuration
  backend    Start backend using the explicit Spring Boot Maven plugin goal
  frontend   Start frontend and proxy /api to the configured backend port
  start      Start backend, wait until ready, then start frontend
  stop       Stop backend/frontend processes listening on workspace ports

Use this script as the only local development startup entry.
EOF
}

write_default_env() {
  mkdir -p "${LOCAL_DIR}"
  if [[ -f "${ENV_FILE}" ]]; then
    echo "Workspace env already exists: ${ENV_FILE}"
    return
  fi

  cat >"${ENV_FILE}" <<EOF
# Mango business project local workspace configuration.
# This file is generated once per workspace and must not be committed.
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

load_workspace_env() {
  if [[ ! -f "${ENV_FILE}" ]]; then
    write_default_env
  fi
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a

  : "${MANGO_BACKEND_PORT:?Missing MANGO_BACKEND_PORT in ${ENV_FILE}}"
  : "${MANGO_FRONTEND_PORT:=5176}"
  : "${MANGO_FRONTEND_HOST:=127.0.0.1}"
  : "${MANGO_FRONTEND_OPEN:=false}"
  : "${MANGO_FRONTEND_AUTO_INSTALL:=true}"
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
  echo "Database:  ${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_DB_NAME}"
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

create_database_if_enabled() {
  validate_db_name
  if [[ "${MANGO_DB_AUTO_CREATE}" != "true" ]]; then
    return
  fi
  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found; skip database auto-create for ${MANGO_DB_NAME}."
    return
  fi

  mysql_exec "CREATE DATABASE IF NOT EXISTS \`${MANGO_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
}

backend_arguments() {
  local db_url
  db_url="jdbc:mysql://${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
  printf '%s %s %s %s %s' \
    "--server.port=${MANGO_BACKEND_PORT}" \
    "--spring.datasource.url=${db_url}" \
    "--spring.datasource.username=${MANGO_DB_USERNAME}" \
    "--spring.datasource.password=${MANGO_DB_PASSWORD}" \
    "--office.plugin.enabled=${MANGO_OFFICE_PLUGIN_ENABLED}"
  if [[ -n "${MANGO_BACKEND_ADDITIONAL_ARGS}" ]]; then
    printf ' %s' "${MANGO_BACKEND_ADDITIONAL_ARGS}"
  fi
}

diagnose_backend_failure() {
  local exit_code="$1"
  local output="$2"
  echo
  echo "Backend startup failed with exit code ${exit_code}."
  echo "Diagnostics:"
  if grep -Eiq "Address already in use|Port .* already in use|Web server failed to start" "${output}"; then
    echo "- Port ${MANGO_BACKEND_PORT} is occupied. Run 'scripts/dev-workspace.sh stop' or edit ${ENV_FILE}."
  fi
  if grep -Eiq "Communications link failure|Connection refused|Access denied for user|Unknown database|Could not create connection" "${output}"; then
    echo "- Database connection failed. Check MANGO_DB_HOST, MANGO_DB_PORT, MANGO_DB_NAME, MANGO_DB_USERNAME and MANGO_DB_PASSWORD in ${ENV_FILE}."
  fi
  if grep -Eiq "Flyway|Migration|Validate failed|Schema.*version" "${output}"; then
    echo "- Flyway migration failed. Check backend/app configuration and module migration files."
  fi
  if grep -Eiq "Unable to find a suitable main class|mainClass|ClassNotFoundException.*Application" "${output}"; then
    echo "- Spring Boot mainClass was not resolved. Check backend/app/pom.xml and the generated Application class."
  fi
  if grep -Eiq "No plugin found for prefix 'spring-boot'|Unknown lifecycle phase|Could not find goal" "${output}"; then
    echo "- Maven Spring Boot goal resolution failed. This script uses ${SPRING_BOOT_PLUGIN}; check Maven repository access and backend/app/pom.xml."
  fi
  if grep -Eiq "Could not resolve dependencies|Could not find artifact|Non-resolvable parent POM" "${output}"; then
    echo "- Maven dependency resolution failed. The script installs backend reactor modules first; check Maven repository settings and generated module coordinates."
  fi
  echo "- Re-run with 'scripts/dev-workspace.sh print' to confirm the active workspace configuration."
}

run_backend() {
  load_workspace_env
  require_port_free "backend" "${MANGO_BACKEND_PORT}"
  create_database_if_enabled
  echo "Starting backend on http://127.0.0.1:${MANGO_BACKEND_PORT}"
  echo "Using database ${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_DB_NAME}"
  cd "${REPO_ROOT}"

  local output_file exit_code
  output_file="$(mktemp "${LOCAL_DIR}/backend-start.XXXXXX.log")"
  set +e
  {
    mvn -f backend/pom.xml -DskipTests install
    mvn -f "${APP_POM}" \
      -Dspring-boot.run.arguments="$(backend_arguments)" \
      "${SPRING_BOOT_PLUGIN}"
  } 2>&1 | tee "${output_file}"
  exit_code=${PIPESTATUS[0]}
  set -e

  if [[ "${exit_code}" -ne 0 ]]; then
    diagnose_backend_failure "${exit_code}" "${output_file}"
    exit "${exit_code}"
  fi
}

ensure_frontend_dependencies() {
  if [[ -x "${FRONTEND_ROOT}/node_modules/.bin/vite" ]]; then
    return
  fi

  if [[ "${MANGO_FRONTEND_AUTO_INSTALL}" != "true" ]]; then
    echo "Frontend dependencies are missing: ${FRONTEND_ROOT}/node_modules"
    echo "Run 'npm --prefix frontend install' or set MANGO_FRONTEND_AUTO_INSTALL=true in ${ENV_FILE}."
    exit 1
  fi

  if ! command -v npm >/dev/null 2>&1; then
    echo "npm not found; cannot install frontend dependencies."
    exit 1
  fi

  echo "Frontend dependencies are missing; running npm install under ${FRONTEND_ROOT}."
  cd "${FRONTEND_ROOT}"
  npm install
}

run_frontend() {
  load_workspace_env
  require_port_free "frontend" "${MANGO_FRONTEND_PORT}"
  ensure_frontend_dependencies
  echo "Starting frontend on http://${MANGO_FRONTEND_HOST}:${MANGO_FRONTEND_PORT}"
  echo "Proxy target http://127.0.0.1:${MANGO_BACKEND_PORT}"
  cd "${FRONTEND_ROOT}"
  VITE_ADMIN_PROXY_PATH="http://127.0.0.1:${MANGO_BACKEND_PORT}" \
    VITE_PORT="${MANGO_FRONTEND_PORT}" \
    VITE_HOST="${MANGO_FRONTEND_HOST}" \
    VITE_OPEN="${MANGO_FRONTEND_OPEN}" \
    npm run dev -- --host "${MANGO_FRONTEND_HOST}" --port "${MANGO_FRONTEND_PORT}"
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

command="${1:-backend}"
case "${command}" in
  init)
    write_default_env
    print_config
    ;;
  print)
    print_config
    ;;
  backend)
    run_backend
    ;;
  frontend)
    run_frontend
    ;;
  start)
    start_all
    ;;
  stop)
    stop_workspace
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac

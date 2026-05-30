#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
PID_DIR="${ROOT_DIR}/.mango/pids"
LOG_DIR="${ROOT_DIR}/.mango/logs"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

MANGO_BACKEND_PORT="${MANGO_BACKEND_PORT:-5555}"
MANGO_FRONTEND_PORT="${MANGO_FRONTEND_PORT:-5173}"
MANGO_DB_HOST="${MANGO_DB_HOST:-127.0.0.1}"
MANGO_DB_PORT="${MANGO_DB_PORT:-3306}"
MANGO_DB_NAME="${MANGO_DB_NAME:-{{moduleKebabSnake}}}"
MANGO_DB_USERNAME="${MANGO_DB_USERNAME:-root}"
MANGO_DB_PASSWORD="${MANGO_DB_PASSWORD:-}"
MANGO_DB_URL="${MANGO_DB_URL:-jdbc:mysql://${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai}"
MANGO_DB_AUTO_CREATE="${MANGO_DB_AUTO_CREATE:-true}"
MANGO_OFFICE_PLUGIN_ENABLED="${MANGO_OFFICE_PLUGIN_ENABLED:-false}"
MANGO_START_TIMEOUT_SECONDS="${MANGO_START_TIMEOUT_SECONDS:-180}"

BACKEND_PID_FILE="${PID_DIR}/backend.pid"
FRONTEND_PID_FILE="${PID_DIR}/frontend.pid"

mkdir -p "${PID_DIR}" "${LOG_DIR}"

ensure_port_free() {
  local port="$1"
  local label="$2"
  if lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "${label} port ${port} is already in use." >&2
    exit 1
  fi
}

validate_db_name() {
  case "${MANGO_DB_NAME}" in
    ''|*[!A-Za-z0-9_]*)
      echo "Invalid MANGO_DB_NAME: ${MANGO_DB_NAME}" >&2
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
  if [[ "${MANGO_DB_AUTO_CREATE}" != "true" ]]; then
    return
  fi
  validate_db_name
  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found; cannot create database ${MANGO_DB_NAME}." >&2
    exit 1
  fi
  mysql_exec "CREATE DATABASE IF NOT EXISTS \`${MANGO_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
}

wait_for_http() {
  local url="$1"
  local pid_file="$2"
  local log_file="$3"
  local label="$4"
  local deadline=$((SECONDS + MANGO_START_TIMEOUT_SECONDS))
  while (( SECONDS < deadline )); do
    if [[ -f "${pid_file}" ]]; then
      local pid
      pid="$(cat "${pid_file}")"
      if [[ -n "${pid}" ]] && ! kill -0 "${pid}" >/dev/null 2>&1; then
        echo "${label} exited before ready. Last log lines:" >&2
        tail -80 "${log_file}" >&2 || true
        exit 1
      fi
    fi
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "${label} is ready: ${url}"
      return
    fi
    sleep 2
  done
  echo "Timed out waiting for ${label}: ${url}. Last log lines:" >&2
  tail -120 "${log_file}" >&2 || true
  exit 1
}

start_backend() {
  ensure_port_free "${MANGO_BACKEND_PORT}" "Backend"
  create_database_if_enabled
  (
    cd "${ROOT_DIR}/backend"
    mvn -pl "apps/{{projectKebab}}-monolith-app" -am -DskipTests package
    backend_jar="$(find "apps/{{projectKebab}}-monolith-app/target" -maxdepth 1 -name "{{projectKebab}}-monolith-app-*.jar" ! -name "*.original" | head -n 1)"
    if [[ -z "${backend_jar}" ]]; then
      echo "Backend jar not found under apps/{{projectKebab}}-monolith-app/target." >&2
      exit 1
    fi
    MANGO_BACKEND_PORT="${MANGO_BACKEND_PORT}" \
    MANGO_DB_URL="${MANGO_DB_URL}" \
    MANGO_DB_USERNAME="${MANGO_DB_USERNAME}" \
    MANGO_DB_PASSWORD="${MANGO_DB_PASSWORD}" \
    MANGO_OFFICE_PLUGIN_ENABLED="${MANGO_OFFICE_PLUGIN_ENABLED}" \
    java -jar "${backend_jar}" \
      --office.plugin.enabled="${MANGO_OFFICE_PLUGIN_ENABLED}" \
      > "${LOG_DIR}/backend.log" 2>&1 &
    echo $! > "${BACKEND_PID_FILE}"
  )
  wait_for_http "http://127.0.0.1:${MANGO_BACKEND_PORT}/actuator/health" "${BACKEND_PID_FILE}" "${LOG_DIR}/backend.log" "Backend"
}

start_frontend() {
  ensure_port_free "${MANGO_FRONTEND_PORT}" "Frontend"
  (
    cd "${ROOT_DIR}"
    VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://127.0.0.1:${MANGO_BACKEND_PORT}}" \
    pnpm --filter "{{projectKebab}}-admin" dev --host 0.0.0.0 --port "${MANGO_FRONTEND_PORT}" \
      > "${LOG_DIR}/frontend.log" 2>&1 &
    echo $! > "${FRONTEND_PID_FILE}"
  )
  wait_for_http "http://127.0.0.1:${MANGO_FRONTEND_PORT}/" "${FRONTEND_PID_FILE}" "${LOG_DIR}/frontend.log" "Frontend"
}

"${ROOT_DIR}/scripts/dev-stop.sh" >/dev/null 2>&1 || true
start_backend
start_frontend

echo "Backend: http://127.0.0.1:${MANGO_BACKEND_PORT}"
echo "Frontend: http://127.0.0.1:${MANGO_FRONTEND_PORT}"
echo "Database: ${MANGO_DB_URL}"
echo "Logs: ${LOG_DIR}"

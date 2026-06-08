#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
LOCAL_DIR="${REPO_ROOT}/.mango"
ENV_FILE="${LOCAL_DIR}/dev-workspace.env"
BACKEND_ROOT="${REPO_ROOT}/backend"
APP_POM="${BACKEND_ROOT}/app/pom.xml"
SPRING_BOOT_PLUGIN="org.springframework.boot:spring-boot-maven-plugin:{{springBootVersion}}:run"
DEFAULT_DB_NAME="{{projectKebabSnake}}"

usage() {
  cat <<'EOF'
Usage: scripts/dev-workspace.sh <command>

Commands:
  init       Create .mango/dev-workspace.env if it does not exist
  print      Print current workspace backend configuration
  backend    Start backend using Maven spring-boot:run
  stop       Stop backend process listening on MANGO_BACKEND_PORT

Use this script as the only backend development startup entry.
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
  if port_in_use "${MANGO_BACKEND_PORT}"; then
    echo "Backend port ${MANGO_BACKEND_PORT} is already in use."
    echo "Edit ${ENV_FILE} and choose a free MANGO_BACKEND_PORT, then retry."
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
  require_port_free
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

stop_backend() {
  load_workspace_env
  if ! command -v lsof >/dev/null 2>&1; then
    echo "lsof not found; cannot stop backend on port ${MANGO_BACKEND_PORT}."
    exit 1
  fi

  local pids
  pids="$(lsof -tiTCP:"${MANGO_BACKEND_PORT}" -sTCP:LISTEN 2>/dev/null | sort -u || true)"
  if [[ -z "${pids}" ]]; then
    echo "No backend process listening on port ${MANGO_BACKEND_PORT}."
    return
  fi

  echo "Stopping backend process(es) on port ${MANGO_BACKEND_PORT}: ${pids//$'\n'/ }"
  # shellcheck disable=SC2086
  kill ${pids} >/dev/null 2>&1 || true
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
  stop)
    stop_backend
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac

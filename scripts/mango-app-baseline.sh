#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
ENV_FILE="${REPO_ROOT}/.mango/dev-workspace.env"
RUNTIME_DIR="${REPO_ROOT}/.runtime/mango-app-baseline"
COMPOSE_FILE="${REPO_ROOT}/scripts/docker/nacos-compose.yml"
MAVEN_FILE="${REPO_ROOT}/mango/pom.xml"

APPS=(
  "monolith:mango-monolith-app:io.mango.app.monolith:mango-app/monolith/mango-monolith-app:18558:openapi"
  "gateway:mango-gateway-app:io.mango.app.microservice:mango-app/microservice/mango-gateway-app:18580:health"
  "platform:mango-platform-app:io.mango.app.microservice:mango-app/microservice/mango-platform-app:18581:openapi"
  "business:mango-business-app:io.mango.app.microservice:mango-app/microservice/mango-business-app:18582:openapi"
  "file-preview-service:mango-file-preview-app:io.mango.app.microservice:mango-app/microservice/mango-file-preview-app:18583:openapi"
  "auth:mango-auth-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-auth-capability-app:18610:openapi"
  "authorization:mango-authorization-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-authorization-capability-app:18611:openapi"
  "identity:mango-identity-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-identity-capability-app:18612:openapi"
  "org:mango-org-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-org-capability-app:18613:openapi"
  "system:mango-system-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-system-capability-app:18614:openapi"
  "resource:mango-resource-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-resource-capability-app:18615:openapi"
  "captcha:mango-captcha-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-captcha-capability-app:18616:openapi"
  "file:mango-file-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-file-capability-app:18617:openapi"
  "file-preview:mango-file-preview-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-file-preview-capability-app:18618:openapi"
  "domain:mango-domain-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-domain-capability-app:18619:openapi"
  "notice:mango-notice-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-notice-capability-app:18620:openapi"
  "workflow:mango-workflow-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-workflow-capability-app:18621:openapi"
  "job:mango-job-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-job-capability-app:18622:openapi"
  "calendar:mango-calendar-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-calendar-capability-app:18623:openapi"
  "grid-layout:mango-grid-layout-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-grid-layout-capability-app:18624:openapi"
  "numgen:mango-numgen-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-numgen-capability-app:18625:openapi"
  "template:mango-template-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-template-capability-app:18626:openapi"
  "payment:mango-payment-capability-app:io.mango.app.platformcapability:mango-app/platform-capability/mango-payment-capability-app:18627:openapi"
)

usage() {
  cat <<'EOF'
Usage: scripts/mango-app-baseline.sh <command> [app]

Commands:
  list                 List baseline app targets
  compile [app]        Compile all targets or one target with Maven -am
  package [app]        Package all targets or one target with Maven -am -DskipTests
  clean-package [app]  Clean and package all targets or one target with Maven -am -DskipTests
  docker-build <app> [image]
                       Build a container image for one app with scripts/docker/mango-app.Dockerfile
  start <app>          Start one app in the background and write logs under .runtime/
  stop <app|all>       Stop one app or all apps started by this script
  verify <app>         Start one app, probe /actuator/health and /v3/api-docs, then stop it
  verify-all           Run verify for every app sequentially
  nacos-up             Start local Nacos with Docker Compose
  nacos-down           Stop local Nacos

Profiles:
  local                Default. Uses H2 and disables Nacos.
  nacos                Set MANGO_BASELINE_PROFILE=nacos to enable Nacos discovery/config.
EOF
}

load_workspace_env() {
  if [[ -f "${ENV_FILE}" ]]; then
    set -a
    # shellcheck disable=SC1090
    . "${ENV_FILE}"
    set +a
  fi
  : "${MANGO_DB_HOST:=127.0.0.1}"
  : "${MANGO_DB_PORT:=3306}"
  : "${MANGO_DB_NAME:=mango_baseline}"
  : "${MANGO_DB_USERNAME:=root}"
  : "${MANGO_DB_PASSWORD:=}"
  : "${MANGO_CRYPTO_SM4_SECRET_KEY:=00000000000000000000000000000000}"
  : "${MANGO_INTERNAL_CALL_SECRET:=mango-baseline-internal-call-secret-change-in-production}"
  : "${MANGO_OFFICE_PLUGIN_ENABLED:=false}"
  : "${MANGO_BASELINE_PROFILE:=local}"
  : "${MANGO_BASELINE_DB_MODE:=mysql}"
  : "${MANGO_BASELINE_DB_NAME:=${MANGO_DB_NAME}_baseline}"
  : "${MANGO_NACOS_SERVER_ADDR:=127.0.0.1:8848}"
  : "${MANGO_BASELINE_STOP_WAIT_SECONDS:=10}"
}

split_app() {
  local entry="$1"
  IFS=':' read -r APP_KEY APP_ARTIFACT APP_GROUP APP_PATH APP_PORT APP_DOC_MODE <<<"${entry}"
}

find_app() {
  local key="$1"
  local entry
  for entry in "${APPS[@]}"; do
    split_app "${entry}"
    if [[ "${APP_KEY}" == "${key}" || "${APP_ARTIFACT}" == "${key}" ]]; then
      echo "${entry}"
      return 0
    fi
  done
  return 1
}

list_apps() {
  printf '%-22s %-38s %-6s %s\n' "KEY" "ARTIFACT" "PORT" "PATH"
  local entry
  for entry in "${APPS[@]}"; do
    split_app "${entry}"
    printf '%-22s %-38s %-6s %s\n' "${APP_KEY}" "${APP_ARTIFACT}" "${APP_PORT}" "${APP_PATH}"
  done
}

require_app() {
  local key="${1:-}"
  if [[ -z "${key}" ]]; then
    echo "Missing app key." >&2
    usage >&2
    exit 1
  fi
  local entry
  if ! entry="$(find_app "${key}")"; then
    echo "Unknown app: ${key}" >&2
    list_apps >&2
    exit 1
  fi
  split_app "${entry}"
}

maven_compile() {
  local key="${1:-}"
  if [[ -n "${key}" ]]; then
    require_app "${key}"
    (cd "${REPO_ROOT}" && mvn -f "${MAVEN_FILE}" -pl ":${APP_ARTIFACT}" -am -DskipTests compile)
    return
  fi
  (cd "${REPO_ROOT}" && mvn -f "${MAVEN_FILE}" -pl "$(all_app_artifacts)" -am -DskipTests compile)
}

maven_package() {
  local key="${1:-}"
  if [[ -n "${key}" ]]; then
    require_app "${key}"
    (cd "${REPO_ROOT}" && mvn -f "${MAVEN_FILE}" -pl ":${APP_ARTIFACT}" -am -DskipTests package)
    return
  fi
  (cd "${REPO_ROOT}" && mvn -f "${MAVEN_FILE}" -pl "$(all_app_artifacts)" -am -DskipTests package)
}

maven_clean_package() {
  local key="${1:-}"
  if [[ -n "${key}" ]]; then
    require_app "${key}"
    (cd "${REPO_ROOT}" && mvn -f "${MAVEN_FILE}" -pl ":${APP_ARTIFACT}" -am -DskipTests clean package)
    return
  fi
  (cd "${REPO_ROOT}" && mvn -f "${MAVEN_FILE}" -pl "$(all_app_artifacts)" -am -DskipTests clean package)
}

all_app_artifacts() {
  local entry first=true
  for entry in "${APPS[@]}"; do
    split_app "${entry}"
    if [[ "${first}" == "true" ]]; then
      printf ':%s' "${APP_ARTIFACT}"
      first=false
    else
      printf ',:%s' "${APP_ARTIFACT}"
    fi
  done
}

pid_file() {
  echo "${RUNTIME_DIR}/${APP_KEY}.pid"
}

log_file() {
  echo "${RUNTIME_DIR}/${APP_KEY}.log"
}

report_file() {
  echo "${RUNTIME_DIR}/${APP_KEY}.report"
}

app_jar_file() {
  local target_dir="${REPO_ROOT}/mango/${APP_PATH}/target"
  find "${target_dir}" -maxdepth 1 -type f -name "${APP_ARTIFACT}-*.jar" ! -name "*.original" | sort | tail -n 1
}

is_boot_jar() {
  local jar_file="$1"
  jar tf "${jar_file}" | grep -q '^BOOT-INF/'
}

ensure_app_jar() {
  local jar_file
  jar_file="$(app_jar_file)"
  if [[ "${MANGO_BASELINE_FORCE_PACKAGE:-true}" != "true" && -n "${jar_file}" && -f "${jar_file}" ]] \
      && is_boot_jar "${jar_file}"; then
    echo "${jar_file}"
    return
  fi
  echo "Clean packaging ${APP_KEY} (${APP_ARTIFACT}) before startup..." >&2
  maven_clean_package "${APP_KEY}" >/dev/null
  jar_file="$(app_jar_file)"
  if [[ -z "${jar_file}" || ! -f "${jar_file}" ]]; then
    echo "Missing app jar for ${APP_KEY}: ${APP_ARTIFACT}" >&2
    exit 1
  fi
  if ! is_boot_jar "${jar_file}"; then
    echo "App jar is not executable Spring Boot jar: ${jar_file}" >&2
    exit 1
  fi
  echo "${jar_file}"
}

docker_build() {
  require_app "${1:-}"
  local image="${2:-mango/${APP_ARTIFACT}:local}"
  local jar_file jar_path
  jar_file="$(ensure_app_jar)"
  jar_path="${jar_file#${REPO_ROOT}/}"
  if [[ "${jar_path}" == "${jar_file}" ]]; then
    echo "App jar must be under repository root: ${jar_file}" >&2
    exit 1
  fi
  docker build \
    -f "${REPO_ROOT}/scripts/docker/mango-app.Dockerfile" \
    --build-arg "JAR_FILE=${jar_path}" \
    --build-arg "APP_PORT=${APP_PORT}" \
    -t "${image}" \
    "${REPO_ROOT}"
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

validate_db_name() {
  local db_name="$1"
  case "${db_name}" in
    ''|*[!A-Za-z0-9_]*)
      echo "Invalid database name: ${db_name}" >&2
      echo "Only letters, numbers and underscore are allowed." >&2
      exit 1
      ;;
  esac
}

create_mysql_database_if_needed() {
  if [[ -n "${MANGO_BASELINE_DB_URL:-}" || "${MANGO_BASELINE_DB_MODE}" != "mysql" ]]; then
    return
  fi
  validate_db_name "${MANGO_BASELINE_DB_NAME}"
  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found; set MANGO_BASELINE_DB_URL or MANGO_BASELINE_DB_MODE=h2." >&2
    exit 1
  fi

  local mysql_args sql
  mysql_args=(--protocol=TCP -h "${MANGO_DB_HOST}" -P "${MANGO_DB_PORT}" -u "${MANGO_DB_USERNAME}")
  sql="CREATE DATABASE IF NOT EXISTS \`${MANGO_BASELINE_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  if [[ -n "${MANGO_DB_PASSWORD}" ]]; then
    MYSQL_PWD="${MANGO_DB_PASSWORD}" mysql "${mysql_args[@]}" -e "${sql}"
  else
    mysql "${mysql_args[@]}" -e "${sql}"
  fi
}

reset_mysql_database_if_needed() {
  if [[ "${MANGO_BASELINE_DB_RESET:-true}" != "true" \
      || -n "${MANGO_BASELINE_DB_URL:-}" \
      || "${MANGO_BASELINE_DB_MODE}" != "mysql" ]]; then
    return
  fi
  validate_db_name "${MANGO_BASELINE_DB_NAME}"
  case "${MANGO_BASELINE_DB_NAME}" in
    *_baseline|mango_baseline)
      ;;
    *)
      echo "Refuse to reset non-baseline database: ${MANGO_BASELINE_DB_NAME}" >&2
      echo "Use a database ending with _baseline, or set MANGO_BASELINE_DB_RESET=false." >&2
      exit 1
      ;;
  esac
  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found; cannot reset ${MANGO_BASELINE_DB_NAME}." >&2
    exit 1
  fi

  local mysql_args sql
  mysql_args=(--protocol=TCP -h "${MANGO_DB_HOST}" -P "${MANGO_DB_PORT}" -u "${MANGO_DB_USERNAME}")
  sql="DROP DATABASE IF EXISTS \`${MANGO_BASELINE_DB_NAME}\`; CREATE DATABASE \`${MANGO_BASELINE_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  if [[ -n "${MANGO_DB_PASSWORD}" ]]; then
    MYSQL_PWD="${MANGO_DB_PASSWORD}" mysql "${mysql_args[@]}" -e "${sql}"
  else
    mysql "${mysql_args[@]}" -e "${sql}"
  fi
}

baseline_args() {
  local datasource_url datasource_driver datasource_username datasource_password
  if [[ -n "${MANGO_BASELINE_DB_URL:-}" ]]; then
    datasource_url="${MANGO_BASELINE_DB_URL}"
    datasource_driver="${MANGO_BASELINE_DB_DRIVER:-com.mysql.cj.jdbc.Driver}"
    datasource_username="${MANGO_BASELINE_DB_USERNAME:-${MANGO_DB_USERNAME}}"
    datasource_password="${MANGO_BASELINE_DB_PASSWORD:-${MANGO_DB_PASSWORD}}"
  elif [[ "${MANGO_BASELINE_DB_MODE}" == "h2" ]]; then
    datasource_url="jdbc:h2:mem:${APP_ARTIFACT//-/_};MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE"
    datasource_driver="org.h2.Driver"
    datasource_username="sa"
    datasource_password=""
  else
    datasource_url="jdbc:mysql://${MANGO_DB_HOST}:${MANGO_DB_PORT}/${MANGO_BASELINE_DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    datasource_driver="com.mysql.cj.jdbc.Driver"
    datasource_username="${MANGO_DB_USERNAME}"
    datasource_password="${MANGO_DB_PASSWORD}"
  fi
  local file_preview_enabled="${MANGO_BASELINE_FILE_PREVIEW_ENABLED:-false}"
  case "${APP_KEY}" in
    file-preview|file-preview-service)
      file_preview_enabled="${MANGO_BASELINE_FILE_PREVIEW_ENABLED:-true}"
      ;;
  esac

  local common=(
    "--server.port=${APP_PORT}"
    "--spring.datasource.url=${datasource_url}"
    "--spring.datasource.driver-class-name=${datasource_driver}"
    "--spring.datasource.username=${datasource_username}"
    "--spring.datasource.password=${datasource_password}"
    "--spring.sql.init.mode=never"
    "--spring.flyway.enabled=${MANGO_BASELINE_SPRING_FLYWAY_ENABLED:-true}"
    "--mango.persistence.flyway.enabled=${MANGO_BASELINE_FLYWAY_ENABLED:-true}"
    "--mango.persistence.schema-validation.enabled=false"
    "--mango.persistence.mybatis-plus.tenant.enabled=false"
    "--mango.kv.store.type=memory"
    "--mango.kv.capability.enabled=true"
    "--mango.kv.capability.locker=true"
    "--mango.kv.capability.token-store=true"
    "--mango.kv.capability.outbox=${MANGO_BASELINE_KV_OUTBOX_ENABLED:-true}"
    "--mango.event.outbox.enabled=false"
    "--mango.event.transport=none"
    "--mango.infra.realtime.enabled=${MANGO_BASELINE_REALTIME_ENABLED:-true}"
    "--mango.infra.realtime.outbox.enabled=false"
    "--mango.file.storage-type=LOCAL"
    "--mango.file.local.root-path=${RUNTIME_DIR}/files/${APP_KEY}"
    "--mango.file-preview.enabled=${file_preview_enabled}"
    "--office.plugin.enabled=${MANGO_OFFICE_PLUGIN_ENABLED}"
    "--mango.workflow.enabled=${MANGO_BASELINE_WORKFLOW_ENABLED:-true}"
    "--flowable.database-schema-update=false"
    "--flowable.db-identity-used=false"
    "--flowable.app.enabled=false"
    "--flowable.cmmn.enabled=false"
    "--flowable.dmn.enabled=false"
    "--flowable.idm.enabled=false"
    "--flowable.eventregistry.enabled=false"
    "--flowable.async-executor-activate=false"
    "--flowable.async-history-executor-activate=false"
    "--mango.job.enabled=${MANGO_BASELINE_JOB_ENABLED:-true}"
    "--mango.job.native.scheduler-enabled=${MANGO_BASELINE_JOB_SCHEDULER_ENABLED:-false}"
    "--mango.job.native.embedded-worker-enabled=${MANGO_BASELINE_JOB_EMBEDDED_WORKER_ENABLED:-false}"
    "--mango.job.probe.enabled=false"
    "--mango.payment.scheduler.enabled=false"
    "--mango.security.jwt.secret=${JWT_SECRET:-mango-secret-key-change-in-production-must-be-at-least-32-chars}"
    "--mango.auth.security.permit-paths=/actuator/health,/actuator/health/**,/v3/api-docs,/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html"
    "--mango.security.permit-paths=/actuator/health,/actuator/health/**,/v3/api-docs,/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html"
    "--mango.access.ip-whitelist.enabled=true"
    "--mango.access.ip-whitelist.rules[0].path-pattern=/actuator/health/**"
    "--mango.access.ip-whitelist.rules[0].methods=GET"
    "--mango.access.ip-whitelist.rules[0].cidrs=127.0.0.1/32,::1/128"
    "--mango.access.ip-whitelist.rules[1].path-pattern=/v3/api-docs/**"
    "--mango.access.ip-whitelist.rules[1].methods=GET"
    "--mango.access.ip-whitelist.rules[1].cidrs=127.0.0.1/32,::1/128"
    "--mango.access.ip-whitelist.rules[2].path-pattern=/swagger-ui/**"
    "--mango.access.ip-whitelist.rules[2].methods=GET"
    "--mango.access.ip-whitelist.rules[2].cidrs=127.0.0.1/32,::1/128"
    "--mango.access.ip-whitelist.rules[3].path-pattern=/swagger-ui.html"
    "--mango.access.ip-whitelist.rules[3].methods=GET"
    "--mango.access.ip-whitelist.rules[3].cidrs=127.0.0.1/32,::1/128"
    "--mango.crypto.sm4.secret-key=${MANGO_CRYPTO_SM4_SECRET_KEY}"
    "--mango.internal-call.secret=${MANGO_INTERNAL_CALL_SECRET}"
    "--mango.web.inner.secret=${MANGO_INTERNAL_CALL_SECRET}"
    "--management.endpoints.web.exposure.include=health,info"
    "--management.endpoint.health.show-details=always"
    "--springdoc.api-docs.enabled=true"
    "--springdoc.api-docs.path=/v3/api-docs"
    "--springdoc.swagger-ui.enabled=true"
    "--springdoc.packages-to-scan=io.mango"
    "--logging.level.io.mango=INFO"
  )
  if [[ "${MANGO_BASELINE_PROFILE}" == "nacos" ]]; then
    common+=(
      "--spring.profiles.active=${MANGO_BASELINE_SPRING_PROFILES:-nacos}"
      "--spring.cloud.discovery.enabled=true"
      "--spring.cloud.nacos.discovery.enabled=true"
      "--spring.cloud.nacos.discovery.register-enabled=true"
      "--spring.cloud.nacos.discovery.server-addr=${MANGO_NACOS_SERVER_ADDR}"
      "--spring.cloud.service-registry.auto-registration.enabled=true"
      "--spring.cloud.nacos.config.enabled=${MANGO_BASELINE_NACOS_CONFIG_ENABLED:-false}"
      "--spring.cloud.nacos.config.server-addr=${MANGO_NACOS_SERVER_ADDR}"
    )
  else
    common+=(
      "--spring.cloud.discovery.enabled=false"
      "--spring.cloud.nacos.discovery.enabled=false"
      "--spring.cloud.nacos.config.enabled=false"
    )
  fi
  printf '%s\n' "${common[@]}"
}

start_app() {
  require_app "$1"
  load_workspace_env
  mkdir -p "${RUNTIME_DIR}"
  local pid_file log_file
  pid_file="$(pid_file)"
  log_file="$(log_file)"
  if [[ -f "${pid_file}" ]] && kill -0 "$(cat "${pid_file}")" >/dev/null 2>&1; then
    echo "${APP_KEY} already running with pid $(cat "${pid_file}")"
    return
  fi
  if port_in_use "${APP_PORT}"; then
    echo "Port ${APP_PORT} is already in use for ${APP_KEY}." >&2
    exit 1
  fi
  local jar_file
  create_mysql_database_if_needed
  jar_file="$(ensure_app_jar)"
  echo "Starting ${APP_KEY} (${APP_ARTIFACT}) on http://127.0.0.1:${APP_PORT}"
  (
    cd "${REPO_ROOT}"
    args=()
    while IFS= read -r arg; do
      args+=("${arg}")
    done < <(baseline_args)
    exec java -jar "${jar_file}" "${args[@]}"
  ) >"${log_file}" 2>&1 &
  echo $! >"${pid_file}"
}

stop_app() {
  require_app "$1"
  local file
  file="$(pid_file)"
  if [[ -f "${file}" ]]; then
    local pid
    pid="$(cat "${file}")"
    if kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" >/dev/null 2>&1 || true
      local attempts=$((MANGO_BASELINE_STOP_WAIT_SECONDS * 2))
      for _ in $(seq 1 "${attempts}"); do
        kill -0 "${pid}" >/dev/null 2>&1 || break
        sleep 0.5
      done
      kill -9 "${pid}" >/dev/null 2>&1 || true
    fi
    rm -f "${file}"
  fi
}

stop_all() {
  local entry
  for entry in "${APPS[@]}"; do
    split_app "${entry}"
    stop_app "${APP_KEY}" || true
  done
}

wait_for_health() {
  local url="http://127.0.0.1:${APP_PORT}/actuator/health"
  local report="$1"
  for i in $(seq 1 90); do
    if curl -fsS --max-time 5 "${url}" >"${report}.health.json" 2>"${report}.health.err"; then
      echo "health PASS ${url}" >>"${report}"
      return 0
    fi
    if [[ -f "$(pid_file)" ]] && ! kill -0 "$(cat "$(pid_file)")" >/dev/null 2>&1; then
      echo "health FAIL process-exited ${url}" >>"${report}"
      return 1
    fi
    sleep 1
  done
  echo "health FAIL timeout ${url}" >>"${report}"
  return 1
}

probe_openapi() {
  local report="$1"
  local url="http://127.0.0.1:${APP_PORT}/v3/api-docs"
  if [[ "${APP_DOC_MODE}" != "openapi" ]]; then
    echo "openapi SKIP ${APP_KEY}" >>"${report}"
    return 0
  fi
  if curl -fsS --max-time 10 "${url}" >"${report}.openapi.json" 2>"${report}.openapi.err"; then
    echo "openapi PASS ${url}" >>"${report}"
    return 0
  fi
  echo "openapi FAIL ${url}" >>"${report}"
  return 1
}

assert_runtime_stable() {
  local report="$1"
  sleep "${MANGO_BASELINE_STABILITY_WAIT_SECONDS:-2}"
  if [[ -f "$(pid_file)" ]] && ! kill -0 "$(cat "$(pid_file)")" >/dev/null 2>&1; then
    echo "runtime FAIL process-exited-after-probe" >>"${report}"
    return 1
  fi
  local log_file
  log_file="$(log_file)"
  if [[ -f "${log_file}" ]] && rg -q "Application run failed|APPLICATION FAILED|NoClassDefFoundError|Invalid signature|Missing X-Internal-Call|UnknownHostException|Remote resource target execution failed" "${log_file}"; then
    echo "runtime FAIL startup-error-log ${log_file}" >>"${report}"
    return 1
  fi
  echo "runtime PASS stable-after-probe" >>"${report}"
}

verify_app() {
  require_app "$1"
  load_workspace_env
  reset_mysql_database_if_needed
  mkdir -p "${RUNTIME_DIR}"
  local report
  report="$(report_file)"
  : >"${report}"
  echo "app ${APP_KEY}" >>"${report}"
  echo "artifact ${APP_ARTIFACT}" >>"${report}"
  echo "port ${APP_PORT}" >>"${report}"
  start_app "${APP_KEY}"
  local status=0
  wait_for_health "${report}" || status=1
  if [[ "${status}" == "0" ]]; then
    probe_openapi "${report}" || status=1
  fi
  if [[ "${status}" == "0" ]]; then
    assert_runtime_stable "${report}" || status=1
  fi
  stop_app "${APP_KEY}"
  if [[ "${status}" == "0" ]]; then
    echo "verify PASS ${APP_KEY}"
  else
    echo "verify FAIL ${APP_KEY}; see ${report} and $(log_file)" >&2
  fi
  return "${status}"
}

verify_all() {
  load_workspace_env
  if [[ "${MANGO_BASELINE_PROFILE}" == "nacos" ]]; then
    verify_all_nacos
    return
  fi
  mkdir -p "${RUNTIME_DIR}"
  local summary="${RUNTIME_DIR}/summary.tsv"
  : >"${summary}"
  local entry status
  for entry in "${APPS[@]}"; do
    split_app "${entry}"
    if verify_app "${APP_KEY}"; then
      status=PASS
    else
      status=FAIL
    fi
    printf '%s\t%s\t%s\t%s\n' "${APP_KEY}" "${APP_ARTIFACT}" "${APP_PORT}" "${status}" >>"${summary}"
  done
  echo "Summary: ${summary}"
  if awk -F '\t' '$4 == "FAIL" { found = 1 } END { exit found ? 0 : 1 }' "${summary}"; then
    return 1
  fi
}

verify_all_nacos() {
  mkdir -p "${RUNTIME_DIR}"
  local summary="${RUNTIME_DIR}/summary.tsv"
  : >"${summary}"
  reset_mysql_database_if_needed

  stop_all
  echo "Starting shared resource dependency for Nacos baseline..."
  start_app resource
  require_app resource
  local resource_report
  resource_report="$(report_file)"
  : >"${resource_report}"
  echo "app ${APP_KEY}" >>"${resource_report}"
  echo "artifact ${APP_ARTIFACT}" >>"${resource_report}"
  echo "port ${APP_PORT}" >>"${resource_report}"
  local resource_status=0
  wait_for_health "${resource_report}" || resource_status=1
  if [[ "${resource_status}" == "0" ]]; then
    probe_openapi "${resource_report}" || resource_status=1
  fi
  if [[ "${resource_status}" == "0" ]]; then
    assert_runtime_stable "${resource_report}" || resource_status=1
  fi

  local entry status
  printf '%s\t%s\t%s\t%s\n' "resource" "mango-resource-capability-app" "18615" \
    "$([[ "${resource_status}" == "0" ]] && echo PASS || echo FAIL)" >>"${summary}"
  for entry in "${APPS[@]}"; do
    split_app "${entry}"
    if [[ "${APP_KEY}" == "resource" ]]; then
      continue
    fi
    local report
    report="$(report_file)"
    : >"${report}"
    echo "app ${APP_KEY}" >>"${report}"
    echo "artifact ${APP_ARTIFACT}" >>"${report}"
    echo "port ${APP_PORT}" >>"${report}"
    start_app "${APP_KEY}"
    status=0
    wait_for_health "${report}" || status=1
    if [[ "${status}" == "0" ]]; then
      probe_openapi "${report}" || status=1
    fi
    if [[ "${status}" == "0" ]]; then
      assert_runtime_stable "${report}" || status=1
    fi
    if [[ "${status}" == "0" ]]; then
      printf '%s\t%s\t%s\t%s\n' "${APP_KEY}" "${APP_ARTIFACT}" "${APP_PORT}" "PASS" >>"${summary}"
    else
      printf '%s\t%s\t%s\t%s\n' "${APP_KEY}" "${APP_ARTIFACT}" "${APP_PORT}" "FAIL" >>"${summary}"
    fi
  done

  stop_all
  echo "Summary: ${summary}"
  if awk -F '\t' '$4 == "FAIL" { found = 1 } END { exit found ? 0 : 1 }' "${summary}"; then
    return 1
  fi
}

nacos_up() {
  docker rm -f mango-baseline-nacos >/dev/null 2>&1 || true
  docker compose -f "${COMPOSE_FILE}" up -d
}

nacos_down() {
  docker compose -f "${COMPOSE_FILE}" down
}

case "${1:-}" in
  list) list_apps ;;
  compile) maven_compile "${2:-}" ;;
  package) maven_package "${2:-}" ;;
  clean-package) maven_clean_package "${2:-}" ;;
  start) start_app "${2:-}" ;;
  stop)
    if [[ "${2:-}" == "all" ]]; then stop_all; else stop_app "${2:-}"; fi
    ;;
  verify) verify_app "${2:-}" ;;
  verify-all) verify_all ;;
  docker-build) docker_build "${2:-}" "${3:-}" ;;
  nacos-up) nacos_up ;;
  nacos-down) nacos_down ;;
  -h|--help|help) usage ;;
  *) usage >&2; exit 1 ;;
esac

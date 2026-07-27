#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
runtime_dir="${repo_root}/.runtime"
run_id="$(date +%Y%m%d-%H%M%S)"
log_file="${runtime_dir}/bootstrap-performance-${run_id}.log"

mysql_host="${MANGO_DB_HOST:-127.0.0.1}"
mysql_port="${MANGO_DB_PORT:-3306}"
mysql_user="${MANGO_DB_USERNAME:-root}"
mysql_password="${MANGO_DB_PASSWORD:-}"
sql_database="${MANGO_BOOTSTRAP_SQL_PERF_DATABASE:-mango_dev_mango_bootstrap_sql_perf}"
resource_database="${MANGO_BOOTSTRAP_RESOURCE_PERF_DATABASE:-mango_dev_mango_bootstrap_resource_perf}"

validate_database_name() {
  local database="$1"
  local suffix="$2"
  if [[ ! "${database}" =~ ^[a-zA-Z0-9_]+$ || "${database}" != *"${suffix}" ]]; then
    echo "Refusing unsafe performance database name: ${database}" >&2
    exit 2
  fi
}

validate_database_name "${sql_database}" "_bootstrap_sql_perf"
validate_database_name "${resource_database}" "_bootstrap_resource_perf"
if [[ "${sql_database}" == "${resource_database}" ]]; then
  echo "SQL and Resource performance databases must be different." >&2
  exit 2
fi

mkdir -p "${runtime_dir}"

mysql_command=(mysql --protocol=TCP --host="${mysql_host}" --port="${mysql_port}" --user="${mysql_user}" --batch --skip-column-names)
mysql_exec() {
  MYSQL_PWD="${mysql_password}" "${mysql_command[@]}" "$@"
}

cleanup() {
  mysql_exec --execute="DROP DATABASE IF EXISTS \`${sql_database}\`; DROP DATABASE IF EXISTS \`${resource_database}\`;" \
    >/dev/null 2>&1 || true
}
trap cleanup EXIT

cleanup
mysql_exec --execute="CREATE DATABASE \`${sql_database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE DATABASE \`${resource_database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

sql_jdbc_url="jdbc:mysql://${mysql_host}:${mysql_port}/${sql_database}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
resource_jdbc_url="jdbc:mysql://${mysql_host}:${mysql_port}/${resource_database}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"

{
  echo "Mango Bootstrap performance run: ${run_id}"
  echo "SQL database: ${sql_database}"
  echo "Resource database: ${resource_database}"

  cd "${repo_root}/mango"
  MANGO_BOOTSTRAP_PERF_DB_URL="${sql_jdbc_url}" \
  MANGO_DB_USERNAME="${mysql_user}" \
  MANGO_DB_PASSWORD="${mysql_password}" \
    mvn -B -ntp \
      -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -am \
      -Dtest=PersistenceColdBaselinePerformanceIntegrationTest \
      -Dsurefire.failIfNoSpecifiedTests=false test

  MANGO_BOOTSTRAP_RESOURCE_PERF_DB_URL="${resource_jdbc_url}" \
  MANGO_BOOTSTRAP_RESOURCE_PERF_DB_USERNAME="${mysql_user}" \
  MANGO_BOOTSTRAP_RESOURCE_PERF_DB_PASSWORD="${mysql_password}" \
    mvn -B -ntp \
      -pl mango-admin-starter -am \
      -Dtest=BootstrapResourcePerformanceIntegrationTest \
      -Dsurefire.failIfNoSpecifiedTests=false test
} 2>&1 | tee "${log_file}"

echo "Performance log: ${log_file}"

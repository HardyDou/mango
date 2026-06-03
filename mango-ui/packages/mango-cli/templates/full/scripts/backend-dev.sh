#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_PORT="${MANGO_BACKEND_PORT:-5555}"

cd "$ROOT_DIR"

mvn -f backend/pom.xml -DskipTests install
mvn -f backend/app/pom.xml \
  -Dspring-boot.run.jvmArguments="-Dserver.port=${BACKEND_PORT}" \
  spring-boot:run

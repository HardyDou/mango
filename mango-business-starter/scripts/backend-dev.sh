#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "scripts/backend-dev.sh is kept for compatibility."
echo "Use mango dev start backend as the backend development startup entry."
exec "${ROOT_DIR}/scripts/dev-workspace.sh" backend

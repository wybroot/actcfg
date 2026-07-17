#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
(cd "$ROOT/backend" && mvn test)
(cd "$ROOT/frontend" && npm install && npm run build)

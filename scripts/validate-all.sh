#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

run_step() {
  local label="$1"
  shift
  echo "==> $label"
  (cd "$ROOT_DIR" && "$@")
}

run_step "Backend — ./mvnw clean test" bash -lc 'cd backend-api && ./mvnw clean test'
run_step "Frontend — ./run-tests.sh" bash -lc 'cd frontend-web && ./run-tests.sh'
run_step "Android — ./gradlew testDebugUnitTest assembleDebug" bash -lc 'cd mobile-android && ./gradlew testDebugUnitTest assembleDebug'

echo "Validation complète OK."

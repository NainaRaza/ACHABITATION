#!/usr/bin/env sh
set -eu
node --check app.js
node --check playwright.config.mjs
for file in src/*.js tests/e2e/*.js; do
  node --check "$file"
done
node tests/frontend-smoke.test.mjs
node tests/frontend-flow.test.mjs

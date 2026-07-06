#!/usr/bin/env sh
set -eu
node --check app.js
for file in src/*.js; do
  node --check "$file"
done
node tests/frontend-smoke.test.mjs
node tests/frontend-flow.test.mjs

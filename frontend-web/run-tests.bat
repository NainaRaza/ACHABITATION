@echo off
node --check app.js || exit /b 1
node --check playwright.config.mjs || exit /b 1
for %%f in (src\*.js tests\e2e\*.js) do node --check %%f || exit /b 1
node tests\frontend-smoke.test.mjs || exit /b 1
node tests\frontend-flow.test.mjs || exit /b 1

@echo off
setlocal
cd /d %~dp0
.\mvnw.cmd clean test
endlocal

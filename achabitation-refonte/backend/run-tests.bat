@echo off
setlocal
cd /d %~dp0
mvn clean test
endlocal

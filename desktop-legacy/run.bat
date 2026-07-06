@echo off
cd /d %~dp0
if not exist out mkdir out
javac -encoding UTF-8 -d out src\com\vacances\ravtricount\*.java
if errorlevel 1 pause & exit /b 1
java -cp out com.vacances.ravtricount.Main
pause

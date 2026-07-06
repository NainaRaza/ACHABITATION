@echo off
setlocal
set MAVEN_VERSION=3.9.10
set MVNW_DIR=%~dp0
set MAVEN_DIR=%MVNW_DIR%.mvn\apache-maven-%MAVEN_VERSION%
set MAVEN_BIN=%MAVEN_DIR%\bin\mvn.cmd
set MAVEN_ZIP=%MVNW_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

if not exist "%MAVEN_BIN%" (
  if not exist "%MVNW_DIR%.mvn" mkdir "%MVNW_DIR%.mvn"
  if not exist "%MAVEN_ZIP%" (
    echo Maven absent du PATH. Telechargement de Maven %MAVEN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
    if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
  )
  echo Extraction de Maven %MAVEN_VERSION%...
  if exist "%MAVEN_DIR%" rmdir /s /q "%MAVEN_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%MAVEN_ZIP%' '%MVNW_DIR%.mvn'"
  if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
)

"%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%

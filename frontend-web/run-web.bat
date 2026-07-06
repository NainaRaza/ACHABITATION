@echo off
cd /d %~dp0
where py >nul 2>nul
if %ERRORLEVEL%==0 (
  py -m http.server 5173
  exit /b %ERRORLEVEL%
)
where python >nul 2>nul
if %ERRORLEVEL%==0 (
  python -m http.server 5173
  exit /b %ERRORLEVEL%
)
echo Python n'est pas disponible. Installez Python ou servez ce dossier avec un serveur web local sur le port 5173.
exit /b 1

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if command -v python3 >/dev/null 2>&1; then
  python3 -m http.server 5173
elif command -v python >/dev/null 2>&1; then
  python -m http.server 5173
else
  echo "Python n'est pas disponible. Servez ce dossier avec un serveur web local sur le port 5173." >&2
  exit 1
fi

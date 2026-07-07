#!/usr/bin/env bash
set -euo pipefail

: "${DATABASE_URL:?DATABASE_URL est obligatoire, ex: postgresql://localhost:5432/achabitation}"
BACKUP_FILE="${1:-}"

if [[ -z "$BACKUP_FILE" || ! -f "$BACKUP_FILE" ]]; then
  echo "Usage: DATABASE_URL=postgresql://... $0 backups/achabitation-YYYYMMDDTHHMMSSZ.dump" >&2
  exit 2
fi

pg_restore --clean --if-exists --no-owner --no-privileges --dbname "$DATABASE_URL" "$BACKUP_FILE"
echo "Restauration terminée depuis : $BACKUP_FILE"

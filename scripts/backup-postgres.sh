#!/usr/bin/env bash
set -euo pipefail

: "${DATABASE_URL:?DATABASE_URL est obligatoire, ex: postgresql://localhost:5432/achabitation}"
BACKUP_DIR="${BACKUP_DIR:-backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUTPUT="$BACKUP_DIR/achabitation-$TIMESTAMP.dump"

mkdir -p "$BACKUP_DIR"
pg_dump --format=custom --no-owner --no-privileges --file "$OUTPUT" "$DATABASE_URL"
find "$BACKUP_DIR" -name 'achabitation-*.dump' -mtime +"$RETENTION_DAYS" -delete

echo "Sauvegarde créée : $OUTPUT"

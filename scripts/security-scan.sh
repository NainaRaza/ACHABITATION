#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "[SECURITY] $*" >&2
  exit 1
}

echo "[SECURITY] Vérification des secrets évidents dans les fichiers suivis par Git..."

# Détecte les affectations suspectes avec une vraie valeur longue, tout en ignorant les placeholders et les noms de variables.
if git grep -nI -E '(password|passwd|secret|token|api[_-]?key|private[_-]?key)[[:space:]]*[:=][[:space:]]*["'"'"']?[A-Za-z0-9_./+=-]{24,}' -- \
    ':(exclude).git/**' \
    ':(exclude)backend-api/target/**' \
    ':(exclude)frontend-web/node_modules/**' \
    ':(exclude)mobile-android/.gradle/**' \
    ':(exclude)mobile-android/build/**' \
    ':(exclude)**/*.md' \
    ':(exclude)**/*.patch' | \
    grep -viE 'CHANGE_ME|example|placeholder|must be set|\$\{|Provider|TokenService|accessToken|refreshToken|sessionToken|csrf|passwordEncoder|password-reset|reset-token|X-Session-Token'; then
  fail "Secret potentiel détecté. Remplacer la valeur par une variable d'environnement ou un placeholder."
fi

echo "[SECURITY] Vérification des fichiers d'environnement sensibles..."
if git ls-files | grep -E '(^|/)\.env(\..*)?$' | grep -vE '(^|/)\.env(\..*)?\.example$'; then
  fail "Un fichier .env réel est suivi par Git. Seuls les fichiers *.example doivent être versionnés."
fi

echo "[SECURITY] Vérification des artefacts locaux suivis par Git..."
if git ls-files | grep -E '(^|/)(target|build|\.gradle|node_modules)/'; then
  fail "Des artefacts locaux sont suivis par Git. Nettoyer l'index avant release."
fi

echo "[SECURITY] OK"

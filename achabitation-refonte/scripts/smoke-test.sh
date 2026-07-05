#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${1:-http://localhost:8080/api/v1}"
STAMP="$(date +%Y%m%d%H%M%S)"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "Commande manquante: $1" >&2; exit 1; }
}
require_cmd curl
require_cmd python3

json_field() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)[sys.argv[1]])' "$1"
}

api() {
  local method="$1" url="$2" token="${3:-}" body="${4:-}"
  if [[ -n "$body" ]]; then
    if [[ -n "$token" ]]; then
      curl -fsS -X "$method" "$url" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "$body"
    else
      curl -fsS -X "$method" "$url" -H "Content-Type: application/json" -d "$body"
    fi
  else
    if [[ -n "$token" ]]; then
      curl -fsS -X "$method" "$url" -H "Authorization: Bearer $token"
    else
      curl -fsS -X "$method" "$url"
    fi
  fi
}

echo "[1/9] Healthcheck"
api GET "$BASE_URL/health" >/dev/null
api GET "$BASE_URL/health/readiness" >/dev/null

echo "[2/9] Création compte owner"
OWNER_JSON=$(api POST "$BASE_URL/auth/register" "" "{\"email\":\"owner-$STAMP@example.com\",\"displayName\":\"Owner Beta\",\"password\":\"motdepassefort\"}")
OWNER_TOKEN=$(printf '%s' "$OWNER_JSON" | json_field devToken)

echo "[3/9] Création voyage"
TRIP_JSON=$(api POST "$BASE_URL/trips" "$OWNER_TOKEN" "{\"name\":\"Smoke test $STAMP\",\"startDate\":\"2026-08-01\",\"endDate\":\"2026-08-15\",\"referenceCurrency\":\"EUR\",\"customConstraints\":[\"Sans porc\"]}")
TRIP_ID=$(printf '%s' "$TRIP_JSON" | json_field id)

echo "[4/9] Création participant·es"
SOFIA_JSON=$(api POST "$BASE_URL/trips/$TRIP_ID/persons" "$OWNER_TOKEN" '{"name":"Sofia","livingRest":2000,"weightMode":"LIVING_REST","advancedLivingRest":false,"vegetarian":false,"noAlcohol":false,"livingRestPublic":true,"customConstraints":[],"presencePeriods":[{"startDate":"2026-08-01","endDate":"2026-08-15"}]}')
SOFIA_ID=$(printf '%s' "$SOFIA_JSON" | json_field id)
KARIM_JSON=$(api POST "$BASE_URL/trips/$TRIP_ID/persons" "$OWNER_TOKEN" '{"name":"Karim","livingRest":0,"weightMode":"AVERAGE","advancedLivingRest":false,"vegetarian":true,"noAlcohol":false,"livingRestPublic":true,"customConstraints":["Sans porc"],"presencePeriods":[{"startDate":"2026-08-01","endDate":"2026-08-15"}]}')
KARIM_ID=$(printf '%s' "$KARIM_JSON" | json_field id)

echo "[5/9] Création dépenses"
api POST "$BASE_URL/trips/$TRIP_ID/expenses" "$OWNER_TOKEN" "{\"title\":\"Courses\",\"date\":\"2026-08-02\",\"payerPersonId\":\"$SOFIA_ID\",\"totalAmount\":120,\"meatAmount\":30,\"alcoholAmount\":20,\"customConstraintAmounts\":{\"Sans porc\":10},\"type\":\"NORMAL\",\"advancedMode\":false,\"manualParticipantIds\":[],\"currency\":\"EUR\",\"exchangeRateToTripCurrency\":1}" >/dev/null
api POST "$BASE_URL/trips/$TRIP_ID/expenses" "$OWNER_TOKEN" "{\"title\":\"Essence mutualisée\",\"date\":\"2026-08-03\",\"payerPersonId\":\"$KARIM_ID\",\"totalAmount\":250,\"meatAmount\":0,\"alcoholAmount\":0,\"customConstraintAmounts\":{},\"type\":\"GLOBAL\",\"advancedMode\":false,\"manualParticipantIds\":[],\"currency\":\"EUR\",\"exchangeRateToTripCurrency\":1}" >/dev/null

echo "[6/9] Résumé"
api GET "$BASE_URL/trips/$TRIP_ID/summary" "$OWNER_TOKEN" >/dev/null

echo "[7/9] Invitation"
INV_JSON=$(api POST "$BASE_URL/trips/$TRIP_ID/invitations" "$OWNER_TOKEN" '{"roleToGrant":"PARTICIPANT","expiresInDays":7}')
INV_CODE=$(printf '%s' "$INV_JSON" | json_field code)
MEMBER_JSON=$(api POST "$BASE_URL/auth/register" "" "{\"email\":\"member-$STAMP@example.com\",\"displayName\":\"Member Beta\",\"password\":\"motdepassefort\"}")
MEMBER_TOKEN=$(printf '%s' "$MEMBER_JSON" | json_field devToken)
api POST "$BASE_URL/trips/$TRIP_ID/join" "$MEMBER_TOKEN" "{\"invitationCode\":\"$INV_CODE\"}" >/dev/null
api GET "$BASE_URL/trips/$TRIP_ID/persons" "$MEMBER_TOKEN" >/dev/null

echo "[8/9] Exports"
api GET "$BASE_URL/trips/$TRIP_ID/exports/expenses.csv" "$OWNER_TOKEN" >/dev/null
api GET "$BASE_URL/trips/$TRIP_ID/exports/summary.csv" "$OWNER_TOKEN" >/dev/null

echo "[9/9] Smoke test OK — voyage $TRIP_ID"

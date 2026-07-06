# Exemples API — ACHABITATION

Ces exemples supposent que le backend tourne sur :

```text
http://localhost:8080/api/v1
```

Les routes applicatives exigent :

```http
Authorization: Bearer <token>
```

Le token est renvoyé par `/auth/register` ou `/auth/login`.

## Créer un compte

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joey@example.com",
    "displayName": "Joey",
    "password": "votre-mot-de-passe"
  }'
```

Conserver la valeur `devToken` retournée.

## Se connecter

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joey@example.com",
    "password": "votre-mot-de-passe"
  }'
```

## Créer un voyage

```bash
curl -X POST http://localhost:8080/api/v1/trips \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Vacances août 2026",
    "startDate": "2026-08-01",
    "endDate": "2026-08-15",
    "referenceCurrency": "EUR",
    "customConstraints": ["Sans porc"]
  }'
```

## Ajouter une personne avec deux périodes de présence

```bash
curl -X POST http://localhost:8080/api/v1/trips/TRIP_ID/persons \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Karim",
    "livingRest": 1200,
    "weightMode": "LIVING_REST",
    "advancedLivingRest": false,
    "vegetarian": false,
    "noAlcohol": true,
    "livingRestPublic": true,
    "customConstraints": [],
    "presencePeriods": [
      {"startDate": "2026-08-01", "endDate": "2026-08-05"},
      {"startDate": "2026-08-10", "endDate": "2026-08-15"}
    ]
  }'
```

## Ajouter une dépense normale

```bash
curl -X POST http://localhost:8080/api/v1/trips/TRIP_ID/expenses \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Courses Carrefour",
    "date": "2026-08-02",
    "payerPersonId": "PERSON_ID",
    "totalAmount": 120,
    "meatAmount": 30,
    "alcoholAmount": 20,
    "customConstraintAmounts": {"Sans porc": 10},
    "type": "NORMAL",
    "advancedMode": false,
    "manualParticipantIds": [],
    "currency": "EUR",
    "exchangeRateToTripCurrency": 1
  }'
```

## Ajouter une dépense globale

```bash
curl -X POST http://localhost:8080/api/v1/trips/TRIP_ID/expenses \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Essence mutualisée",
    "date": "2026-08-03",
    "payerPersonId": "PERSON_ID",
    "totalAmount": 250,
    "meatAmount": 0,
    "alcoholAmount": 0,
    "customConstraintAmounts": {},
    "type": "GLOBAL",
    "advancedMode": false,
    "manualParticipantIds": [],
    "currency": "EUR",
    "exchangeRateToTripCurrency": 1
  }'
```

## Créer une invitation

```bash
curl -X POST http://localhost:8080/api/v1/trips/TRIP_ID/invitations \
  -H "Authorization: Bearer TOKEN_OWNER" \
  -H "Content-Type: application/json" \
  -d '{
    "roleToGrant": "PARTICIPANT",
    "expiresInDays": 7
  }'
```

## Rejoindre un voyage

```bash
curl -X POST http://localhost:8080/api/v1/trips/TRIP_ID/join \
  -H "Authorization: Bearer TOKEN_MEMBER" \
  -H "Content-Type: application/json" \
  -d '{
    "invitationCode": "CODE_INVITATION"
  }'
```

## Lier son compte à un guest sans appliquer son profil

```bash
curl -X POST http://localhost:8080/api/v1/trips/TRIP_ID/persons/PERSON_ID/link-current-user \
  -H "Authorization: Bearer TOKEN_MEMBER" \
  -H "Content-Type: application/json" \
  -d '{
    "applyProfileToGuest": false
  }'
```

## Voir le résumé

```bash
curl http://localhost:8080/api/v1/trips/TRIP_ID/summary \
  -H "Authorization: Bearer TOKEN"
```

## Exports CSV

```bash
curl http://localhost:8080/api/v1/trips/TRIP_ID/exports/expenses.csv \
  -H "Authorization: Bearer TOKEN" \
  -o depenses.csv

curl http://localhost:8080/api/v1/trips/TRIP_ID/exports/summary.csv \
  -H "Authorization: Bearer TOKEN" \
  -o resume.csv
```

## Rejoindre un voyage avec un code d'invitation

Une personne connectée peut rejoindre un voyage sans connaître son identifiant technique en utilisant uniquement le code transmis par l'organisateur·ice.

```http
POST /api/v1/trips/join-by-code
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "invitationCode": "FaB_m-ZB0S0N5FJm",
  "applyProfileToGuest": false
}
```

Réponse : le voyage rejoint.

```json
{
  "id": "...",
  "name": "Vacances été 2026",
  "startDate": "2026-07-01",
  "endDate": "2026-07-12",
  "referenceCurrency": "EUR",
  "active": true
}
```

Dans l'interface web, le champ est disponible depuis le tableau de bord, dans le bloc “Mes voyages”, section “Rejoindre un voyage”.

## Créer une personne directement liée au compte connecté

Cette route sert au bouton **+ M’ajouter moi-même** et au choix **Autre personne** après avoir rejoint un voyage.

```http
POST /api/v1/trips/{tripId}/persons/current-user
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Nvoskerjen",
  "applyProfileToPerson": true,
  "presencePeriods": [
    {"startDate": "2026-07-01", "endDate": "2026-07-12"}
  ]
}
```

Règles :

- le compte doit déjà être membre du voyage ;
- le compte ne doit pas déjà être lié à une autre personne du même voyage ;
- le nom choisi doit être unique dans le voyage ;
- les périodes de présence doivent respecter les dates du voyage ;
- si `applyProfileToPerson` vaut `true`, le profil utilisateur doit être exploitable ;
- si `applyProfileToPerson` vaut `false`, la personne est créée en mode moyenne.

## Rejoindre puis choisir un guest ou créer sa personne

Le parcours conseillé côté interface est :

1. appeler `POST /api/v1/trips/join-by-code` avec le code d’invitation ;
2. charger `GET /api/v1/trips/{tripId}/persons` ;
3. proposer les guests existants ;
4. soit appeler `POST /api/v1/trips/{tripId}/join` avec `guestPersonId` ;
5. soit appeler `POST /api/v1/trips/{tripId}/persons/current-user` pour créer une nouvelle personne liée au compte.

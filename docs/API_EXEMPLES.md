# Exemples API ACHABITATION

Les exemples utilisent l’URL locale :

```text
http://localhost:8080/api/v1
```

Les routes protégées exigent :

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Les UUID retournés par l’API sont représentés par des variables dans les exemples : `$TOKEN`, `$TRIP_ID`, `$PERSON_ID`, etc.

## Healthcheck

```bash
curl -s http://localhost:8080/api/v1/health
curl -s http://localhost:8080/api/v1/health/readiness
```

## Authentification

Créer un compte :

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@example.com",
    "displayName": "Owner Beta",
    "password": "motdepassefort"
  }'
```

Se connecter :

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@example.com",
    "password": "motdepassefort"
  }'
```

Le champ attendu par le backend est `email`, pas `identifier`.

Se déconnecter :

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

## Profil utilisateur

Lire le profil :

```bash
curl -s http://localhost:8080/api/v1/auth/profile \
  -H "Authorization: Bearer $TOKEN"
```

Modifier le compte :

```bash
curl -s -X PUT http://localhost:8080/api/v1/auth/account \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner.new@example.com",
    "displayName": "Owner Nouveau"
  }'
```

Modifier le profil RAV :

```bash
curl -s -X PUT http://localhost:8080/api/v1/auth/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Owner Beta",
    "livingRest": 1800,
    "weightMode": "LIVING_REST",
    "advancedLivingRest": false,
    "netIncomeAfterTax": 0,
    "rent": 0,
    "credits": 0,
    "fixedCharges": 0,
    "transport": 0,
    "insurance": 0,
    "otherMandatoryExpenses": 0,
    "menstrualProtection": 0,
    "vegetarian": false,
    "noAlcohol": false,
    "livingRestPublic": false,
    "customConstraints": ["Sans porc"]
  }'
```

Appliquer le profil à des personnes liées :

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/profile/apply-to-linked-persons \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "personIds": ["<person-id>"]
  }'
```

## Voyages

Créer un voyage :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Vacances août",
    "startDate": "2026-08-01",
    "endDate": "2026-08-15",
    "referenceCurrency": "EUR",
    "customConstraints": ["Sans porc", "Sans lactose"]
  }'
```

Lister ses voyages :

```bash
curl -s http://localhost:8080/api/v1/trips \
  -H "Authorization: Bearer $TOKEN"
```

Modifier les contraintes du voyage :

```bash
curl -s -X PUT http://localhost:8080/api/v1/trips/$TRIP_ID/constraints \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "customConstraints": ["Sans porc", "Sans lactose", "Sans gluten"] }'
```

Cette opération exige `OWNER` ou `ADMIN`.

## Invitations

Créer une invitation :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/$TRIP_ID/invitations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roleToGrant": "PARTICIPANT",
    "expiresInDays": 7
  }'
```

`roleToGrant` peut être `ADMIN`, `PARTICIPANT` ou `READ_ONLY`. Une demande `OWNER` est ramenée à `PARTICIPANT` par le service.

Rejoindre par code :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/join-by-code \
  -H "Authorization: Bearer $MEMBER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invitationCode": "<code>",
    "guestPersonId": null,
    "applyProfileToGuest": false
  }'
```

Révoquer une invitation :

```bash
curl -i -X DELETE http://localhost:8080/api/v1/trips/$TRIP_ID/invitations/$INVITATION_ID \
  -H "Authorization: Bearer $TOKEN"
```

## Personnes

Créer une personne guest :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/$TRIP_ID/persons \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sofia",
    "livingRest": 2000,
    "weightMode": "LIVING_REST",
    "advancedLivingRest": false,
    "netIncomeAfterTax": 0,
    "rent": 0,
    "credits": 0,
    "fixedCharges": 0,
    "transport": 0,
    "insurance": 0,
    "otherMandatoryExpenses": 0,
    "menstrualProtection": 0,
    "vegetarian": false,
    "noAlcohol": false,
    "livingRestPublic": true,
    "customConstraints": [],
    "presencePeriods": [
      { "startDate": "2026-08-01", "endDate": "2026-08-15" }
    ]
  }'
```

Ajouter le compte courant comme personne du voyage :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/$TRIP_ID/persons/current-user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Owner Beta",
    "applyProfileToPerson": true,
    "presencePeriods": [
      { "startDate": "2026-08-01", "endDate": "2026-08-15" }
    ]
  }'
```

Lier un guest au compte courant :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/$TRIP_ID/persons/$PERSON_ID/link-current-user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "applyProfileToGuest": false }'
```

`userId` existe dans le DTO mais n’est pas utilisé pour choisir le compte : le compte pris en compte est celui du token authentifié.

Lister les personnes :

```bash
curl -s http://localhost:8080/api/v1/trips/$TRIP_ID/persons \
  -H "Authorization: Bearer $TOKEN"
```

Si une personne est liée à un compte et que son RAV est privé, les autres membres reçoivent `livingRest=null` et `livingRestHidden=true`.

Désactiver une personne :

```bash
curl -i -X DELETE http://localhost:8080/api/v1/trips/$TRIP_ID/persons/$PERSON_ID \
  -H "Authorization: Bearer $TOKEN"
```

## Dépenses

Créer une dépense normale :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/$TRIP_ID/expenses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Courses",
    "date": "2026-08-02",
    "payerPersonId": "<person-id-payeur>",
    "totalAmount": 120,
    "meatAmount": 30,
    "alcoholAmount": 20,
    "customConstraintAmounts": { "Sans porc": 10 },
    "type": "NORMAL",
    "advancedMode": false,
    "manualParticipantIds": [],
    "currency": "EUR",
    "exchangeRateToTripCurrency": 1
  }'
```

Créer une dépense globale :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/$TRIP_ID/expenses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Essence mutualisée",
    "date": "2026-08-03",
    "payerPersonId": "<person-id-payeur>",
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

Créer une dépense avancée avec participant·es manuel·les :

```bash
curl -s -X POST http://localhost:8080/api/v1/trips/$TRIP_ID/expenses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Activité optionnelle",
    "date": "2026-08-05",
    "payerPersonId": "<person-id-payeur>",
    "totalAmount": 80,
    "meatAmount": 0,
    "alcoholAmount": 0,
    "customConstraintAmounts": {},
    "type": "NORMAL",
    "advancedMode": true,
    "manualParticipantIds": ["<person-id-1>", "<person-id-2>"],
    "currency": "EUR",
    "exchangeRateToTripCurrency": 1
  }'
```

Lister, modifier et supprimer les dépenses :

```bash
curl -s http://localhost:8080/api/v1/trips/$TRIP_ID/expenses \
  -H "Authorization: Bearer $TOKEN"

curl -s -X PUT http://localhost:8080/api/v1/trips/$TRIP_ID/expenses/$EXPENSE_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ ... même forme que la création ... }'

curl -i -X DELETE http://localhost:8080/api/v1/trips/$TRIP_ID/expenses/$EXPENSE_ID \
  -H "Authorization: Bearer $TOKEN"
```

## Résumé, audit et exports

```bash
curl -s http://localhost:8080/api/v1/trips/$TRIP_ID/summary \
  -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8080/api/v1/trips/$TRIP_ID/audit-logs \
  -H "Authorization: Bearer $TOKEN"

curl -OJ http://localhost:8080/api/v1/trips/$TRIP_ID/exports/expenses.csv \
  -H "Authorization: Bearer $TOKEN"

curl -OJ http://localhost:8080/api/v1/trips/$TRIP_ID/exports/summary.csv \
  -H "Authorization: Bearer $TOKEN"
```

## Format d’erreur

Les erreurs contrôlées utilisent une structure proche de :

```json
{
  "timestamp": "2026-07-06T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "details": ["Message d'erreur métier"]
}
```

Codes fréquents :

```text
400 : validation ou incohérence métier
401 : authentification absente ou session expirée
403 : rôle insuffisant ou accès interdit
429 : trop de tentatives sur login/register
500 : erreur inattendue
```

# Modèle de sécurité — bêta fermée

## Principe

La bêta utilise une authentification par token de session stocké côté backend. Le token est renvoyé à l'inscription ou à la connexion puis envoyé par l'interface via :

```http
Authorization: Bearer <token>
```

Ce n'est pas encore un JWT de production, mais les règles d'accès métier sont appliquées côté backend.

## Rôles de voyage

| Rôle | Lecture | Dépenses | Guests | Contraintes | Invitations |
|---|---:|---:|---:|---:|---:|
| OWNER | oui | oui | oui | oui | oui |
| ADMIN | oui | oui | oui | oui | oui |
| PARTICIPANT | oui | oui | non | non | non |
| READ_ONLY | oui | non | non | non | non |

## RAV privé

Si une personne est liée à un compte et que son RAV est privé :

- le compte propriétaire voit son RAV ;
- les autres comptes membres voient `livingRestHidden = true` ;
- le champ `livingRest` est renvoyé à `null` pour les autres ;
- le moteur de calcul utilise quand même le vrai RAV côté backend.

La confidentialité est donc une confidentialité d'affichage et d'API, pas une impossibilité mathématique d'inférence.

## Guests

Un guest est une personne non liée à un compte. Un compte peut se lier à un guest si :

- il est membre du voyage ou rejoint le voyage par invitation ;
- le guest n'est pas déjà lié à un autre compte ;
- le compte n'est pas déjà lié à une autre personne du même voyage.

La liaison ne remplace pas automatiquement les données du guest. L'application doit demander explicitement si le profil doit être appliqué.

## Endpoints à protéger systématiquement

- `/api/v1/trips/**`
- `/api/v1/trips/{tripId}/persons/**`
- `/api/v1/trips/{tripId}/expenses/**`
- `/api/v1/trips/{tripId}/summary`
- `/api/v1/trips/{tripId}/audit-logs`
- `/api/v1/trips/{tripId}/exports/**`
- `/api/v1/auth/profile/**`

Les endpoints publics sont limités à :

- `/api/v1/auth/register`
- `/api/v1/auth/login`
- `/api/v1/health`
- `/api/v1/health/readiness`
- `/app/**`

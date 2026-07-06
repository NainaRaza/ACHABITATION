# Exports CSV

Deux exports sont disponibles dans l'API et dans l'interface web :

```http
GET /api/v1/trips/{tripId}/exports/expenses.csv
GET /api/v1/trips/{tripId}/exports/summary.csv
```

Ils nécessitent l'en-tête d'authentification :

```http
Authorization: Bearer <token>
```

Les exports sont générés en UTF-8 avec séparateur `;`, adapté à un usage courant dans Excel en configuration française.

# Exports CSV

Deux exports CSV sont disponibles dans l’API, le frontend web et le client Android.

```http
GET /api/v1/trips/{tripId}/exports/expenses.csv
GET /api/v1/trips/{tripId}/exports/summary.csv
```

Ils nécessitent une session valide :

```http
Authorization: Bearer <accessToken>
```

## Format

- encodage : UTF-8 ;
- séparateur : `;` ;
- type de contenu : `text/csv` ;
- header exposé par CORS : `Content-Disposition`.

Ce format est adapté à un usage courant dans Excel ou LibreOffice en configuration française.

## Export dépenses

L’export dépenses contient les dépenses du voyage, avec les informations utiles au contrôle : date, titre, payeur, montants, type, devise, taux de conversion et montants spécifiques.

## Export résumé

L’export résumé contient les soldes et les remboursements suggérés, dans la devise de référence du voyage.

## Limites actuelles

- Pas d’export PDF natif.
- Pas d’export Excel `.xlsx` natif.
- Sur Android, le CSV est affiché sous forme de texte copiable ; l’enregistrement via Storage Access Framework reste à ajouter.

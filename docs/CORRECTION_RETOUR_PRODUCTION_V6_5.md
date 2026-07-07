# Correction retour production — V6.5

Base traitée : `achabitation-refonte-stabilisation-v6-4-depense-mutualisee-contraintes.zip`.

## Corrections P0 appliquées

### 1. Dépenses mutualisées voyage et contraintes personnalisées

Le bug signalé était réel : `ExpenseService.apply` vidait `customConstraintAmounts`, puis ne persistait les montants personnalisés que pour les dépenses `NORMAL`.

Correction appliquée : les montants de contraintes personnalisées sont maintenant persistés pour les dépenses datées comme pour les dépenses mutualisées voyage.

Test ajouté : `globalExpensePersistsCustomConstraintAmountsAndUsesThemInSummary`.

Ce test vérifie :

- création d'une dépense `GLOBAL` ;
- persistance de `customConstraintAmounts` ;
- conservation à la relecture API ;
- prise en compte dans le résumé.

### 2. Fuite d'email dans les audits

`TripService.linkGuestToUser` écrivait l'email brut du compte lié dans la description d'audit.

Corrections appliquées :

- suppression de l'email brut dans la description ;
- remplacement par une formulation neutre ;
- restriction de `AuditQueryService.list` à `requireAdmin` au lieu de `requireReadable` ;
- test de non-régression : un participant ne peut plus lire les audit logs et l'email lié n'apparaît pas dans la réponse owner/admin.

### 3. Réponses d'erreur production

`ApiExceptionHandler` exposait des détails techniques sur les contraintes DB et les erreurs 500.

Corrections appliquées :

- réponse générique pour `DataIntegrityViolationException` ;
- réponse générique pour erreurs 500 ;
- ajout du `requestId` dans la réponse d'erreur ;
- log serveur complet conservé avec le `requestId`.

### 4. CI / reproductibilité front

Le job Playwright utilisait encore `npm install`.

Correction appliquée : passage à `npm ci` avec cache basé sur `frontend-web/package-lock.json`.

## Corrections P1 appliquées

### 1. Validations serveur financières

Ajout d'annotations Bean Validation sur :

- montants de dépenses ;
- parts viande / alcool / contraintes ;
- devise ISO 4217 ;
- champs financiers de personnes ;
- champs financiers du profil utilisateur ;
- nombre maximum de contraintes et participants manuels.

Ajout d'un test API pour rejet d'une dépense négative.

### 2. Contraintes DB PostgreSQL

Ajout de la migration :

```text
backend-api/src/main/resources/db/migration/V4__production_validation_constraints.sql
```

Elle ajoute des `CHECK` PostgreSQL sur :

- montants non négatifs ;
- montant de dépense strictement positif ;
- taux de change positif ;
- devise en trois lettres majuscules ;
- dates de voyage / présence cohérentes.

### 3. Test optionnel PostgreSQL réel

Ajout de :

```text
backend-api/src/test/java/fr/achabitation/api/PostgresIntegrationSmokeTest.java
```

Le test utilise Testcontainers PostgreSQL et reste désactivé par défaut pour ne pas casser les environnements sans Docker.

Commande :

```bash
cd backend-api
ACHABITATION_POSTGRES_IT=true ./mvnw test -Dtest=PostgresIntegrationSmokeTest
```

## Points restant non faisables ici

- SMTP réel bout en bout ;
- rate limiting distribué Redis/gateway ;
- reverse proxy HTTPS réel ;
- restauration PostgreSQL réelle ;
- monitoring/alerting externe ;
- scans sécurité Maven/npm/Gradle/Docker réellement exécutés ;
- passage web de `localStorage` vers cookie `HttpOnly` ;
- build Android release signé / AAB / Play Store.

## Validation locale dans cet environnement

Validé :

- `frontend-web/run-tests.sh` ;
- `node --check` sur les fichiers JavaScript ;
- parsing XML/YAML des fichiers modifiés ;
- packaging ZIP.

Non validé ici :

- `./mvnw clean test`, car Maven doit être téléchargé depuis `repo.maven.apache.org` et le DNS est indisponible dans cet environnement ;
- Testcontainers PostgreSQL ;
- Gradle Android ;
- Playwright navigateur réel.

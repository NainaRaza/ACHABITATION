# Tests automatisés backend

Ce document décrit les tests du backend Spring Boot.

## Commande principale

Depuis `achabitation-refonte/backend-api` :

```bash
./mvnw clean test
```

Sous Windows :

```bat
mvnw.cmd clean test
```

## Base utilisée en test

Les tests utilisent le profil Spring `test` et une base H2 en mémoire :

```text
jdbc:h2:mem:achabitation-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

Ils ne modifient pas la base locale de développement :

```text
backend-api/data/achabitation.mv.db
```

## Tests unitaires purs

- `BalanceCalculatorTest` : calcul RAV, poids moyen, exclusions, dépenses normales/globales/avancées, devises, arrondis, remboursements et validations bloquantes.
- `PresencePeriodTest` : validation des bornes de présence.
- `EntityMapperTest` : normalisation des noms, arrondi monétaire, taux par défaut et masquage du RAV privé.
- `AuthServiceTest` : inscription, connexion, normalisation email, hash de mot de passe, rejet des doublons et profil utilisateur.

## Tests d’intégration API

`AchabitationApiIntegrationTest` couvre notamment :

- scénario bêta complet : compte, voyage, personnes, dépenses, résumé, exports et audit ;
- authentification obligatoire ;
- rejet explicite des tokens expirés ;
- invalidation serveur du token au logout ;
- refus d’accès pour un utilisateur non membre sur personnes, exports, audit logs et invitations ;
- invitations et adhésion à un voyage ;
- liaison guest ↔ compte sans écrasement automatique ;
- application explicite du profil à une personne liée ;
- confidentialité du RAV ;
- droits `OWNER`, `ADMIN`, `PARTICIPANT`, `READ_ONLY` ;
- validations serveur : dates, périodes, doublons, contraintes et dépenses incohérentes.

## Smoke test API

Après lancement du backend :

```bash
./mvnw spring-boot:run
```

Dans un autre terminal, depuis `achabitation-refonte` :

```bash
./scripts/smoke-test.sh
```

Sous PowerShell :

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

Le smoke test crée un compte, un voyage, des participant·es, des dépenses, un résumé, une invitation, un second compte, des exports CSV, vérifie l’audit puis contrôle que le logout invalide le token.

## Résultat attendu

```text
BUILD SUCCESS
Smoke test OK
```

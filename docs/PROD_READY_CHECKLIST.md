# ACHABITATION — Correctifs V1 / préparation production

Ce document liste les changements ajoutés pour rapprocher le projet d'une V1 exploitable hors prototype.

## Sécurité et droits

Le backend impose maintenant une authentification par token de session sur les routes applicatives sensibles. Le token est envoyé par l'interface web via l'en-tête :

```http
Authorization: Bearer <token>
```

Les règles appliquées sont :

- un utilisateur non connecté ne peut pas créer de voyage ;
- la liste des voyages ne renvoie que les voyages dont l'utilisateur est membre ;
- les données d'un voyage ne sont accessibles qu'aux membres du voyage ;
- les contraintes du voyage et les invitations sont réservées aux rôles `OWNER` et `ADMIN` ;
- la création de participant·es guest est réservée aux rôles `OWNER` et `ADMIN` ;
- la modification d'une personne liée à un compte est réservée à ce compte ou aux administrateurs, avec protection du profil financier ;
- la désactivation d'une personne est réservée aux rôles `OWNER` et `ADMIN` ;
- les dépenses et résumés exigent au minimum l'appartenance au voyage.

Les rôles reconnus sont :

```text
OWNER
ADMIN
PARTICIPANT
READ_ONLY
```

## Invitations

Ajout d'un système d'invitations :

```http
POST   /api/v1/trips/{tripId}/invitations
GET    /api/v1/trips/{tripId}/invitations
DELETE /api/v1/trips/{tripId}/invitations/{invitationId}
POST   /api/v1/trips/{tripId}/join
```

Un utilisateur non membre doit fournir un `invitationCode` pour rejoindre un voyage.

## Export CSV

Ajout de deux exports :

```http
GET /api/v1/trips/{tripId}/exports/expenses.csv
GET /api/v1/trips/{tripId}/exports/summary.csv
```

L'interface web expose ces exports dans l'onglet Résumé.

## Base de données production

Le profil `prod` est prévu pour PostgreSQL :

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://localhost:5432/achabitation
DATABASE_USER=achabitation
DATABASE_PASSWORD=achabitation
```

En prod :

```yaml
spring.jpa.hibernate.ddl-auto: validate
spring.flyway.enabled: true
```

Le schéma initial est versionné dans :

```text
backend/src/main/resources/db/migration/V1__initial_schema.sql
```

Le profil local reste en H2 avec `ddl-auto=update` pour ne pas casser le développement.

## Docker

Ajout :

```text
backend/Dockerfile
docker-compose.yml
```

Lancement local PostgreSQL + backend :

```bash
docker compose up --build
```

## Limites restantes avant vraie production publique

Les points suivants restent à traiter avant exposition à des utilisateurs réels :

- remplacement du token de session maison par JWT ou session serveur durcie ;
- expiration/rotation avancée des sessions ;
- emails transactionnels pour invitations et réinitialisation de mot de passe ;
- suppression de compte et export RGPD complet ;
- sauvegardes automatisées et procédure de restauration testée ;
- durcissement CORS/CSRF selon le mode de déploiement ;
- observabilité : logs structurés, métriques, alerting ;
- tests de charge et tests de concurrence.

# Backend API ACHABITATION

`backend-api/` contient le serveur applicatif ACHABITATION : API REST, sécurité, règles métier, persistance, exports CSV, audit et tests backend.

Le backend est la source de vérité métier. Les clients web et Android ne doivent pas recalculer les soldes : ils saisissent les données et consomment les endpoints REST.

## Prérequis

- Java 21.
- Maven Wrapper fourni : `mvnw` et `mvnw.cmd`.

## Lancement local

Linux/macOS :

```bash
./mvnw spring-boot:run
```

Windows :

```bat
mvnw.cmd spring-boot:run
```

URLs exposées :

```text
API        : http://localhost:8080/api/v1
Health     : http://localhost:8080/api/v1/health
Readiness  : http://localhost:8080/api/v1/health/readiness
H2 console : http://localhost:8080/h2-console
```

Connexion H2 locale :

```text
JDBC URL : jdbc:h2:file:./data/achabitation;AUTO_SERVER=TRUE;MODE=PostgreSQL
User     : sa
Password : vide
```

La base locale est stockée dans `backend-api/data/`.

## Profil local et profil production

Le profil par défaut utilise H2 avec `ddl-auto=update` et Flyway désactivé.

Le profil `prod` utilise PostgreSQL, Flyway et `ddl-auto=validate` :

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/achabitation"
$env:DATABASE_USER="achabitation"
$env:DATABASE_PASSWORD="achabitation"
./mvnw spring-boot:run
```

Le schéma initial de production se trouve dans :

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

## Docker

Depuis la racine du projet :

```bash
docker compose -f infra/docker-compose.yml up --build
```

Ce mode lance PostgreSQL 16 et le backend avec `SPRING_PROFILES_ACTIVE=prod`.

## Tests backend

```bash
./mvnw clean test
```

Sous Windows :

```bat
mvnw.cmd clean test
```

Voir aussi `TESTS.md`.

## Sécurité bêta

Routes publiques :

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/health/**
GET  /h2-console/** en développement local
```

Les autres routes `/api/v1/**` exigent :

```http
Authorization: Bearer <accessToken>
```

Le token est opaque. Le backend renvoie le token brut à l’inscription ou à la connexion, puis stocke uniquement son hash SHA-256 dans `app_user.session_token_hash`. La durée de validité actuelle côté filtre est de 30 jours à partir de `session_token_issued_at`.

La déconnexion invalide la session côté serveur :

```http
POST /api/v1/auth/logout
```

`/register` et `/login` sont protégés par un rate limiting mémoire : 12 tentatives par IP et par action sur une fenêtre de 10 minutes. Ce mécanisme convient à une bêta locale ; il doit être remplacé par une solution distribuée si l’application est exposée publiquement.

## CORS local

Le backend autorise les origines de développement suivantes :

```text
http://localhost:5173
http://127.0.0.1:5173
```

La configuration est dans `src/main/java/fr/achabitation/config/SecurityConfig.java`.

## Organisation interne

```text
src/main/java/fr/achabitation/api             contrôleurs REST et DTO
src/main/java/fr/achabitation/application     services applicatifs
src/main/java/fr/achabitation/config          sécurité, CORS, filtre token
src/main/java/fr/achabitation/domain          moteur métier pur
src/main/java/fr/achabitation/infrastructure  entités JPA et repositories
src/main/resources                            configuration Spring et migrations
src/test                                      tests automatisés
```

## Principaux endpoints

Authentification et profil :

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET  /api/v1/auth/profile
PUT  /api/v1/auth/account
PUT  /api/v1/auth/profile
POST /api/v1/auth/profile/apply-to-linked-persons
```

Voyages et invitations :

```text
POST   /api/v1/trips
GET    /api/v1/trips
POST   /api/v1/trips/join-by-code
PUT    /api/v1/trips/{tripId}/constraints
POST   /api/v1/trips/{tripId}/join
POST   /api/v1/trips/{tripId}/invitations
GET    /api/v1/trips/{tripId}/invitations
DELETE /api/v1/trips/{tripId}/invitations/{invitationId}
```

Personnes :

```text
POST   /api/v1/trips/{tripId}/persons
POST   /api/v1/trips/{tripId}/persons/current-user
GET    /api/v1/trips/{tripId}/persons
PUT    /api/v1/trips/{tripId}/persons/{personId}
POST   /api/v1/trips/{tripId}/persons/{personId}/link-current-user
DELETE /api/v1/trips/{tripId}/persons/{personId}
```

Dépenses, résumé, audit et exports :

```text
POST   /api/v1/trips/{tripId}/expenses
GET    /api/v1/trips/{tripId}/expenses
PUT    /api/v1/trips/{tripId}/expenses/{expenseId}
DELETE /api/v1/trips/{tripId}/expenses/{expenseId}
GET    /api/v1/trips/{tripId}/summary
GET    /api/v1/trips/{tripId}/audit-logs
GET    /api/v1/trips/{tripId}/exports/expenses.csv
GET    /api/v1/trips/{tripId}/exports/summary.csv
```

## Endpoint d’ajout du compte courant comme personne

```http
POST /api/v1/trips/{tripId}/persons/current-user
```

Cette route crée une personne directement liée au compte authentifié. Si `applyProfileToPerson=true`, les données du profil utilisateur sont copiées vers la personne créée. Sinon, la personne est créée en mode `AVERAGE`, ce qui permet un ajout valide même si le profil ne contient pas encore de RAV exploitable.

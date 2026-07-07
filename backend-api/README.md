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
OpenAPI    : http://localhost:8080/v3/api-docs
Swagger UI : http://localhost:8080/swagger-ui.html
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
$env:DATABASE_PASSWORD="mot-de-passe-local-fort"
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

Ce mode lance PostgreSQL 16 et le backend avec `SPRING_PROFILES_ACTIVE=prod`. Il nécessite des variables définies dans un fichier `.env` local basé sur `.env.example`; aucun mot de passe de production ne doit être codé en dur.

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

Les autres routes `/api/v1/**` exigent une session valide.

Pour le web, la session passe par le cookie `ACHABITATION_SESSION` `HttpOnly` et les requêtes mutantes authentifiées par cookie exigent le couple CSRF `XSRF-TOKEN` / `X-XSRF-TOKEN`. Le client web envoie `X-Achabitation-Client: web`; dans ce cas, le champ `accessToken` est masqué dans la réponse JSON.

Pour Android et les scripts, la session passe par :

```http
Authorization: Bearer <accessToken>
```

Le token est opaque. Le backend stocke uniquement son hash côté serveur. La durée de validité actuelle est de 30 jours.

La déconnexion invalide la session côté serveur :

```http
POST /api/v1/auth/logout
```

Le changement de mot de passe connecté force une rotation du token et invalide l'ancien token opaque :

```http
PUT /api/v1/auth/password
```

L'export de compte et la suppression/anonymisation sont disponibles pour préparer le socle RGPD :

```http
GET    /api/v1/auth/export
DELETE /api/v1/auth/account
```

`/register` et `/login` sont protégés par un rate limiting mémoire : 12 tentatives par IP et par action sur une fenêtre de 10 minutes. Ce mécanisme convient à une bêta locale ; il doit être remplacé par une solution distribuée si l’application est exposée publiquement.

## CORS local

Le backend autorise les origines de développement suivantes :

```text
http://localhost:5173
http://127.0.0.1:5173
```

La configuration est dans `src/main/java/fr/achabitation/config/SecurityConfig.java`. Les routes API fonctionnent en mode stateless : le `SecurityContext` est alimenté par le filtre de token opaque.

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
POST   /api/v1/auth/logout
GET    /api/v1/auth/profile
PUT    /api/v1/auth/account
PUT    /api/v1/auth/password
GET    /api/v1/auth/export
DELETE /api/v1/auth/account
PUT    /api/v1/auth/profile
POST   /api/v1/auth/profile/apply-to-linked-persons
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

## OpenAPI / Swagger

La documentation OpenAPI est générée en profil local via Springdoc :

```text
GET /v3/api-docs
GET /swagger-ui.html
```

En profil `prod`, Swagger UI et `/v3/api-docs` sont désactivés par défaut dans `application-prod.yml`. Pour exporter un contrat API versionné, lancer le backend en profil local/test puis sauvegarder la réponse de `/v3/api-docs` dans un fichier `openapi.json`.

## Observabilité minimale

Chaque requête reçoit ou propage un en-tête `X-Request-ID`. Ce même identifiant est placé dans les logs via le MDC afin de corréler une erreur utilisateur avec les traces serveur sans journaliser de token ni de RAV sensible.

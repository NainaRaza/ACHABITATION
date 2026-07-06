# Backend API ACHABITATION

Ce dossier contient uniquement le serveur applicatif ACHABITATION : API REST, sécurité, règles métier, persistance, exports et tests backend.

L’interface web a été sortie dans `../frontend-web`. Les futures applications Android et iOS devront consommer la même API REST.

## Lancement local

```bash
mvn spring-boot:run
```

API :

```text
http://localhost:8080/api/v1
```

Console H2 :

```text
http://localhost:8080/h2-console
```

JDBC URL H2 :

```text
jdbc:h2:file:./data/achabitation;AUTO_SERVER=TRUE;MODE=PostgreSQL
```

User : `sa`  
Password : vide

## Tests

```bash
mvn clean test
```

Voir aussi :

```text
TESTS.md
../docs/MVP_BETA_CHECKLIST.md
```

## Frontend web séparé

Le backend ne sert plus `/app`. Pour lancer l’interface :

```bash
cd ../frontend-web
./run-web.sh
```

ou sous Windows :

```bat
cd ..\frontend-web
run-web.bat
```

Puis ouvrir :

```text
http://localhost:5173
```

Le CORS du backend autorise le frontend local sur :

```text
http://localhost:5173
http://127.0.0.1:5173
```

## Profil production expérimental

Le profil local reste basé sur H2. Le profil `prod` utilise PostgreSQL et Flyway :

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/achabitation"
$env:DATABASE_USER="achabitation"
$env:DATABASE_PASSWORD="achabitation"
mvn spring-boot:run
```

## Docker

Depuis la racine du projet :

```bash
docker compose -f infra/docker-compose.yml up --build
```

## Organisation interne

```text
src/main/java/fr/achabitation/api             Contrôleurs REST + DTO
src/main/java/fr/achabitation/application     Services applicatifs
src/main/java/fr/achabitation/config          Sécurité, CORS, configuration
src/main/java/fr/achabitation/domain          Calculs et règles métier pures
src/main/java/fr/achabitation/infrastructure  Entités JPA et repositories
src/main/resources/db/migration               Migrations Flyway
src/test                                      Tests automatisés
```

## Endpoint d'ajout direct du compte courant comme personne

Le backend expose :

```http
POST /api/v1/trips/{tripId}/persons/current-user
```

Cette route crée une `PersonEntity` directement liée au compte authentifié. Elle sert au parcours web **+ M’ajouter moi-même** et au choix **Autre personne** après invitation.

Si `applyProfileToPerson` vaut `true`, les données du profil utilisateur sont copiées vers la personne créée. Sinon, la personne est créée en mode `AVERAGE`, ce qui permet de rester valide même si le profil ne contient pas encore de RAV exploitable.

# ACHABITATION — partage de dépenses au reste à vivre

ACHABITATION est une application de partage de dépenses fondée sur le reste à vivre, abrégé RAV. Elle permet de répartir les frais d’un voyage ou d’un séjour en tenant compte de la capacité contributive des participant·es, de leurs dates de présence et de contraintes comme végétarien, sans alcool ou contraintes personnalisées.

Le projet est organisé autour d’un backend API commun. Le web et Android consomment la même API REST ; les calculs métier restent côté backend afin d’éviter les divergences entre clients.

## Statut du projet

Le dépôt correspond à un MVP bêta local avancé :

- backend Spring Boot fonctionnel avec authentification, persistance, règles métier, exports CSV, audit et tests ;
- frontend web HTML/CSS/JavaScript refactoré en modules ;
- application Android Kotlin / Jetpack Compose fonctionnelle ;
- dossier iOS encore préparatoire ;
- ancien client Swing conservé uniquement comme référence historique.

Ce n’est pas encore une version production publique. Les points à durcir sont listés dans `docs/PROD_READY_CHECKLIST.md`.

## Structure du dépôt

```text
achabitation-refonte/
├── backend-api/       API Spring Boot, sécurité, persistance, calculs, exports, tests
├── frontend-web/      interface web locale HTML/CSS/JavaScript consommant l'API
├── mobile-android/    client Android natif Kotlin / Jetpack Compose
├── mobile-ios/        emplacement réservé au futur client iOS
├── desktop-legacy/    ancienne application Java Swing conservée comme référence
├── docs/              documentation fonctionnelle, technique, sécurité et API
├── infra/             docker-compose PostgreSQL + backend-api
└── scripts/           smoke tests transverses
```

## Prérequis utiles

- Java 21 pour le backend.
- Node.js pour les tests frontend.
- Android Studio pour le client Android.
- Docker pour lancer PostgreSQL + backend via `infra/docker-compose.yml`.

## Lancer le backend en local

```bash
cd backend-api
./mvnw spring-boot:run
```

Sous Windows :

```bat
cd backend-api
mvnw.cmd spring-boot:run
```

URLs locales :

```text
API        : http://localhost:8080/api/v1
Health     : http://localhost:8080/api/v1/health
Readiness  : http://localhost:8080/api/v1/health/readiness
OpenAPI    : http://localhost:8080/v3/api-docs
Swagger UI : http://localhost:8080/swagger-ui.html
H2 console : http://localhost:8080/h2-console
```

Base H2 locale de développement :

```text
backend-api/data/achabitation.mv.db
```

## Lancer le frontend web

Terminal 1 : backend lancé sur `localhost:8080`.

Terminal 2 :

```bash
cd frontend-web
./run-web.sh
```

Sous Windows :

```bat
cd frontend-web
run-web.bat
```

Puis ouvrir :

```text
http://localhost:5173
```

Le frontend appelle par défaut :

```text
http://localhost:8080/api/v1
```

Cette URL peut être surchargée par `window.ACHABITATION_API_BASE_URL` ou par `localStorage`.

## Lancer Android

Le backend doit être lancé au préalable.

Depuis Android Studio, ouvrir :

```text
mobile-android/
```

Sur émulateur Android, l’URL API par défaut est :

```text
http://10.0.2.2:8080/api/v1
```

Sur téléphone physique, utiliser l’adresse IP locale du PC qui exécute le backend, par exemple :

```text
http://192.168.1.42:8080/api/v1
```

Commandes Gradle utiles :

```bash
cd mobile-android
./gradlew clean assembleDebug
```

Sous Windows :

```bat
cd mobile-android
gradlew.bat clean assembleDebug
```

## Validation globale

Depuis la racine du dépôt :

```bash
./scripts/validate-all.sh
```

Sous PowerShell :

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\validate-all.ps1
```

Cette commande exécute les tests backend, les tests frontend, les tests unitaires Android et le build `assembleDebug`.

## Docker / PostgreSQL

Depuis la racine du dépôt :

```bash
docker compose -f infra/docker-compose.yml up --build
```

Ce mode lance PostgreSQL 16 et `backend-api` avec le profil Spring `prod`. Il nécessite un fichier `.env` local basé sur `.env.example`; les secrets de production ne doivent pas être codés dans le dépôt.

## Tests

Backend :

```bash
cd backend-api
./mvnw clean test
```

Frontend :

```bash
cd frontend-web
./run-tests.sh
```

Tests navigateur Playwright, après installation npm et navigateurs :

```bash
cd frontend-web
npm install
npx playwright install chromium
npm run test:e2e
```

Android :

```bash
cd mobile-android
./gradlew testDebugUnitTest assembleDebug
```

Smoke test API, backend déjà lancé :

```bash
./scripts/smoke-test.sh
```

Sous PowerShell :

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

## Documentation principale

```text
docs/ARCHITECTURE_REFONTE.md
docs/SPECIFICATION_FONCTIONNELLE_TECHNIQUE.md
docs/DOSSIER_TECHNIQUE_JAVA.md
docs/API_EXEMPLES.md
docs/API_OPENAPI.md
docs/SECURITY_BETA_MODEL.md
docs/MVP_BETA_CHECKLIST.md
docs/PROD_READY_CHECKLIST.md
docs/FRONT_WEB_REFACTOR.md
docs/INTERFACE_WEB_LOCALE.md
docs/V1_AUTH_PROFIL_CONTRAINTES.md
docs/EXPORTS_CSV.md
docs/BACKUP_RESTORE.md
docs/RGPD_DONNEES_SENSIBLES.md
docs/OBSERVABILITE.md
docs/RESTE_A_FAIRE_ET_EVOLUTIONS.md
```

## Intégration continue

Le workflow GitHub Actions situé dans `.github/workflows/ci.yml` lance désormais les tests backend avec Java 21, les tests frontend Node.js avec Node.js 22, les tests navigateur Playwright, puis les tests unitaires Android et `assembleDebug` avec Gradle.

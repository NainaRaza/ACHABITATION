# ACHABITATION - Gestion finance - partage au RAV

ACHABITATION est une application de partage de dépenses au reste à vivre. Le projet a été refondu pour séparer clairement le serveur, le client web et les futurs clients mobiles.

L’objectif architectural est le suivant : un backend API commun porte les règles métier et les données ; chaque interface — web, Android, iOS — consomme cette API sans dupliquer les calculs.

## Structure du dépôt

```text
achabitation-refonte/
├── backend-api/       API Spring Boot, sécurité, persistance, calculs, exports
├── frontend-web/      interface web HTML/CSS/JS consommant backend-api
├── mobile-android/    dossier préparatoire pour la future application Android
├── mobile-ios/        dossier préparatoire pour la future application iOS
├── desktop-legacy/    ancienne application Swing conservée comme référence
├── docs/              spécifications, architecture, sécurité, dossier technique
├── infra/             docker-compose PostgreSQL + backend-api
└── scripts/           smoke tests et scripts transverses
```

## Backend API

Le backend est une API Java Spring Boot organisée en couches :

```text
api/             contrôleurs REST et DTO
application/     services applicatifs
config/          configuration sécurité et CORS
domain/          moteur métier pur, indépendant de Spring
infrastructure/  entités JPA et repositories
```

Le moteur de calcul RAV est dans :

```text
backend-api/src/main/java/fr/achabitation/domain
```

Il ne dépend pas de Spring. C’est volontaire : il peut être testé isolément et réutilisé par le backend cloud, une interface mobile ou d’autres clients.

## Lancer en local

Terminal 1 : lancer l’API.

```bash
cd backend-api
mvn spring-boot:run
```

Par défaut :

```text
API        : http://localhost:8080/api/v1
H2 console : http://localhost:8080/h2-console
```

Base de données locale de développement :

```text
backend-api/data/achabitation.mv.db
```

Terminal 2 : lancer l’interface web.

Windows :

```bat
cd frontend-web
run-web.bat
```

Linux/macOS :

```bash
cd frontend-web
./run-web.sh
```

Puis ouvrir :

```text
http://localhost:5173
```

## Frontend web

Le frontend web n’est plus servi par Spring Boot. Il vit dans `frontend-web/` et appelle l’API :

```text
http://localhost:8080/api/v1
```

Ce découpage permet d’ajouter plus tard Android et iOS sans modifier l’API métier.

## Mobile

Les dossiers suivants sont préparés mais volontairement vides :

```text
mobile-android/
mobile-ios/
```

Ils accueilleront de futurs clients mobiles qui consommeront les mêmes endpoints REST que `frontend-web`.

## Docker / PostgreSQL

Le fichier Docker Compose est dans :

```text
infra/docker-compose.yml
```

Depuis la racine :

```bash
docker compose -f infra/docker-compose.yml up --build
```

Ce mode lance PostgreSQL et `backend-api` avec le profil `prod`.

## Tests

Tests backend :

```bash
cd backend-api
mvn clean test
```

Smoke test API, backend lancé :

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

ou :

```bash
./scripts/smoke-test.sh
```

## Documentation principale

```text
docs/ARCHITECTURE_REFONTE.md
docs/SPECIFICATION_FONCTIONNELLE_TECHNIQUE.md
docs/DOSSIER_TECHNIQUE_JAVA.md
docs/INTERFACE_WEB_LOCALE.md
docs/MVP_BETA_CHECKLIST.md
docs/SECURITY_BETA_MODEL.md
docs/PROD_READY_CHECKLIST.md
```

## Statut

Le projet est un POC avancé / MVP bêta local. Il est structuré pour évoluer vers une architecture multi-client : web, Android, iOS.

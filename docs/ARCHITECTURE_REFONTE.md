# Architecture refondue ACHABITATION

## Objectif

La refonte architecturale sépare le projet en plusieurs applications distinctes :

```text
backend-api      serveur central et règles métier
frontend-web     client web
mobile-android   futur client Android
mobile-ios       futur client iOS
```

Le principe est que le backend porte la vérité métier et expose une API REST. Les interfaces ne doivent pas dupliquer les calculs ni les droits : elles consomment les endpoints du backend.

## Structure cible du dépôt

```text
achabitation-refonte/
├── backend-api/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/fr/achabitation/
│   │   ├── api/
│   │   ├── application/
│   │   ├── config/
│   │   ├── domain/
│   │   └── infrastructure/
│   ├── src/main/resources/
│   └── src/test/
│
├── frontend-web/
│   ├── index.html
│   ├── app.js
│   ├── styles.css
│   ├── run-web.bat
│   └── run-web.sh
│
├── mobile-android/
│   └── README.md
│
├── mobile-ios/
│   └── README.md
│
├── desktop-legacy/
│   └── ancienne application Swing
│
├── docs/
│   └── documentation projet
│
├── infra/
│   └── docker-compose.yml
│
└── scripts/
    └── smoke tests
```

## Flux général

```text
Navigateur web
    ↓ HTTP REST
backend-api
    ↓ JPA
Base H2 ou PostgreSQL
```

Plus tard :

```text
Android       ┐
iOS           ├── HTTP REST → backend-api → PostgreSQL
Frontend web  ┘
```

## Backend API

Le backend est une application Spring Boot. Il contient :

- l’authentification ;
- les rôles ;
- les invitations ;
- la gestion des voyages ;
- la gestion des personnes et guests ;
- les dépenses ;
- les contraintes de voyage ;
- le calcul RAV ;
- le résumé ;
- les remboursements ;
- les exports CSV ;
- l’historique ;
- les validations métier ;
- la persistance.

Il ne contient plus les fichiers de l’interface web.

## Frontend web

Le frontend web est une application HTML/CSS/JavaScript sans framework.

Il est volontairement séparé dans `frontend-web/`. Il appelle par défaut :

```text
http://localhost:8080/api/v1
```

Il ne doit pas contenir de règles métier critiques. Les validations côté écran ne sont là que pour améliorer l’expérience utilisateur.

## Mobile Android / iOS

Les dossiers `mobile-android/` et `mobile-ios/` sont préparés pour la suite.

Les applications mobiles devront consommer l’API REST de `backend-api`.

Règle importante : le calcul RAV ne doit pas être recodé côté mobile. Le backend doit rester l’autorité.

## Séparation des responsabilités

### Backend API

Responsable de :

- droits ;
- sécurité ;
- validation ;
- calcul ;
- données ;
- exports ;
- historique.

### Frontend web

Responsable de :

- affichage ;
- formulaires ;
- navigation ;
- messages utilisateur ;
- appels API.

### Mobile

Responsable de :

- expérience mobile ;
- appels API ;
- stockage local éventuel de session ;
- notifications futures éventuelles.

## Configuration locale

Backend :

```bash
cd backend-api
mvn spring-boot:run
```

Frontend :

```bash
cd frontend-web
./run-web.sh
```

URL frontend :

```text
http://localhost:5173
```

URL API :

```text
http://localhost:8080/api/v1
```

## Docker

La configuration Docker est dans :

```text
infra/docker-compose.yml
```

Lancement :

```bash
docker compose -f infra/docker-compose.yml up --build
```

## Avantages de la nouvelle architecture

- le backend devient un vrai serveur API ;
- le frontend web peut évoluer indépendamment ;
- Android et iOS pourront être ajoutés sans refondre le serveur ;
- les tests backend restent concentrés sur les règles métier ;
- le déploiement est plus clair ;
- la documentation distingue mieux serveur, clients et infra.

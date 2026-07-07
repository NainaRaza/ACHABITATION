# Corrections backend, front-web et Android après analyse

Ce document récapitule les corrections déjà appliquées au projet et les points qui restent à surveiller.

## Backend

### Build reproductible

`backend-api/` contient le Maven Wrapper :

```text
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
```

Commande standard :

```bash
./mvnw clean test
```

Le projet cible Java 21 et Spring Boot 3.5.15.

### Authentification

Le vocabulaire `devToken` a été remplacé par `accessToken` côté API native/scripts.

Le token est opaque. Le backend génère un token brut, puis stocke seulement son hash SHA-256. Le web n’utilise plus de stockage token JavaScript : il envoie `X-Achabitation-Client: web`, reçoit une session `ACHABITATION_SESSION` en cookie `HttpOnly` et envoie un jeton CSRF sur les requêtes mutantes.

Android et les scripts continuent d’utiliser :

```http
Authorization: Bearer <accessToken>
```

La déconnexion serveur existe :

```http
POST /api/v1/auth/logout
```

### Sécurité HTTP

Les routes publiques sont limitées à :

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/health/**
GET  /h2-console/** en développement local
```

Les routes `/api/v1/**` exigent une authentification Spring Security, complétée par les contrôles métier des services.

### Rate limiting

Un rate limiting mémoire protège `/register` et `/login` : 12 tentatives par IP et par action sur 10 minutes. Pour une production exposée, il faut le déplacer vers Redis, Bucket4j distribué, une gateway ou un WAF.

### Rôles et autorisations

Les rôles de voyage existent : `OWNER`, `ADMIN`, `PARTICIPANT`, `READ_ONLY`.

Règles actuelles : lecture pour les membres du voyage, écriture pour `OWNER/ADMIN/PARTICIPANT`, administration pour `OWNER/ADMIN`, modification du profil financier d’une personne liée par le compte lié ou un admin selon le cas.

### Exports et audit

Le backend expose :

```text
GET /api/v1/trips/{tripId}/exports/expenses.csv
GET /api/v1/trips/{tripId}/exports/summary.csv
GET /api/v1/trips/{tripId}/audit-logs
```

## Frontend web

### Découpage réel de `app.js`

`frontend-web/app.js` n’est plus monolithique. Il fait environ 50 lignes et installe les modules fonctionnels.

La logique est répartie dans :

```text
src/api.js
src/state.js
src/ui.js
src/utils.js
src/auth.js
src/trips.js
src/persons.js
src/expenses.js
src/summary.js
src/invitations.js
src/audit.js
src/profile.js
src/constraints.js
src/form-helpers.js
src/render.js
```

### Parcours couverts

Inscription, connexion, logout serveur, profil utilisateur, voyages, contraintes personnalisées, invitations, ajout du compte courant comme personne, rattachement d’un guest, personnes, dépenses, résumé, exports CSV et audit logs.

### Tests frontend

`frontend-web/run-tests.sh` vérifie la syntaxe de `app.js`, la syntaxe de tous les modules `src/*.js`, puis exécute `frontend-smoke.test.mjs` et `frontend-flow.test.mjs`.

Ces tests restent des tests Node sans navigateur réel. Ils ne remplacent pas des tests E2E type Playwright.

## Android

Le dossier `mobile-android/` contient désormais un client Android natif Kotlin / Jetpack Compose, et non un simple dossier préparatoire.

Corrections appliquées :

- Gradle Wrapper présent ;
- Android Gradle Plugin `8.7.3` ;
- Gradle `9.6.1` ;
- stockage de session via `EncryptedSharedPreferences` ;
- HTTP cleartext autorisé uniquement en variante debug ;
- variante release configurée avec `usesCleartextTraffic=false` ;
- refus des URL `http://` en release côté ViewModel/API ;
- écran “Session expirée” après HTTP 401 ;
- confirmations sur suppression de dépense, désactivation de personne et révocation d’invitation.

## Points encore fragiles

- CI Android présente dans `.github/workflows/ci.yml` et validée sur GitHub Actions.
- Pas de tests E2E web avec navigateur réel.
- Pas de refresh token ni rotation de session.
- Rate limiting non distribué.
- Pas encore de packaging production web complet.
- Exports Android affichés en texte copiable, sans Storage Access Framework.
- iOS reste à faire.

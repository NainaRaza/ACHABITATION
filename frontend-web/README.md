# Frontend web ACHABITATION

Ce dossier contient l’interface web locale séparée du backend. Le frontend est une application HTML/CSS/JavaScript sans framework qui consomme l’API REST exposée par `backend-api`.

## Lancement local

Terminal 1 : lancer l’API.

```bash
cd ../backend-api
./mvnw spring-boot:run
```

Terminal 2 : servir le frontend web.

Windows :

```bat
run-web.bat
```

Linux/macOS :

```bash
./run-web.sh
```

Puis ouvrir :

```text
http://localhost:5173
```

## Configuration API

Par défaut, le frontend appelle :

```text
http://localhost:8080/api/v1
```

La constante est définie dans `src/api.js` via `API_BASE_URL`. Elle peut être surchargée par :

```js
window.ACHABITATION_API_BASE_URL
```

ou par le localStorage :

```js
localStorage.setItem("achabitation.apiBaseUrl", "http://localhost:8080/api/v1")
```

## Organisation du code

```text
app.js              point d’entrée léger et installation des modules
src/api.js         appels HTTP, erreur 401, exports blob
src/state.js       état courant + localStorage
src/ui.js          messages utilisateur
src/utils.js       formatage dates/montants/contraintes
src/auth.js        connexion, inscription, compte, logout
src/profile.js     profil RAV et propagation aux personnes liées
src/trips.js       voyages, sélection, dashboard, exports
src/persons.js     personnes, guests, liaison compte, présences
src/expenses.js    dépenses, participants concernés, formulaires
src/summary.js     soldes et remboursements
src/invitations.js invitations et révocation
src/audit.js       historique
src/constraints.js contraintes personnalisées du voyage
src/form-helpers.js helpers de formulaires
src/render.js      navigation, rendu global, bind events
```

`app.js` est réduit à un point d’entrée. La logique fonctionnelle est répartie dans les modules `src/`.

## Parcours couverts

- inscription et connexion ;
- logout serveur ;
- session expirée ;
- compte et profil utilisateur ;
- création, sélection et adhésion à un voyage ;
- contraintes personnalisées du voyage ;
- invitations ;
- création de guests ;
- ajout direct du compte courant comme personne ;
- liaison d’un guest au compte courant ;
- dépenses datées, mutualisées voyage et avancées ;
- résumé des soldes ;
- exports CSV ;
- audit logs.

## Tests frontend

Deux tests JavaScript sans navigateur externe sont fournis.

Linux/macOS :

```bash
./run-tests.sh
```

Windows :

```bat
run-tests.bat
```

Le script exécute :

```text
node --check app.js
node --check playwright.config.mjs
node --check src/*.js
node --check tests/e2e/*.js
node tests/frontend-smoke.test.mjs
node tests/frontend-flow.test.mjs
```

Ces tests vérifient la syntaxe, quelques utilitaires et un parcours principal mocké.

Des tests navigateur Playwright sont également préparés :

```bash
npm install
npx playwright install chromium
npm run test:e2e
```

Ils couvrent les parcours inscription → voyage → participant → dépense → résumé, erreur de connexion, et logout serveur. Ils nécessitent l'installation des dépendances npm et des navigateurs Playwright ; ils ne sont donc pas exécutables dans un environnement sans accès Internet.

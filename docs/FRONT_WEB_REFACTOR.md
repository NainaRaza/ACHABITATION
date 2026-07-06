# Refonte du frontend web

## Constat initial

L’ancien `frontend-web/app.js` concentrait l’essentiel de la logique interface, API, état, formulaires et rendu. Cette organisation rendait les évolutions risquées et compliquait les tests.

## État actuel

`app.js` est maintenant un point d’entrée court. Il installe les modules et expose l’application pour les tests Node.

La logique fonctionnelle est répartie dans :

```text
src/api.js
src/state.js
src/ui.js
src/utils.js
src/auth.js
src/profile.js
src/trips.js
src/persons.js
src/expenses.js
src/summary.js
src/invitations.js
src/audit.js
src/constraints.js
src/form-helpers.js
src/render.js
```

## Responsabilités des modules

```text
api.js           configuration API, appels HTTP, erreurs 401, exports blob
state.js         état global, lecture/écriture localStorage
ui.js            messages succès/erreur
utils.js         dates, montants, normalisation contraintes
auth.js          inscription, connexion, compte, logout
profile.js       profil RAV, propagation aux personnes liées
trips.js         voyages, sélection, création, exports
persons.js       guests, personnes liées, présences, édition
expenses.js      dépenses, formulaire guidé, participants, suppression
summary.js       soldes et remboursements
invitations.js   création, liste et révocation des invitations
audit.js         chargement des logs d'audit
constraints.js   contraintes personnalisées du voyage
form-helpers.js  lecture et préparation des formulaires
render.js        rendu global, navigation, binding d'événements
```

## Configuration API

Par défaut :

```text
http://localhost:8080/api/v1
```

Surcharge possible :

```js
window.ACHABITATION_API_BASE_URL = "http://localhost:8080/api/v1";
localStorage.setItem("achabitation.apiBaseUrl", "http://localhost:8080/api/v1");
```

## Tests

```bash
cd frontend-web
./run-tests.sh
```

Sous Windows :

```bat
cd frontend-web
run-tests.bat
```

Le script exécute `node --check`, `frontend-smoke.test.mjs` et `frontend-flow.test.mjs`.

## Limites restantes

- Pas de tests navigateur réel.
- Pas de tests d’accessibilité automatisés.
- Pas de bundler ni typage TypeScript.
- Pas de gestion offline.
- UI encore adaptée au MVP, pas à une production grand public.

# Corrections après audit du 07/07/2026

Ce document trace les corrections appliquées après l’audit qualité du projet.

## Gradle Android

- Le wrapper Android reste aligné sur Gradle `9.6.1`.
- `mobile-android/build.gradle.kts` utilise maintenant `tasks.wrapper { gradleVersion = "9.6.1" }`.
- Les documentations Android et dossier technique ne mentionnent plus Gradle `8.9`.

## CI

- Les documentations ne présentent plus la CI Android comme “à confirmer”.
- Le statut documenté est maintenant : CI validée sur GitHub Actions.

## Authentification web

- Le frontend web n’envoie plus de header `Authorization`.
- Le frontend web déclare explicitement `X-Achabitation-Client: web`.
- Le backend pose la session web dans `ACHABITATION_SESSION` en cookie `HttpOnly`.
- Quand `X-Achabitation-Client: web` est présent, le backend masque `accessToken` dans la réponse JSON.
- `AuthResponse` expose `sessionEstablished` pour que le web sache distinguer une création de compte connectée d’une création nécessitant une vérification email.
- Les requêtes mutantes authentifiées par cookie web restent protégées par CSRF.
- Les clients Android/scripts continuent d’utiliser `Authorization: Bearer <accessToken>`.

## Tests ajoutés ou renforcés

- Test backend : inscription web avec cookie `HttpOnly` et `accessToken` masqué.
- Test backend : requête mutante web refusée sans CSRF puis acceptée avec cookie XSRF + header `X-XSRF-TOKEN`.
- Test frontend flow : vérification que le token n’est pas stocké côté web.
- Test frontend flow : vérification que le header `X-Achabitation-Client: web` est envoyé.
- Test E2E Playwright : assertion d’absence de header `Authorization` côté web.

## Refactor backend

- Extraction de l’export RGPD et de l’anonymisation de compte dans `AccountDataService`.
- `AuthService` ne porte plus directement les responsabilités export/anonymisation.
- `AuthService` reste centré sur inscription, login, sessions, email verification, reset password et modification de mot de passe.

## Validation locale dans cet environnement

Validé ici :

```bash
cd frontend-web && ./run-tests.sh
node --check frontend-web/src/*.js frontend-web/tests/*.mjs frontend-web/tests/e2e/*.js
```

Non validé ici, faute d’accès réseau pour télécharger Maven/Gradle/Playwright :

```bash
cd backend-api && ./mvnw test
cd mobile-android && ./gradlew testDebugUnitTest assembleDebug
cd frontend-web && npm run test:e2e
```

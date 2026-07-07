# Release, versionnement et rollback

## Versionnement

- Tag Git conseillé : `vMAJOR.MINOR.PATCH`.
- Backend : artefact jar versionné.
- Docker : image taggée avec la version et le SHA court.
- Android : `versionCode` incrémenté et `versionName` aligné.
- Front-web : bundle ou archive statique versionnée si hébergement séparé.

## Checklist avant release

```bash
./scripts/validate-all.sh
```

Puis, sur environnement réel :

```bash
cd backend-api && ./mvnw clean test
cd frontend-web && npm install && npm run e2e
cd mobile-android && ./gradlew testDebugUnitTest assembleDebug
```

## Déploiement

1. Sauvegarde PostgreSQL avant migration.
2. Déploiement backend avec migrations Flyway.
3. Vérification `/api/v1/health/liveness` et `/api/v1/health/readiness`.
4. Vérification login, export, suppression compte sur compte de test.
5. Vérification logs sans token ni RAV brut.

## Rollback

1. Revenir à l'image Docker précédente.
2. Vérifier compatibilité de schéma.
3. Restaurer backup uniquement si migration destructive ou corruption.
4. Documenter la cause de rollback.

Les migrations doivent être conçues comme non destructives sauf décision explicite.

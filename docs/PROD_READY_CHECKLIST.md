# Checklist production publique — Achabitation

État après le lot **V6.5 retour production**. Cette checklist distingue les corrections intégrées dans le dépôt de ce qui reste nécessairement à prouver sur une infrastructure réelle.

## P0 lancement public ouvert

| Sujet | Statut V6.5 | Commentaire |
|---|---|---|
| Dépense mutualisée voyage + contraintes personnalisées | Fait | La persistance backend de `customConstraintAmounts` n'est plus limitée aux dépenses datées. Test API ajouté pour dépense `GLOBAL` + contrainte personnalisée + résumé. |
| Fuite d'email dans les audits | Fait | `TripService.linkGuestToUser` ne stocke plus l'email brut dans la description d'audit. Les audit logs voyage sont maintenant réservés owner/admin. Test de non-régression ajouté. |
| Erreurs techniques exposées au client | Fait | `ApiExceptionHandler` renvoie des messages génériques pour contraintes DB et erreurs 500, avec `requestId`. Les détails techniques restent dans les logs serveur. |
| Vérification email | Fait applicatif | Endpoints de demande/confirmation, token hashé, expiration 24h, exigence activée en profil `prod`. SMTP réel à configurer et tester. |
| Email SMTP réel | Fait côté application, non faisable ici | `spring-boot-starter-mail`, `SMTP_*`, `MAIL_FROM`, fail-fast prod. Envoi réel à tester avec le fournisseur cible. |
| Rate limiting distribué réel | Exemple edge fourni, non faisable ici | Exemple Nginx `limit_req`. Redis/gateway reste nécessaire si plusieurs reverse proxies ou plusieurs edges. |
| Politique confidentialité / CGU finalisées | Non faisable ici | Les documents restent des drafts techniques. Validation juridique nécessaire. |
| Backup restauré réellement | Non faisable ici | Scripts disponibles, mais restauration à prouver sur une base PostgreSQL réelle. |
| Monitoring/alerting branché | Fait applicatif minimal, non faisable ici | Actuator + Prometheus exposés. Alerting externe à brancher. |
| CI complète sur machine propre | À valider | Workflow GitHub Actions présent. Le job front E2E utilise maintenant `npm ci` et `package-lock.json`. Succès réel à fournir. |

## P0 seulement si Android public

| Sujet | Statut V6.5 | Commentaire |
|---|---|---|
| Signature release | Non faisable ici | Clé release à générer et stocker hors dépôt. |
| AAB release testé | Non faisable ici | Dépend du build Gradle réel. |
| URL API production définitive | Fait partiel | Contrôle `checkReleaseApiBaseUrl`. Domaine final à renseigner. |
| Politique confidentialité Play Store | Non faisable ici | Dépend du texte final validé juridiquement. |
| Tests APK/AAB installé | Non faisable ici | À faire sur émulateur/appareil réel. |

## P1 production sérieuse

| Sujet | Statut V6.5 | Commentaire |
|---|---|---|
| Validations serveur financières | Fait partiel renforcé | Ajout d'annotations Bean Validation sur dépenses, personnes, profil et devise. Ajout d'une migration SQL de contraintes `CHECK` PostgreSQL. |
| Tests backend contre PostgreSQL | Fait optionnel | Ajout d'un test Testcontainers Postgres désactivé par défaut. À exécuter avec `ACHABITATION_POSTGRES_IT=true ./mvnw test`. |
| Matrice de permissions métier | Fait partiel | Matrice documentée. Audit logs désormais owner/admin uniquement. Des tests supplémentaires restent utiles pour toutes les combinaisons owner/admin/participant/viewer. |
| Stockage token web | Fait bêta renforcée | Le web utilise `ACHABITATION_SESSION` en cookie `HttpOnly` avec `SameSite` et CSRF ciblé. Le token n’est plus stocké dans `localStorage`. En production, activer `Secure`, HTTPS strict et vérifier la configuration proxy. |
| Réduction des gros services backend | Fait partiel renforcé | `AccountSessionService`, `AccountEmailService`, `SecurityEventService`, `UserProfileService` et `AccountDataService` isolent maintenant les responsabilités principales. `AuthService` reste à surveiller mais n’embarque plus export/anonymisation RGPD. |
| Scan dépendances/images | Reste à faire | Maven/npm/Gradle/Docker audit à brancher en CI avant ouverture publique. |
| Monitoring et alerting réels | Reste à faire infra | Les métriques existent ; alertes et dashboards restent à brancher. |
| Audit accessibilité web | Reste à faire | Audit clavier/contrastes/labels/responsive réel non exécuté ici. |
| Politique conservation logs | Documenté | Voir `docs/LOG_RETENTION_POLICY.md`. À valider juridiquement. |

## Commandes de preuve à exécuter hors environnement ChatGPT

```bash
cd backend-api && ./mvnw clean test
ACHABITATION_POSTGRES_IT=true ./mvnw test -Dtest=PostgresIntegrationSmokeTest
cd frontend-web && npm ci && ./run-tests.sh && npx playwright test
cd mobile-android && ./gradlew testDebugUnitTest assembleDebug checkReleaseApiBaseUrl
./scripts/validate-all.sh
```

À ajouter pour un go/no-go public : restauration PostgreSQL complète, test SMTP réel, alerting externe, scan headers HTTPS, test AAB signé si Android public.

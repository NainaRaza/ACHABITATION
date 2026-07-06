# Checklist MVP bêta

Cette checklist décrit l’état attendu pour considérer le projet utilisable en bêta locale fermée.

## Backend

- [x] API Spring Boot séparée du frontend.
- [x] Maven Wrapper disponible.
- [x] Java 21 configuré.
- [x] Base H2 locale de développement.
- [x] Profil `prod` PostgreSQL.
- [x] Migration initiale Flyway.
- [x] Healthcheck et readiness database.
- [x] Authentification par `accessToken` opaque.
- [x] Token brut non stocké en base.
- [x] Logout serveur.
- [x] Rate limiting mémoire sur login/register.
- [x] Rôles de voyage.
- [x] Contrôles d’accès par appartenance et rôle.
- [x] Profil utilisateur central.
- [x] Confidentialité du RAV.
- [x] Guests et liaison à un compte.
- [x] Création directe du compte courant comme personne du voyage.
- [x] Contraintes personnalisées déclarées au niveau du voyage.
- [x] Dépenses normales, globales et avancées.
- [x] Devise et taux de conversion vers la devise du voyage.
- [x] Résumé des soldes.
- [x] Remboursements suggérés.
- [x] Exports CSV.
- [x] Audit logs.
- [x] Tests backend unitaires et intégration.
- [x] Smoke tests API.

## Frontend web

- [x] Dossier séparé `frontend-web/`.
- [x] `app.js` réduit à un point d’entrée.
- [x] Modules fonctionnels dans `frontend-web/src/`.
- [x] Configuration API surchargeable.
- [x] Inscription / connexion / logout.
- [x] Session expirée gérée côté API/front.
- [x] Profil utilisateur.
- [x] Création et sélection de voyage.
- [x] Rejoindre un voyage par code.
- [x] Gestion des contraintes de voyage.
- [x] Personnes, guests et liaison compte.
- [x] Ajout direct du compte courant comme personne.
- [x] Dépenses.
- [x] Résumé.
- [x] Invitations.
- [x] Audit.
- [x] Exports CSV.
- [x] Tests Node de syntaxe et de parcours mocké.

## Android

- [x] Client Android Kotlin / Jetpack Compose présent.
- [x] Gradle Wrapper présent.
- [x] Gradle 8.9.
- [x] Android Gradle Plugin 8.7.3.
- [x] URL API locale par défaut sur émulateur : `10.0.2.2`.
- [x] Configuration possible de l’URL API.
- [x] Authentification.
- [x] Stockage chiffré via `EncryptedSharedPreferences`.
- [x] Cleartext HTTP autorisé en debug uniquement.
- [x] Cleartext refusé en release.
- [x] Écran session expirée.
- [x] Voyages, personnes, dépenses, résumé, invitations, audit.
- [x] Exports CSV en aperçu copiable.
- [x] Confirmations sur actions destructrices principales.
- [x] Tests unitaires Android basiques.

## Documentation

- [x] README racine à jour.
- [x] Documentation backend à jour.
- [x] Documentation front-web à jour.
- [x] Documentation Android à jour.
- [x] Documentation API avec exemples.
- [x] Documentation sécurité bêta.
- [x] Checklist production séparée.
- [x] Ancien desktop documenté comme legacy.

## Points bloquants avant bêta fermée réelle

- [ ] Lancer manuellement le parcours complet backend + web sur une base H2 fraîche.
- [ ] Lancer manuellement le parcours complet Android sur émulateur.
- [ ] Vérifier `./mvnw clean test` sur une machine propre.
- [ ] Vérifier `frontend-web/run-tests.sh` sur une machine propre.
- [ ] Vérifier `mobile-android/gradlew clean assembleDebug` sur une machine propre.
- [ ] Tester l’invitation avec deux comptes réels.
- [ ] Tester les droits `READ_ONLY` dans l’interface.
- [ ] Tester le masquage du RAV privé dans web et Android.
- [ ] Tester une session expirée ou un token invalidé.
- [ ] Vérifier que les messages d’erreur sont compréhensibles pour un utilisateur non technique.

## Hors périmètre MVP bêta

- [ ] Production publique.
- [ ] Application iOS.
- [ ] Mode hors-ligne.
- [ ] OCR de tickets de caisse.
- [ ] Export PDF.
- [ ] Export Excel natif.
- [ ] Multi-devises avec taux automatiques.
- [ ] Notifications.
- [ ] Gestion fine des suppressions définitives et RGPD complet.

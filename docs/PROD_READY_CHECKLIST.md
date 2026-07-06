# Checklist production

Cette checklist liste les actions nécessaires avant exposition publique. Le projet actuel est un MVP bêta local avancé, pas une application production-ready.

## Sécurité applicative

- [x] Routes applicatives protégées par authentification.
- [x] Token brut non stocké en base.
- [x] Logout serveur.
- [x] Rôles de voyage.
- [x] Contrôle du RAV privé côté backend.
- [ ] Remplacer ou compléter le token opaque par une stratégie de session production : refresh token, rotation, révocation multi-session ou session serveur robuste.
- [ ] Ajouter expiration configurable par environnement.
- [ ] Ajouter changement et reset de mot de passe.
- [ ] Ajouter vérification email.
- [ ] Ajouter politique de mot de passe documentée.
- [ ] Ajouter protection brute-force distribuée.
- [ ] Ajouter journaux de sécurité exploitables.
- [ ] Ajouter durcissement des headers HTTP derrière reverse proxy.
- [ ] Ajouter configuration CORS par environnement.
- [ ] Supprimer l’accès H2 console hors dev.

## Données et base

- [x] H2 local.
- [x] PostgreSQL en profil `prod`.
- [x] Migration initiale Flyway.
- [ ] Supprimer `ddl-auto=update` de tout environnement partagé.
- [ ] Ajouter migrations incrémentales versionnées.
- [ ] Ajouter stratégie de sauvegarde PostgreSQL et restauration testée.
- [ ] Ajouter index complémentaires après mesure.
- [ ] Ajouter contraintes de suppression/archivage cohérentes.
- [ ] Ajouter politique de conservation des audit logs.
- [ ] Ajouter export/suppression des données utilisateur selon obligations applicables.

## Backend

- [x] Tests unitaires domaine.
- [x] Tests d’intégration API.
- [x] Smoke tests.
- [ ] Ajouter tests de charge simples.
- [ ] Ajouter tests d’autorisation exhaustifs par route.
- [ ] Ajouter pagination si les listes deviennent volumineuses.
- [ ] Ajouter limites de taille strictes sur payloads.
- [ ] Ajouter logs structurés, métriques et request-id.
- [ ] Ajouter documentation OpenAPI générée.

## Frontend web

- [x] Séparation frontend/backend.
- [x] Modules JS séparés.
- [x] Tests Node de syntaxe et parcours mocké.
- [ ] Ajouter tests E2E navigateur.
- [ ] Ajouter tests d’accessibilité.
- [ ] Ajouter build/bundling production ou choisir explicitement de rester sans bundler.
- [ ] Ajouter CSP adaptée.
- [ ] Vérifier l’ergonomie mobile web.
- [ ] Ajouter design system minimal.
- [ ] Ajouter messages d’erreur homogènes.

## Android

- [x] Client Kotlin / Compose.
- [x] Gradle Wrapper 8.9.
- [x] Stockage chiffré via `EncryptedSharedPreferences`.
- [x] Cleartext autorisé seulement en debug.
- [x] Cleartext refusé en release.
- [x] Session expirée gérée.
- [ ] Ajouter CI Android.
- [ ] Ajouter tests UI Compose et tests instrumentés.
- [ ] Ajouter signature release réelle.
- [ ] Remplacer l’URL release placeholder.
- [ ] Ajouter Storage Access Framework pour les exports CSV.
- [ ] Ajouter gestion fine de perte réseau.
- [ ] Ajouter mode hors-ligne ou décider explicitement qu’il n’existe pas.
- [ ] Vérifier accessibilité TalkBack.
- [ ] Préparer publication Play Store si nécessaire.

## iOS

- [ ] Créer le projet iOS.
- [ ] Aligner les DTO sur le backend.
- [ ] Implémenter auth, voyages, personnes, dépenses, résumé, invitations, audit.
- [ ] Ajouter stockage sécurisé Keychain.
- [ ] Ajouter tests.

## DevOps

- [x] Docker Compose local PostgreSQL + backend.
- [x] CI backend et frontend.
- [ ] Ajouter CI Android.
- [ ] Ajouter build Docker reproductible avec tags.
- [ ] Ajouter reverse proxy TLS.
- [ ] Ajouter configuration par secrets.
- [ ] Ajouter monitoring, alerting, environnements distincts et rollback.

## Produit / conformité

- [ ] Conditions d’utilisation.
- [ ] Politique de confidentialité.
- [ ] Mentions légales.
- [ ] Procédure de suppression de compte.
- [ ] Export des données personnelles.
- [ ] Analyse RGPD minimale.
- [ ] Support utilisateur.
- [ ] Onboarding clair.
- [ ] Texte expliquant que le RAV privé peut parfois être inféré indirectement par les calculs.

## Décision de statut

Le projet peut être considéré comme cohérent pour une bêta locale fermée si les tests passent et si les parcours manuels principaux sont validés sur web et Android.

Il ne doit pas être considéré comme prêt pour une production publique tant que les points sécurité, DevOps, RGPD, monitoring, CI Android et tests E2E ne sont pas traités.

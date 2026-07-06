# Architecture refonte ACHABITATION

## Objectif

La refonte sépare l’application en clients et serveur :

```text
frontend-web      ┐
mobile-android    ├──> backend-api ──> base de données
mobile-ios futur  ┘
```

Le backend concentre les données, les droits et les calculs. Les clients affichent l’interface, collectent les saisies et appellent l’API.

## Modules du dépôt

```text
backend-api/       API Spring Boot, sécurité, persistance, calculs, exports, audit
frontend-web/      client web local HTML/CSS/JavaScript
mobile-android/    client Android Kotlin / Jetpack Compose
mobile-ios/        dossier réservé au futur client iOS
desktop-legacy/    ancien client Java Swing conservé comme référence historique
docs/              documentation technique et fonctionnelle
infra/             docker-compose PostgreSQL + backend-api
scripts/           smoke tests API
```

## Backend

Le backend est organisé en couches :

```text
fr/achabitation/api             contrôleurs REST et DTO
fr/achabitation/application     services applicatifs
fr/achabitation/config          sécurité, CORS, filtre token
fr/achabitation/domain          modèle métier pur et calcul RAV
fr/achabitation/infrastructure  entités JPA et repositories
```

### Couche `domain`

La couche domaine ne dépend pas de Spring. Elle contient les règles de calcul : personnes actives, dates de présence, dépenses normales/globales/avancées, exclusions viande/alcool/contraintes, RAV classique, poids moyen, devises, soldes et remboursements.

### Couche `application`

Les services applicatifs orchestrent les cas d’usage :

- `AuthService` : comptes, login, logout, profil utilisateur, propagation du profil ;
- `TripService` : voyages, contraintes du voyage, invitations, adhésion ;
- `PersonService` : guests, personnes liées, présences, confidentialité RAV ;
- `ExpenseService` : création, modification, validation et suppression des dépenses ;
- `SummaryService` : appel au moteur de calcul et construction du résumé ;
- `ExportService` : exports CSV ;
- `AuditService` et `AuditQueryService` : journalisation et lecture de l’historique ;
- `AuthorizationService` : contrôles d’accès par rôle et propriété du profil ;
- `SessionTokenService` : génération et hash des tokens opaques ;
- `LoginRateLimiter` : limitation mémoire des tentatives de login/register.

### Couche `api`

Les contrôleurs exposent les endpoints REST sous `/api/v1`. Les DTO sont placés dans `fr/achabitation/api/dto` et constituent le contrat consommé par le web et Android.

### Couche `infrastructure`

La persistance utilise Spring Data JPA. En local, la base est H2 fichier. En profil `prod`, le backend utilise PostgreSQL avec Flyway.

## Sécurité

Le modèle de sécurité actuel est adapté à une bêta locale fermée :

- inscription et login publics ;
- routes applicatives protégées par `Authorization: Bearer <accessToken>` ;
- token brut non stocké en base ;
- hash SHA-256 stocké dans `app_user.session_token_hash` ;
- validité actuelle du token : 30 jours ;
- endpoint logout serveur ;
- rate limiting mémoire sur login/register ;
- rôles de voyage : `OWNER`, `ADMIN`, `PARTICIPANT`, `READ_ONLY`.

Ce modèle doit être durci avant exposition publique. Les actions à mener sont dans `PROD_READY_CHECKLIST.md`.

## Frontend web

Le frontend web est une application sans framework située dans `frontend-web/`. Le fichier `app.js` est un point d’entrée court ; la logique est répartie dans `src/` : `api`, `state`, `auth`, `profile`, `trips`, `persons`, `expenses`, `summary`, `invitations`, `audit`, `constraints`, `form-helpers`, `render`, `ui` et `utils`.

Le frontend est servi séparément du backend sur `http://localhost:5173` et appelle `http://localhost:8080/api/v1`.

## Android

`mobile-android/` contient un client Kotlin / Jetpack Compose. Le token est stocké via `EncryptedSharedPreferences`. Le HTTP cleartext est autorisé en debug pour le backend local et refusé en release.

Structure principale :

```text
MainActivity.kt          shell applicatif, drawer, top bar
AuthScreen.kt            connexion, inscription, session expirée
HomeScreen.kt            accueil, voyages, compte, profil
TripScreen.kt            détail de voyage et navigation interne
PersonsScreen.kt         personnes, guests, rattachement compte
ExpensesScreen.kt        dépenses et formulaire guidé
SummaryScreen.kt         soldes, remboursements, exports
InvitationsScreen.kt     codes d'invitation
AuditScreen.kt           journal d'audit
MainViewModel.kt         état écran et orchestration API
AchabitationApi.kt       client HTTP REST
ApiModels.kt             DTO alignés sur le backend
SecurePreferences.kt     stockage local chiffré
```

## iOS et desktop legacy

`mobile-ios/` est un emplacement réservé. La cible prévue est un client qui consomme la même API REST, sans recoder les calculs RAV.

`desktop-legacy/` contient l’ancien client Swing. Il sert uniquement de référence historique.

## Flux principal

1. Un utilisateur crée un compte ou se connecte.
2. Il crée un voyage ou rejoint un voyage par code d’invitation.
3. Il crée des personnes guests, lie son compte à un guest ou ajoute son compte directement comme personne.
4. Il saisit les RAV, les contraintes et les périodes de présence.
5. Il saisit les dépenses.
6. Le backend calcule les soldes et remboursements.
7. Les clients affichent le résumé, les exports et l’audit.

## Limites assumées

- Pas encore de CI Android.
- Pas de tests E2E navigateur.
- Pas de refresh token.
- Rate limiting non distribué.
- Pas de reverse proxy documenté pour production.
- Pas d’application iOS.
- Pas de mode hors-ligne mobile.

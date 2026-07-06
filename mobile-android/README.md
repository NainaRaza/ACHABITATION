# ACHABITATION Android

Application Android native Kotlin / Jetpack Compose consommant l’API Spring Boot `backend-api`.

## Statut

Le dossier `mobile-android/` contient un vrai client Android MVP, pas un simple dossier préparatoire. Il couvre les principaux parcours exposés par le backend et partage le même contrat REST que le frontend web.

## Lancement local

1. Démarrer le backend depuis la racine du dépôt :

```bash
cd backend-api
./mvnw spring-boot:run
```

2. Ouvrir ce dossier dans Android Studio :

```text
mobile-android/
```

3. Lancer l’application sur émulateur ou téléphone.

URL API par défaut sur émulateur Android :

```text
http://10.0.2.2:8080/api/v1
```

Sur téléphone physique connecté au même Wi-Fi que le PC, remplacer `10.0.2.2` par l’adresse IP locale du PC, par exemple :

```text
http://192.168.1.42:8080/api/v1
```

## Build

Le projet utilise :

```text
Android Gradle Plugin : 8.7.3
Gradle Wrapper        : 8.9
Kotlin                : 2.0.21
compileSdk            : 35
minSdk                : 26
targetSdk             : 35
```

Commandes utiles :

```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
```

Sous Windows :

```bat
gradlew.bat clean assembleDebug
gradlew.bat testDebugUnitTest
```

## Fonctionnalités mobiles couvertes

- création de compte ;
- connexion / déconnexion ;
- session expirée ;
- modification email + nom de compte ;
- consultation et modification du profil RAV ;
- calcul RAV simple ou avancé ;
- contraintes alimentaires et personnalisées ;
- application du profil aux personnes liées ;
- création et liste des voyages ;
- rejoindre un voyage par code d’invitation ;
- modification des contraintes personnalisées du voyage ;
- ajout direct du compte connecté comme personne du voyage ;
- rattachement du compte connecté à un guest existant ;
- création, modification et désactivation de personnes ;
- périodes de présence ;
- ajout, modification et suppression de dépenses ;
- dépenses normales, globales et avancées ;
- montants viande, alcool et contraintes personnalisées ;
- participants manuels ;
- devise et taux de conversion vers la devise du voyage ;
- soldes et remboursements suggérés ;
- création, copie et révocation d’invitations ;
- audit logs ;
- exports CSV dépenses et résumé en aperçu copiable.

## Architecture

```text
app/src/main/java/fr/achabitation/mobile/
├── MainActivity.kt          shell applicatif, drawer, top bar
├── AuthScreen.kt            connexion, inscription, session expirée
├── HomeScreen.kt            accueil, voyages, compte, profil
├── TripScreen.kt            détail de voyage et navigation interne
├── PersonsScreen.kt         personnes, guests, rattachement compte
├── ExpensesScreen.kt        dépenses et formulaire guidé
├── SummaryScreen.kt         soldes, remboursements, exports
├── InvitationsScreen.kt     codes d'invitation
├── AuditScreen.kt           journal d'audit
├── MainViewModel.kt         état écran + orchestration API
├── AchabitationApi.kt       client HTTP REST sans logique métier
├── ApiModels.kt             DTO alignés sur backend-api
├── SecurePreferences.kt     stockage local chiffré
├── UiComponents.kt          composants communs
└── UiTheme.kt               thème Material 3 et enums UI
```

La logique métier reste côté backend. L’application Android ne recalcule pas les soldes ni le RAV collectif.

## Sécurité Android

- Le token et les informations de session sont stockés via `EncryptedSharedPreferences`.
- En debug, le HTTP cleartext est autorisé pour le backend local : `10.0.2.2`, `localhost`, `127.0.0.1`.
- En release, `usesCleartextTraffic=false`.
- En release, le ViewModel/API refuse les URL `http://`.
- Une réponse HTTP 401 nettoie la session locale et affiche l’écran “Session expirée”.
- Les actions destructrices principales demandent confirmation : suppression de dépense, désactivation de personne, révocation d’invitation.

## Limites actuelles

- Pas encore de CI Android dans `.github/workflows/ci.yml`.
- Pas de tests UI Compose.
- Pas de tests instrumentés.
- Pas de mode hors-ligne.
- Pas de sauvegarde native des exports CSV via Storage Access Framework ; les exports sont affichés en texte copiable.
- URL release encore placeholder : `https://api.achabitation.example/api/v1`.
- Pas encore de signature release réelle ni de préparation Play Store.

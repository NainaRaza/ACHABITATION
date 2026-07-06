# ACHABITATION Android

Application Android native Kotlin / Jetpack Compose consommant l'API Spring Boot `backend-api`.

## Lancement local

1. Démarrer le backend :

```bash
cd backend-api
mvn spring-boot:run
```

2. Ouvrir ce dossier dans Android Studio :

```text
mobile-android/
```

3. Lancer l'application sur émulateur ou téléphone.

URL API par défaut sur émulateur Android :

```text
http://10.0.2.2:8080/api/v1
```

Sur téléphone physique connecté au même Wi-Fi que le PC, remplacer `10.0.2.2` par l'adresse IP locale du PC, par exemple :

```text
http://192.168.1.42:8080/api/v1
```

## Fonctionnalités mobiles couvertes

Cette version Android reprend les principales fonctionnalités disponibles via le backend et l'interface web :

- création de compte ;
- connexion / déconnexion ;
- modification email + nom de compte ;
- consultation et modification du profil RAV ;
- calcul RAV simple ou avancé ;
- contraintes alimentaires et personnalisées ;
- application du profil aux personnes liées ;
- création de voyage ;
- liste des voyages ;
- rejoindre un voyage par code d'invitation ;
- modification des contraintes personnalisées du voyage ;
- ajout direct du compte connecté comme personne du voyage ;
- rattachement du compte connecté à un guest existant ;
- création de guest ;
- modification de personne ;
- désactivation de personne ;
- périodes de présence principales et seconde période optionnelle ;
- ajout de dépense ;
- modification de dépense ;
- suppression de dépense ;
- type de dépense normale ou globale ;
- montants viande / alcool ;
- montants par contrainte personnalisée ;
- participants manuels ;
- devise et taux de conversion vers la devise du voyage ;
- soldes ;
- remboursements suggérés ;
- création d'invitations ;
- copie du code d'invitation ;
- révocation d'invitations ;
- audit logs ;
- exports CSV dépenses et résumé en aperçu copiable.

## Architecture

```text
app/src/main/java/fr/achabitation/mobile/
├── MainActivity.kt      # UI Compose
├── MainViewModel.kt     # état écran + orchestration API
├── AchabitationApi.kt   # client HTTP REST sans logique métier
└── ApiModels.kt         # DTO alignés sur backend-api
```

La logique métier reste côté backend. L'application Android ne recalcule pas les soldes ni le RAV collectif ; elle saisit les données et consomme les endpoints REST.

## Remarques

L'export CSV est affiché dans l'application sous forme de texte sélectionnable. Pour une vraie production, il faudrait ajouter un enregistrement via le Storage Access Framework Android ou un partage natif.

Cette version reste un client mobile MVP complet, pas encore une application Play Store : il manque notamment des tests UI, un thème graphique avancé, une gestion hors-ligne et un stockage sécurisé du token via EncryptedSharedPreferences.

## Refonte UI mobile

Cette archive contient une refonte de l'interface Android pour éviter le modèle « tout sur une seule page » :

- menu latéral global accessible via `☰` ;
- écran d'accueil séparé des écrans compte/profil ;
- sections dédiées pour créer un voyage et rejoindre un voyage ;
- navigation par sections dans un voyage : vue, personnes, dépenses, résumé, invitations, audit ;
- cartes Material 3 surélevées pour améliorer la lisibilité mobile ;
- écran de connexion plus propre avec bloc d'en-tête ;
- messages d'erreur/succès mieux visibles ;
- conservation des appels API et du ViewModel existants afin de limiter les régressions fonctionnelles.

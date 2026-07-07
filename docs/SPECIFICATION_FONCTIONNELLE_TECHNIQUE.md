# Spécification fonctionnelle et technique — ACHABITATION

## 1. Objet

ACHABITATION est une application de partage de dépenses au reste à vivre, abrégé RAV. Elle vise les voyages, colocations temporaires, séjours entre ami·es ou tout contexte où des dépenses sont partagées par un groupe.

Contrairement à une répartition strictement égalitaire, ACHABITATION peut répartir les dépenses selon la capacité contributive des participant·es. Le RAV sert de poids de contribution : plus le RAV est élevé, plus la part théorique est élevée.

La version actuelle est une refonte multi-client :

```text
frontend-web      → backend-api → base de données
mobile-android    → backend-api → base de données
mobile-ios futur  → backend-api → base de données
```

L’ancien client Swing est conservé dans `desktop-legacy/` comme référence historique uniquement.

## 2. Périmètre fonctionnel actuel

Le périmètre actuel couvre :

- comptes utilisateurs ;
- authentification par token opaque ;
- profil utilisateur central ;
- RAV simple ;
- calcul avancé du RAV ;
- mode poids moyen ;
- RAV public ou privé ;
- voyages multiples ;
- rôles de voyage ;
- invitations par code ;
- guests ;
- liaison d’un guest à un compte ;
- ajout direct du compte courant comme personne du voyage ;
- contraintes végétarien et sans alcool ;
- contraintes personnalisées déclarées au niveau du voyage ;
- périodes de présence ;
- dépenses datées ;
- dépenses mutualisées voyage ;
- dépenses avancées avec participant·es manuel·les ;
- montants spécifiques viande, alcool et contraintes personnalisées ;
- devise de référence du voyage ;
- devise et taux de conversion par dépense ;
- résumé des soldes ;
- remboursements suggérés ;
- exports CSV ;
- audit logs ;
- interface web locale ;
- client Android MVP.

Le périmètre ne couvre pas encore :

- application iOS ;
- production publique ;
- refresh token ;
- reset de mot de passe ;
- vérification email ;
- paiement ;
- OCR de tickets ;
- export PDF ;
- export Excel natif ;
- mode hors-ligne ;
- synchronisation offline/online.

## 3. Concepts métier

### 3.1 Utilisateur

Un utilisateur possède un compte, un email, un nom affiché, un mot de passe hashé et un profil RAV central.

Le profil contient notamment :

- RAV ;
- mode `LIVING_REST` ou `AVERAGE` ;
- calcul avancé ;
- revenus et charges ;
- végétarien ;
- sans alcool ;
- contraintes personnalisées ;
- visibilité du RAV.

Modifier le profil central ne modifie pas automatiquement les personnes déjà liées dans les voyages. L’utilisateur doit appliquer explicitement son profil à une ou plusieurs personnes liées.

### 3.2 Voyage

Un voyage contient :

- nom ;
- date de début ;
- date de fin ;
- devise de référence ;
- contraintes personnalisées officielles ;
- membres et rôles ;
- invitations ;
- personnes ;
- dépenses ;
- audit logs.

Les contraintes personnalisées sont déclarées au niveau du voyage. Une personne ou une dépense ne peut utiliser qu’une contrainte déclarée dans ce voyage.

### 3.3 Rôles de voyage

Rôles disponibles :

```text
OWNER
ADMIN
PARTICIPANT
READ_ONLY
```

Règles actuelles :

- lecture : membre du voyage ;
- écriture : `OWNER`, `ADMIN`, `PARTICIPANT` ;
- administration : `OWNER`, `ADMIN` ;
- lecture seule : `READ_ONLY`.

### 3.4 Personne

Une personne représente un membre du groupe dans un voyage. Elle peut être :

- un guest non lié à un compte ;
- une personne liée à un compte utilisateur.

Une personne possède :

- nom ;
- RAV ;
- mode de poids ;
- paramètres de calcul avancé ;
- visibilité du RAV ;
- végétarien ;
- sans alcool ;
- contraintes personnalisées ;
- périodes de présence ;
- statut actif/inactif.

Deux personnes du même voyage ne peuvent pas avoir le même nom après normalisation.

### 3.5 Dépense

Une dépense contient :

- titre ;
- date ;
- payeur ;
- montant total ;
- montant viande ;
- montant alcool ;
- montants par contrainte personnalisée ;
- type `NORMAL` ou `GLOBAL` ;
- mode avancé ou non ;
- participant·es manuel·les si mode avancé ;
- devise ;
- taux de conversion vers la devise du voyage.

La somme viande + alcool + contraintes personnalisées ne peut pas dépasser le total.

## 4. Règles de calcul

### 4.1 Dépense datée

Une dépense datée concerne les personnes actives présentes à la date de dépense, sauf si le mode avancé sélectionne explicitement les participant·es.

### 4.2 Dépense mutualisée voyage

Une dépense mutualisée voyage correspond au type technique `GLOBAL`. Elle ignore les dates de présence et concerne les personnes actives du voyage. Elle sert aux dépenses décidées collectivement, par exemple essence, logement, courses du premier jour ou frais communs hors jour précis.

Elle n’ignore pas les exclusions : les blocs viande, alcool et contraintes personnalisées restent appliqués. Par exemple, si une dépense mutualisée voyage contient une part alcool, les personnes marquées sans alcool ne paient pas cette part.

### 4.3 Mode avancé

En mode avancé, les participant·es sont sélectionné·es manuellement. Le backend valide que la dépense concerne au moins une personne.

### 4.4 Exclusions

- Une personne végétarienne ne participe pas au bloc viande.
- Une personne sans alcool ne participe pas au bloc alcool.
- Une personne qui porte une contrainte personnalisée ne participe pas au bloc correspondant à cette contrainte.

Le reste du montant est réparti selon les règles générales.

### 4.5 RAV classique

En mode `LIVING_REST`, la part d’une personne est proportionnelle à son RAV.

Une personne en mode `LIVING_REST` doit avoir un RAV strictement positif.

### 4.6 Poids moyen

En mode `AVERAGE`, la personne utilise la moyenne des RAV exploitables des autres participant·es du bloc.

Si toutes les personnes concernées par un bloc sont en mode `AVERAGE`, le bloc est réparti à parts égales.

### 4.7 Devises

Chaque voyage possède une devise de référence. Chaque dépense peut avoir sa propre devise et un taux `exchangeRateToTripCurrency`. Les calculs de résumé se font dans la devise de référence du voyage.

## 5. Résumé et remboursements

Le résumé contient :

- total payé par personne ;
- total dû par personne ;
- solde ;
- remboursements suggérés.

Un solde positif signifie que la personne doit recevoir de l’argent. Un solde négatif signifie qu’elle doit rembourser.

## 6. Confidentialité du RAV

Une personne liée à un compte peut choisir `livingRestPublic=false`.

Dans ce cas, les autres membres ne reçoivent pas le RAV direct dans la liste des personnes. Le backend continue néanmoins à utiliser le RAV réel pour les calculs. Il faut donc considérer cette confidentialité comme un masquage d’affichage, pas comme une impossibilité mathématique absolue d’inférence.

## 7. API

Les endpoints principaux sont documentés dans `API_EXEMPLES.md`.

Familles de routes :

```text
/api/v1/auth
/api/v1/trips
/api/v1/trips/{tripId}/persons
/api/v1/trips/{tripId}/expenses
/api/v1/trips/{tripId}/summary
/api/v1/trips/{tripId}/exports
/api/v1/trips/{tripId}/audit-logs
/api/v1/health
```

## 8. Architecture technique

Backend : Java 21, Spring Boot 3.5.15, Spring Security, Spring Data JPA, H2 local, PostgreSQL en profil `prod`, Flyway, Maven Wrapper.

Frontend web : HTML/CSS/JavaScript sans framework, modules ES, serveur local via scripts `run-web`.

Android : Kotlin, Jetpack Compose, Material 3, kotlinx.serialization, EncryptedSharedPreferences, Gradle 8.9.

## 9. Tests

Backend :

```bash
cd backend-api
./mvnw clean test
```

Frontend :

```bash
cd frontend-web
./run-tests.sh
```

Android :

```bash
cd mobile-android
./gradlew testDebugUnitTest
```

Smoke test API :

```bash
./scripts/smoke-test.sh
```

## 10. Critères d’acceptation bêta

Le projet est cohérent pour une bêta locale fermée si :

- les tests backend passent ;
- les tests frontend passent ;
- Android compile en debug ;
- un parcours complet est validé manuellement sur web ;
- un parcours complet est validé manuellement sur Android ;
- le masquage du RAV privé est vérifié ;
- les rôles et invitations sont vérifiés ;
- les exports CSV sont vérifiés.

## 11. Limites de production

Avant production publique, il faut traiter au minimum :

- stratégie de session production ;
- reset mot de passe ;
- vérification email ;
- CORS par environnement ;
- rate limiting distribué ;
- CI Android ajoutée, à valider sur runner ou machine propre ;
- tests E2E web ;
- monitoring ;
- logs structurés ;
- RGPD ;
- politique de sauvegarde ;
- reverse proxy TLS ;
- remplacement de l’URL Android release placeholder.

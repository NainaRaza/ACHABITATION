# Dossier technique Java — ACHABITATION

## Vue générale

ACHABITATION est une application de partage de dépenses au reste à vivre. Le projet actuel est structuré autour d’une API Java Spring Boot, d’un client web local et d’un client Android natif.

Le backend Java porte le contrat REST, l’authentification, les règles d’autorisation, la persistance, les calculs RAV, les exports CSV, l’audit et les tests automatisés.

Le dossier `desktop-legacy/` contient une ancienne application Java Swing. Elle est conservée comme référence historique, mais elle ne représente plus l’architecture cible.

## Technologies backend

```text
Java                  21
Spring Boot           3.5.15
Spring Web            API REST
Spring Data JPA       persistance
Spring Security       authentification et autorisations
Bean Validation       validations DTO
H2                    développement local et tests
PostgreSQL            profil prod expérimental
Flyway                migrations prod
Maven Wrapper         build reproductible
JUnit / Spring Test   tests automatisés
```

## Organisation du backend

```text
backend-api/src/main/java/fr/achabitation/
├── AchabitationApplication.java
├── api/
│   ├── controller/
│   └── dto/
├── application/
├── config/
├── domain/
└── infrastructure/
```

## Couche API

Contrôleurs :

```text
AuthController      /api/v1/auth
TripController      /api/v1/trips
PersonController    /api/v1/trips/{tripId}/persons
ExpenseController   /api/v1/trips/{tripId}/expenses
SummaryController   /api/v1/trips/{tripId}/summary
ExportController    /api/v1/trips/{tripId}/exports
AuditController     /api/v1/trips/{tripId}/audit-logs
HealthController    /api/v1/health
ApiExceptionHandler gestion homogène des erreurs
```

DTO principaux : `AuthDtos`, `TripDtos`, `PersonDtos`, `ExpenseDtos`, `SummaryDtos` et `AuditDtos`.

## Couche sécurité

`SecurityConfig` désactive CSRF pour l’API locale, configure le CORS local, limite les routes publiques, impose l’authentification sur `/api/v1/**` et ajoute le filtre `SessionTokenAuthenticationFilter`.

Routes publiques :

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/health/**
GET  /h2-console/** en développement local
```

`SessionTokenAuthenticationFilter` lit `Authorization: Bearer <accessToken>`, hashe le token reçu, vérifie le hash stocké en base et refuse les tokens émis depuis plus de 30 jours.

`SessionTokenService` génère un token brut aléatoire de 32 octets et stocke uniquement son hash SHA-256.

`LoginRateLimiter` limite `register` et `login` à 12 tentatives par IP et par action sur 10 minutes. Ce mécanisme est mémoire et doit être remplacé en production distribuée.

## Couche application

### `AuthService`

Création de compte, normalisation email, hash BCrypt, login, génération de session, logout, modification du compte, profil RAV et application explicite du profil aux personnes liées.

### `AuthorizationService`

Vérifie l’authentification, l’appartenance au voyage, les droits de lecture/écriture/admin et la propriété du profil financier d’une personne liée.

Matrice actuelle :

```text
Lecture voyage              membre du voyage
Écriture voyage             OWNER, ADMIN, PARTICIPANT
Administration              OWNER, ADMIN
Gestion invitations          OWNER, ADMIN
Contraintes du voyage        OWNER, ADMIN
Profil financier lié         compte lié ou admin selon cas
READ_ONLY                    lecture uniquement
```

### `TripService`

Création des voyages, ajout du créateur comme `OWNER`, normalisation des contraintes personnalisées, invitations, adhésion par code, liaison optionnelle à un guest et audit.

Une invitation demandant `OWNER` est ramenée à `PARTICIPANT`. La durée d’invitation est bornée entre 1 et 30 jours.

### `PersonService`

Création de guests, création d’une personne liée au compte courant, modification, désactivation, périodes de présence, contraintes personnelles et confidentialité du RAV.

Règles importantes : nom unique par voyage après normalisation, RAV strictement positif en mode `LIVING_REST`, RAV nul autorisé en mode `AVERAGE`, périodes cohérentes et contraintes personnelles déclarées dans le voyage.

### `ExpenseService`

Création, modification, liste et suppression des dépenses. Le service valide le payeur, les dates, les montants, les contraintes personnalisées et le fait qu’une dépense concerne au moins une personne.

Une dépense `GLOBAL` ignore les dates de présence dans le calcul. Une dépense avancée utilise `manualParticipantIds`.

### `SummaryService`, `ExportService`, `AuditService`

`SummaryService` appelle le moteur de calcul et expose soldes/remboursements. `ExportService` génère les CSV UTF-8 avec séparateur `;`. `AuditService` écrit les actions importantes et `AuditQueryService` lit l’historique d’un voyage.

## Couche domaine

Modèles : `DomainPerson`, `DomainExpense`, `Balance`, `Settlement`, `PresencePeriod`, `ExpenseType`, `WeightMode`.

`BalanceCalculator` calcule les montants payés, dus, les soldes, les remboursements optimisés, les exclusions et le poids moyen. Il ne dépend ni de Spring ni de JPA.

Types de dépenses :

```text
NORMAL : prend en compte les dates de présence, sauf mode avancé manuel.
GLOBAL : mutualise la dépense hors dates de présence.
```

Modes de poids :

```text
LIVING_REST : utilise le RAV réel de la personne.
AVERAGE     : utilise la moyenne des RAV exploitables des autres participant·es concerné·es.
```

Si toutes les personnes concernées sont en `AVERAGE`, le bloc est réparti à parts égales.

## Couche infrastructure

Entités principales : `UserEntity`, `TripEntity`, `TripMemberEntity`, `TripInvitationEntity`, `PersonEntity`, `PresencePeriodEntity`, `ExpenseEntity`, `ExpenseParticipantEntity`, `AuditLogEntity`.

Repositories : `UserRepository`, `TripRepository`, `TripMemberRepository`, `TripInvitationRepository`, `PersonRepository`, `ExpenseRepository`, `AuditLogRepository`.

La migration initiale est :

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

## Tests

Commande :

```bash
cd backend-api
./mvnw clean test
```

Tests unitaires : `BalanceCalculatorTest`, `PresencePeriodTest`, `EntityMapperTest`, `AuthServiceTest`.

Test d’intégration principal : `AchabitationApiIntegrationTest`.

## Frontend web

Le client web est dans `frontend-web/`. Il n’est plus servi par Spring Boot. `app.js` est un point d’entrée ; la logique est répartie dans `src/`.

Tests :

```bash
cd frontend-web
./run-tests.sh
```

## Android

Le client Android est dans `mobile-android/`.

Technologies : Kotlin, Jetpack Compose, Material 3, kotlinx.serialization, EncryptedSharedPreferences, Gradle 8.9, Android Gradle Plugin 8.7.3.

Le client consomme les mêmes DTO que le backend via `AchabitationApi.kt` et `ApiModels.kt`.

## Limites techniques actuelles

- Authentification opaque correcte pour bêta, mais pas encore architecture production complète.
- Rate limiting mémoire non distribué.
- Pas de migrations incrémentales au-delà du schéma initial.
- Pas de CI Android.
- Pas de tests E2E navigateur.
- Pas de client iOS.
- Exports limités au CSV.
- Pas de mode hors-ligne mobile.

## Commandes utiles

```bash
cd backend-api && ./mvnw spring-boot:run
cd backend-api && ./mvnw clean test
cd frontend-web && ./run-web.sh
cd frontend-web && ./run-tests.sh
cd mobile-android && ./gradlew clean assembleDebug
cd mobile-android && ./gradlew testDebugUnitTest
docker compose -f infra/docker-compose.yml up --build
```

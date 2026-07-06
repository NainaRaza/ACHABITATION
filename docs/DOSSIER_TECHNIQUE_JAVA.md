# Dossier technique Java — ACHABITATION

Ce dossier sert à comprendre, expliquer et maintenir le code Java du projet ACHABITATION. Il ne remplace pas la lecture du code source : il donne une carte technique pour savoir quel fichier fait quoi, quelles méthodes portent les règles importantes et comment présenter l’architecture sans se perdre dans les détails.

> Positionnement honnête : ce document est un support d’appropriation technique. Il permet de démontrer que tu comprends le projet, les choix d’architecture et les règles métier. Il ne doit pas être utilisé pour masquer une contribution externe ou inventer une paternité technique.

## 1. Vue d’ensemble

ACHABITATION est une application de partage de dépenses au reste à vivre. Le backend est une application Spring Boot exposant une API REST et servant aussi une interface web locale. L’ancien client Swing est conservé dans `desktop-legacy` comme référence historique.

Architecture logique :

```text
Interface web statique / navigateur
        ↓ HTTP JSON
Contrôleurs REST Spring Boot
        ↓
Services applicatifs
        ↓
Repositories Spring Data JPA
        ↓
Base H2 locale ou PostgreSQL en profil prod
```

La règle centrale est isolée dans le package `domain` : le moteur de calcul ne dépend ni de Spring, ni de JPA, ni du web. C’est un bon point de conception : le calcul peut être testé indépendamment et réutilisé plus tard par une app mobile, un backend cloud ou un autre frontend.

## 2. Organisation des packages principaux

```text
fr.achabitation.api.controller       Endpoints REST
fr.achabitation.api.dto              Objets d’entrée/sortie API
fr.achabitation.application          Services métier applicatifs
fr.achabitation.config               Configuration sécurité
fr.achabitation.domain               Calcul pur et modèle métier
fr.achabitation.infrastructure.entity Entités JPA persistées
fr.achabitation.infrastructure.repository Accès base Spring Data
fr.achabitation.web                  Entrée de l’interface web statique
```

## 3. Parcours fonctionnels importants

### Création d’un voyage

1. L’utilisateur crée un compte ou se connecte.
2. `TripController.create()` reçoit la requête.
3. `AuthContextService.requiredUser()` récupère l’utilisateur via token.
4. `TripService.create()` valide les dates, crée le voyage, ajoute l’utilisateur comme `OWNER`, sauvegarde et journalise.
5. `EntityMapper.toTripResponse()` renvoie un DTO exploitable par le frontend.

### Ajout d’une personne guest

1. `PersonController.create()` reçoit les données.
2. `AuthorizationService.requireAdmin()` impose un rôle suffisant.
3. `PersonService.create()` valide nom unique, présence, RAV, contraintes.
4. La personne est persistée avec ses périodes de présence.

### Liaison guest ↔ compte

1. L’utilisateur connecté rejoint un voyage ou sélectionne un guest.
2. `PersonController.linkCurrentUser()` ou `TripService.joinByCode()` déclenche la liaison.
3. `TripService.linkGuestToUser()` vérifie que le guest appartient au voyage, n’est pas déjà lié à un autre compte et que le compte n’est pas déjà lié ailleurs dans le même voyage.
4. Le profil utilisateur n’est appliqué au guest que si `applyProfileToGuest=true`.

### Calcul résumé

1. `SummaryController.summary()` reçoit la demande.
2. `SummaryService.summary()` récupère personnes et dépenses.
3. `EntityMapper` convertit les entités JPA en modèles domaine.
4. `BalanceCalculator` calcule les parts, soldes et remboursements.
5. Les données sont converties en `SummaryResponse`.

## 4. Détail fichier par fichier

### `backend/src/main/java/fr/achabitation/AchabitationApplication.java`

**Type :** class `AchabitationApplication`.  
**Rôle :** Point d’entrée Spring Boot. Démarre le backend et charge la configuration Spring.

**Méthodes :**
- `main(String[] args)` : Lance l’application Spring Boot.

### `backend/src/main/java/fr/achabitation/api/controller/ApiExceptionHandler.java`

**Type :** class `ApiExceptionHandler`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Records / DTO définis :**
- `ApiError` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `Instant timestamp`
  - `int status`
  - `String error`
  - `List<String> details`

**Méthodes :**
- `ApiError(Instant timestamp, int status, String error, List<String> details)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `unauthorized(AuthenticationCredentialsNotFoundException ex)` : Transforme une exception technique ou métier en réponse HTTP structurée.
- `forbidden(AccessDeniedException ex)` : Transforme une exception technique ou métier en réponse HTTP structurée.
- `illegalArgument(IllegalArgumentException ex)` : Transforme une exception technique ou métier en réponse HTTP structurée.
- `validation(MethodArgumentNotValidException ex)` : Transforme une exception technique ou métier en réponse HTTP structurée.
- `constraintViolation(ConstraintViolationException ex)` : Transforme une exception technique ou métier en réponse HTTP structurée.
- `dataIntegrity(DataIntegrityViolationException ex)` : Transforme une exception technique ou métier en réponse HTTP structurée.
- `unexpected(Exception ex)` : Transforme une exception technique ou métier en réponse HTTP structurée.

### `backend/src/main/java/fr/achabitation/api/controller/AuditController.java`

**Type :** class `AuditController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(AuditQueryService auditQueryService, AuthContextService authContextService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `list(@PathVariable UUID tripId, HttpServletRequest httpRequest)` : Retourne une liste de ressources accessibles à l’utilisateur courant.

### `backend/src/main/java/fr/achabitation/api/controller/AuthController.java`

**Type :** class `AuthController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(AuthService authService, AuthContextService authContextService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `register(@Valid @RequestBody RegisterRequest request)` : Crée un compte utilisateur, encode le mot de passe et retourne un token de session.
- `login(@Valid @RequestBody LoginRequest request)` : Authentifie l’utilisateur par email ou nom affiché et mot de passe, puis retourne un token de session.
- `profile(HttpServletRequest request)` : Retourne le profil du compte connecté.
- `updateAccount(HttpServletRequest request, @Valid @RequestBody AccountUpdateRequest accountRequest)` : Modifie l’email et le nom affiché du compte connecté.
- `updateProfile(HttpServletRequest request, @RequestBody UserProfileRequest profileRequest)` : Met à jour le profil financier/personnel du compte connecté sans propager automatiquement aux voyages.
- `applyProfileToLinkedPersons(HttpServletRequest request, @RequestBody ApplyProfileToLinkedPersonsRequest applyRequest)` : Applique explicitement le profil utilisateur aux personnes liées sélectionnées.

### `backend/src/main/java/fr/achabitation/api/controller/ExpenseController.java`

**Type :** class `ExpenseController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(ExpenseService expenseService, AuthContextService authContextService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `create(@PathVariable UUID tripId, @Valid @RequestBody ExpenseCreateRequest request, HttpServletRequest httpRequest)` : Crée une ressource selon le contrôleur ou le service concerné, après validation et contrôle des droits.
- `list(@PathVariable UUID tripId, HttpServletRequest httpRequest)` : Retourne une liste de ressources accessibles à l’utilisateur courant.
- `update(@PathVariable UUID tripId, @PathVariable UUID expenseId, @Valid @RequestBody ExpenseCreateRequest request, HttpServletRequest httpRequest)` : Met à jour une ressource existante avec validation métier et contrôle des droits.
- `delete(@PathVariable UUID tripId, @PathVariable UUID expenseId, HttpServletRequest httpRequest)` : Supprime ou désactive une ressource, selon le cas, après contrôle des droits.

### `backend/src/main/java/fr/achabitation/api/controller/ExportController.java`

**Type :** class `ExportController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(ExportService exportService, AuthContextService authContextService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `expensesCsv(@PathVariable UUID tripId, HttpServletRequest request)` : Construit l’export CSV des dépenses du voyage.
- `summaryCsv(@PathVariable UUID tripId, HttpServletRequest request)` : Construit l’export CSV du résumé financier et des remboursements.
- `csv(String filename, byte[] payload)` : Prépare une réponse HTTP contenant un fichier CSV téléchargeable.

### `backend/src/main/java/fr/achabitation/api/controller/HealthController.java`

**Type :** class `HealthController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(DataSource dataSource)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `health()` : Retourne un état simple de disponibilité de l’application.
- `readiness()` : Vérifie que l’application et la connexion base sont prêtes.

### `backend/src/main/java/fr/achabitation/api/controller/PersonController.java`

**Type :** class `PersonController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(PersonService personService, AuthContextService authContextService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `create(@PathVariable UUID tripId, @Valid @RequestBody PersonCreateRequest request, HttpServletRequest httpRequest)` : Crée une ressource selon le contrôleur ou le service concerné, après validation et contrôle des droits.
- `list(@PathVariable UUID tripId, HttpServletRequest httpRequest)` : Retourne une liste de ressources accessibles à l’utilisateur courant.
- `update(@PathVariable UUID tripId, @PathVariable UUID personId, @Valid @RequestBody PersonUpdateRequest request, HttpServletRequest httpRequest)` : Met à jour une ressource existante avec validation métier et contrôle des droits.
- `linkCurrentUser(@PathVariable UUID tripId, @PathVariable UUID personId, @RequestBody(required = false) LinkGuestRequest request, HttpServletRequest httpRequest)` : Lie le compte connecté à une personne guest du voyage.
- `disable(@PathVariable UUID tripId, @PathVariable UUID personId, HttpServletRequest httpRequest)` : Désactive une personne sans supprimer l’historique des dépenses.

### `backend/src/main/java/fr/achabitation/api/controller/SummaryController.java`

**Type :** class `SummaryController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(SummaryService summaryService, AuthContextService authContextService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `summary(@PathVariable UUID tripId, HttpServletRequest httpRequest)` : Retourne le résumé financier du voyage : soldes, remboursements et totaux.

### `backend/src/main/java/fr/achabitation/api/controller/TripController.java`

**Type :** class `TripController`.  
**Rôle :** Contrôleur REST. Il expose des endpoints HTTP, récupère l’utilisateur courant puis délègue la logique métier aux services applicatifs.

**Méthodes :**
- `constructeur(TripService tripService, AuthContextService authContextService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `create(@Valid @RequestBody TripCreateRequest request, HttpServletRequest httpRequest)` : Crée une ressource selon le contrôleur ou le service concerné, après validation et contrôle des droits.
- `list(HttpServletRequest httpRequest)` : Retourne une liste de ressources accessibles à l’utilisateur courant.
- `joinByCode(@RequestBody JoinTripByCodeRequest request, HttpServletRequest httpRequest)` : Permet de rejoindre un voyage à partir d’un simple code d’invitation.
- `updateConstraints(@PathVariable UUID tripId, @RequestBody TripConstraintUpdateRequest request, HttpServletRequest httpRequest)` : Met à jour la liste officielle des contraintes personnalisées du voyage.
- `join(@PathVariable UUID tripId, @RequestBody(required = false) JoinTripRequest request, HttpServletRequest httpRequest)` : Permet à un utilisateur authentifié de rejoindre un voyage avec une invitation.
- `createInvitation(@PathVariable UUID tripId, @Valid @RequestBody(required = false) TripInvitationCreateRequest request, HttpServletRequest httpRequest)` : Crée un code d’invitation avec rôle et date d’expiration.
- `listInvitations(@PathVariable UUID tripId, HttpServletRequest httpRequest)` : Liste les invitations d’un voyage.
- `revokeInvitation(@PathVariable UUID tripId, @PathVariable UUID invitationId, HttpServletRequest httpRequest)` : Révoque une invitation pour empêcher son usage futur.

### `backend/src/main/java/fr/achabitation/api/dto/AuditDtos.java`

**Type :** record `AuditLogResponse`.  
**Rôle :** Fichier DTO. Il définit les objets échangés par l’API en entrée et en sortie, avec validations de surface quand nécessaire.

**Records / DTO définis :**
- `AuditLogResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID id`
  - `AuditAction action`
  - `String entityType`
  - `UUID entityId`
  - `String description`
  - `UUID actorUserId`
  - `Instant createdAt`

**Méthodes :**
- `AuditDtos()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `constructeur(UUID id, AuditAction action, String entityType, UUID entityId, String description, UUID actorUserId, Instant createdAt)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/api/dto/AuthDtos.java`

**Type :** record `RegisterRequest`.  
**Rôle :** Fichier DTO. Il définit les objets échangés par l’API en entrée et en sortie, avec validations de surface quand nécessaire.

**Records / DTO définis :**
- `RegisterRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@Email @NotBlank String email`
  - `@NotBlank @Size(min = 2`
  - `max = 120) String displayName`
  - `@NotBlank @Size(min = 8`
  - `max = 120) String password`
- `LoginRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@NotBlank String email`
  - `@NotBlank String password`
- `AccountUpdateRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@Email @NotBlank String email`
  - `@NotBlank @Size(min = 2`
  - `max = 120) String displayName`
- `AuthResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID userId`
  - `String email`
  - `String displayName`
  - `String devToken`
  - `String note`
- `UserProfileRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `String displayName`
  - `BigDecimal livingRest`
  - `WeightMode weightMode`
  - `boolean advancedLivingRest`
  - `BigDecimal netIncomeAfterTax`
  - `BigDecimal rent`
  - `BigDecimal credits`
  - `BigDecimal fixedCharges`
  - `BigDecimal transport`
  - `BigDecimal insurance`
  - `BigDecimal otherMandatoryExpenses`
  - `BigDecimal menstrualProtection`
  - `boolean vegetarian`
  - `boolean noAlcohol`
  - `boolean livingRestPublic`
  - `Set<String> customConstraints`
- `ApplyProfileToLinkedPersonsRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `Set<UUID> personIds`
- `LinkedProfilePersonResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID personId`
  - `String personName`
  - `UUID tripId`
  - `String tripName`
- `UserProfileResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID userId`
  - `String email`
  - `String displayName`
  - `BigDecimal livingRest`
  - `WeightMode weightMode`
  - `boolean advancedLivingRest`
  - `BigDecimal netIncomeAfterTax`
  - `BigDecimal rent`
  - `BigDecimal credits`
  - `BigDecimal fixedCharges`
  - `BigDecimal transport`
  - `BigDecimal insurance`
  - `BigDecimal otherMandatoryExpenses`
  - `BigDecimal menstrualProtection`
  - `boolean vegetarian`
  - `boolean noAlcohol`
  - `boolean livingRestPublic`
  - `Set<String> knownCustomConstraints`
  - `Set<String> customConstraints`
  - `List<LinkedProfilePersonResponse> linkedPersons`

**Méthodes :**
- `AuthDtos()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `constructeur(@Email @NotBlank String email, @NotBlank @Size(min = 2, max = 120) String displayName, @NotBlank @Size(min = 8, max = 120) String password)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `LoginRequest(@NotBlank String email, @NotBlank String password)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `AccountUpdateRequest(@Email @NotBlank String email, @NotBlank @Size(min = 2, max = 120) String displayName)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `AuthResponse(UUID userId, String email, String displayName, String devToken, String note)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `UserProfileRequest(String displayName, BigDecimal livingRest, WeightMode weightMode, boolean advancedLivingRest, BigDecimal netIncomeAfterTax, BigDecimal rent, BigDecimal credits, BigDecimal fixedCharges, BigDecimal transport, BigDecimal insurance, BigDecimal otherMandatoryExpenses, BigDecimal menstrualProtection, boolean vegetarian, boolean noAlcohol, boolean livingRestPublic, Set<String> customConstraints)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `ApplyProfileToLinkedPersonsRequest(Set<UUID> personIds)` : Applique des valeurs entrantes sur une entité ou un profil en respectant les règles métier.
- `LinkedProfilePersonResponse(UUID personId, String personName, UUID tripId, String tripName)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `UserProfileResponse(UUID userId, String email, String displayName, BigDecimal livingRest, WeightMode weightMode, boolean advancedLivingRest, BigDecimal netIncomeAfterTax, BigDecimal rent, BigDecimal credits, BigDecimal fixedCharges, BigDecimal transport, BigDecimal insurance, BigDecimal otherMandatoryExpenses, BigDecimal menstrualProtection, boolean vegetarian, boolean noAlcohol, boolean livingRestPublic, Set<String> knownCustomConstraints, Set<String> customConstraints, List<LinkedProfilePersonResponse> linkedPersons)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/api/dto/ExpenseDtos.java`

**Type :** record `ExpenseCreateRequest`.  
**Rôle :** Fichier DTO. Il définit les objets échangés par l’API en entrée et en sortie, avec validations de surface quand nécessaire.

**Records / DTO définis :**
- `ExpenseCreateRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@NotBlank @Size(max = 180) String title`
  - `@NotNull LocalDate date`
  - `@NotNull UUID payerPersonId`
  - `@NotNull @Positive BigDecimal totalAmount`
  - `@PositiveOrZero BigDecimal meatAmount`
  - `@PositiveOrZero BigDecimal alcoholAmount`
  - `Map<String, BigDecimal> customConstraintAmounts`
  - `ExpenseType type`
  - `boolean advancedMode`
  - `Set<UUID> manualParticipantIds`
  - `@Size(min = 3`
  - `max = 3) String currency`
  - `@Positive BigDecimal exchangeRateToTripCurrency`
- `ExpenseResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID id`
  - `String title`
  - `LocalDate date`
  - `UUID payerPersonId`
  - `String payerName`
  - `BigDecimal totalAmount`
  - `BigDecimal meatAmount`
  - `BigDecimal alcoholAmount`
  - `Map<String, BigDecimal> customConstraintAmounts`
  - `ExpenseType type`
  - `boolean advancedMode`
  - `Set<UUID> manualParticipantIds`
  - `String currency`
  - `BigDecimal exchangeRateToTripCurrency`

**Méthodes :**
- `ExpenseDtos()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `constructeur(@NotBlank @Size(max = 180) String title, @NotNull LocalDate date, @NotNull UUID payerPersonId, @NotNull @Positive BigDecimal totalAmount, @PositiveOrZero BigDecimal meatAmount, @PositiveOrZero BigDecimal alcoholAmount, Map<String, BigDecimal> customConstraintAmounts, ExpenseType type, boolean advancedMode, Set<UUID> manualParticipantIds, @Size(min = 3, max = 3) String currency, @Positive BigDecimal exchangeRateToTripCurrency)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `ExpenseResponse(UUID id, String title, LocalDate date, UUID payerPersonId, String payerName, BigDecimal totalAmount, BigDecimal meatAmount, BigDecimal alcoholAmount, Map<String, BigDecimal> customConstraintAmounts, ExpenseType type, boolean advancedMode, Set<UUID> manualParticipantIds, String currency, BigDecimal exchangeRateToTripCurrency)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/api/dto/PersonDtos.java`

**Type :** record `PresencePeriodRequest`.  
**Rôle :** Fichier DTO. Il définit les objets échangés par l’API en entrée et en sortie, avec validations de surface quand nécessaire.

**Records / DTO définis :**
- `PresencePeriodRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@NotNull LocalDate startDate`
  - `@NotNull LocalDate endDate`
- `PresencePeriodResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `LocalDate startDate`
  - `LocalDate endDate`
- `PersonCreateRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@NotBlank @Size(max = 120) String name`
  - `BigDecimal livingRest`
  - `WeightMode weightMode`
  - `boolean advancedLivingRest`
  - `BigDecimal netIncomeAfterTax`
  - `BigDecimal rent`
  - `BigDecimal credits`
  - `BigDecimal fixedCharges`
  - `BigDecimal transport`
  - `BigDecimal insurance`
  - `BigDecimal otherMandatoryExpenses`
  - `BigDecimal menstrualProtection`
  - `boolean vegetarian`
  - `boolean noAlcohol`
  - `boolean livingRestPublic`
  - `Set<String> customConstraints`
  - `@Valid @NotEmpty List<PresencePeriodRequest> presencePeriods`
- `PersonUpdateRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@NotBlank @Size(max = 120) String name`
  - `BigDecimal livingRest`
  - `WeightMode weightMode`
  - `boolean advancedLivingRest`
  - `BigDecimal netIncomeAfterTax`
  - `BigDecimal rent`
  - `BigDecimal credits`
  - `BigDecimal fixedCharges`
  - `BigDecimal transport`
  - `BigDecimal insurance`
  - `BigDecimal otherMandatoryExpenses`
  - `BigDecimal menstrualProtection`
  - `boolean vegetarian`
  - `boolean noAlcohol`
  - `boolean livingRestPublic`
  - `Set<String> customConstraints`
  - `boolean active`
  - `@Valid @NotEmpty List<PresencePeriodRequest> presencePeriods`
- `LinkGuestRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID userId`
  - `boolean applyProfileToGuest`
- `PersonResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID id`
  - `String name`
  - `UUID linkedUserId`
  - `String linkedUserEmail`
  - `boolean guest`
  - `BigDecimal livingRest`
  - `boolean livingRestHidden`
  - `boolean livingRestPublic`
  - `boolean canEditFinancialProfile`
  - `WeightMode weightMode`
  - `boolean advancedLivingRest`
  - `BigDecimal netIncomeAfterTax`
  - `BigDecimal rent`
  - `BigDecimal credits`
  - `BigDecimal fixedCharges`
  - `BigDecimal transport`
  - `BigDecimal insurance`
  - `BigDecimal otherMandatoryExpenses`
  - `BigDecimal menstrualProtection`
  - `boolean vegetarian`
  - `boolean noAlcohol`
  - `Set<String> customConstraints`
  - `boolean active`
  - `List<PresencePeriodResponse> presencePeriods`

**Méthodes :**
- `PersonDtos()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `constructeur(@NotNull LocalDate startDate, @NotNull LocalDate endDate)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `PresencePeriodResponse(LocalDate startDate, LocalDate endDate)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `PersonCreateRequest(@NotBlank @Size(max = 120) String name, BigDecimal livingRest, WeightMode weightMode, boolean advancedLivingRest, BigDecimal netIncomeAfterTax, BigDecimal rent, BigDecimal credits, BigDecimal fixedCharges, BigDecimal transport, BigDecimal insurance, BigDecimal otherMandatoryExpenses, BigDecimal menstrualProtection, boolean vegetarian, boolean noAlcohol, boolean livingRestPublic, Set<String> customConstraints, @Valid @NotEmpty List<PresencePeriodRequest> presencePeriods)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `PersonUpdateRequest(@NotBlank @Size(max = 120) String name, BigDecimal livingRest, WeightMode weightMode, boolean advancedLivingRest, BigDecimal netIncomeAfterTax, BigDecimal rent, BigDecimal credits, BigDecimal fixedCharges, BigDecimal transport, BigDecimal insurance, BigDecimal otherMandatoryExpenses, BigDecimal menstrualProtection, boolean vegetarian, boolean noAlcohol, boolean livingRestPublic, Set<String> customConstraints, boolean active, @Valid @NotEmpty List<PresencePeriodRequest> presencePeriods)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `LinkGuestRequest(UUID userId, boolean applyProfileToGuest)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `PersonResponse(UUID id, String name, UUID linkedUserId, String linkedUserEmail, boolean guest, BigDecimal livingRest, boolean livingRestHidden, boolean livingRestPublic, boolean canEditFinancialProfile, WeightMode weightMode, boolean advancedLivingRest, BigDecimal netIncomeAfterTax, BigDecimal rent, BigDecimal credits, BigDecimal fixedCharges, BigDecimal transport, BigDecimal insurance, BigDecimal otherMandatoryExpenses, BigDecimal menstrualProtection, boolean vegetarian, boolean noAlcohol, Set<String> customConstraints, boolean active, List<PresencePeriodResponse> presencePeriods)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/api/dto/SummaryDtos.java`

**Type :** record `BalanceResponse`.  
**Rôle :** Fichier DTO. Il définit les objets échangés par l’API en entrée et en sortie, avec validations de surface quand nécessaire.

**Records / DTO définis :**
- `BalanceResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID personId`
  - `String personName`
  - `BigDecimal totalPaid`
  - `BigDecimal totalOwed`
  - `BigDecimal balance`
- `SettlementResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID fromPersonId`
  - `String fromPersonName`
  - `UUID toPersonId`
  - `String toPersonName`
  - `BigDecimal amount`
- `SummaryResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `String referenceCurrency`
  - `List<BalanceResponse> balances`
  - `List<SettlementResponse> settlements`

**Méthodes :**
- `SummaryDtos()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `constructeur(UUID personId, String personName, BigDecimal totalPaid, BigDecimal totalOwed, BigDecimal balance)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `SettlementResponse(UUID fromPersonId, String fromPersonName, UUID toPersonId, String toPersonName, BigDecimal amount)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `SummaryResponse(String referenceCurrency, List<BalanceResponse> balances, List<SettlementResponse> settlements)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/api/dto/TripDtos.java`

**Type :** record `TripCreateRequest`.  
**Rôle :** Fichier DTO. Il définit les objets échangés par l’API en entrée et en sortie, avec validations de surface quand nécessaire.

**Records / DTO définis :**
- `TripCreateRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `@NotBlank @Size(max = 160) String name`
  - `@NotNull LocalDate startDate`
  - `@NotNull LocalDate endDate`
  - `@Size(min = 3`
  - `max = 3) String referenceCurrency`
  - `UUID ownerUserId`
  - `Set<String> customConstraints`
- `TripConstraintUpdateRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `Set<String> customConstraints`
- `JoinTripRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID guestPersonId`
  - `boolean applyProfileToGuest`
  - `String invitationCode`
- `JoinTripByCodeRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID guestPersonId`
  - `boolean applyProfileToGuest`
  - `String invitationCode`
- `TripInvitationCreateRequest` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `TripRole roleToGrant`
  - `@Min(1) @Max(30) Integer expiresInDays`
- `TripInvitationResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID id`
  - `UUID tripId`
  - `String code`
  - `TripRole roleToGrant`
  - `Instant createdAt`
  - `Instant expiresAt`
  - `boolean revoked`
  - `boolean usable`
- `TripResponse` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID id`
  - `String name`
  - `@NotNull LocalDate startDate`
  - `@NotNull LocalDate endDate`
  - `String referenceCurrency`
  - `Set<String> customConstraints`
  - `boolean active`

**Méthodes :**
- `TripDtos()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `constructeur(@NotBlank @Size(max = 160) String name, @NotNull LocalDate startDate, @NotNull LocalDate endDate, @Size(min = 3, max = 3) String referenceCurrency, UUID ownerUserId, Set<String> customConstraints)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `TripConstraintUpdateRequest(Set<String> customConstraints)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `JoinTripRequest(UUID guestPersonId, boolean applyProfileToGuest, String invitationCode)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `JoinTripByCodeRequest(UUID guestPersonId, boolean applyProfileToGuest, String invitationCode)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `TripInvitationCreateRequest(TripRole roleToGrant, @Min(1) @Max(30) Integer expiresInDays)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `TripInvitationResponse(UUID id, UUID tripId, String code, TripRole roleToGrant, Instant createdAt, Instant expiresAt, boolean revoked, boolean usable)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `TripResponse(UUID id, String name, @NotNull LocalDate startDate, @NotNull LocalDate endDate, String referenceCurrency, Set<String> customConstraints, boolean active)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/application/AuditQueryService.java`

**Type :** class `AuditQueryService`.  
**Rôle :** Service de lecture de l’historique d’audit d’un voyage, avec contrôle d’accès.

**Méthodes :**
- `constructeur(AuditLogRepository auditLogRepository, EntityMapper mapper, AuthorizationService authorizationService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `list(UUID tripId, UserEntity actor)` : Retourne une liste de ressources accessibles à l’utilisateur courant.

### `backend/src/main/java/fr/achabitation/application/AuditService.java`

**Type :** class `AuditService`.  
**Rôle :** Service d’écriture de l’historique. Il centralise la création des événements d’audit.

**Méthodes :**
- `constructeur(AuditLogRepository auditLogRepository)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `log(TripEntity trip, UserEntity actor, AuditAction action, String entityType, UUID entityId, String description)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/application/AuthContextService.java`

**Type :** class `AuthContextService`.  
**Rôle :** Service qui extrait le token Bearer de la requête HTTP et retrouve l’utilisateur connecté.

**Méthodes :**
- `constructeur(UserRepository userRepository)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `optionalUser(HttpServletRequest request)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `requiredUser(HttpServletRequest request)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.
- `tokenStillValid(UserEntity user)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `tokenFromRequest(HttpServletRequest request)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.

### `backend/src/main/java/fr/achabitation/application/AuthService.java`

**Type :** class `AuthService`.  
**Rôle :** Service de gestion des comptes : inscription, connexion, profil, RAV personnel, contraintes et propagation volontaire du profil.

**Méthodes :**
- `constructeur(UserRepository userRepository, PersonRepository personRepository, PasswordEncoder passwordEncoder, EntityMapper mapper)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `normalizedEmail(String email)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `normalizedDisplayName(String displayName)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `ensureValidDisplayName(String displayName)` : Garantit une précondition métier avant de poursuivre le traitement.
- `findUserByEmailOrDisplayName(String identifier)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `register(RegisterRequest request)` : Crée un compte utilisateur, encode le mot de passe et retourne un token de session.
- `login(LoginRequest request)` : Authentifie l’utilisateur par email ou nom affiché et mot de passe, puis retourne un token de session.
- `updateAccount(UserEntity user, AccountUpdateRequest request)` : Modifie l’email et le nom affiché du compte connecté.
- `profile(UserEntity user)` : Retourne le profil du compte connecté.
- `updateProfile(UserEntity user, UserProfileRequest request)` : Met à jour le profil financier/personnel du compte connecté sans propager automatiquement aux voyages.
- `applyProfileToLinkedPersons(UserEntity user, ApplyProfileToLinkedPersonsRequest request)` : Applique explicitement le profil utilisateur aux personnes liées sélectionnées.
- `applyFinancialAndConstraintProfile(UserEntity user, UserProfileRequest request)` : Applique des valeurs entrantes sur une entité ou un profil en respectant les règles métier.
- `applyUserProfileToPerson(UserEntity user, PersonEntity person)` : Applique des valeurs entrantes sur une entité ou un profil en respectant les règles métier.
- `profileConstraintsAllowedInTrip(UserEntity user, PersonEntity person)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `normalizeConstraints(Set<String> constraints)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `replaceUserCustomConstraints(UserEntity user, Set<String> requested)` : Remplace une collection ou un groupe de champs en conservant la cohérence Hibernate/métier.
- `computeLivingRest(boolean advanced, BigDecimal requestLivingRest, UserEntity user)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `ensureValidUserProfile(UserEntity user)` : Garantit une précondition métier avant de poursuivre le traitement.
- `validateUserProfileForStorage(UserEntity user)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.
- `toAuthResponse(UserEntity user, String note)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `linkedPersons(UserEntity user)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `toProfileResponse(UserEntity user)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.

### `backend/src/main/java/fr/achabitation/application/AuthorizationService.java`

**Type :** class `AuthorizationService`.  
**Rôle :** Service de contrôle des droits. Il vérifie les rôles et empêche les accès ou modifications non autorisés.

**Méthodes :**
- `constructeur(TripMemberRepository tripMemberRepository, PersonRepository personRepository)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `requireAuthenticated(UserEntity user)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.
- `requireTripMember(UUID tripId, UserEntity user)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.
- `requireReadable(UUID tripId, UserEntity user)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.
- `requireWritable(UUID tripId, UserEntity user)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.
- `requireAdmin(UUID tripId, UserEntity user)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.
- `isAdmin(UUID tripId, UserEntity user)` : Retourne l’état booléen `admin`.
- `isLinkedPersonOwner(PersonEntity person, UserEntity user)` : Retourne l’état booléen `linkedPersonOwner`.
- `requirePersonUpdateAllowed(PersonEntity person, UserEntity user)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.
- `canEditFinancialProfile(PersonEntity person, UserEntity user)` : Retourne un booléen indiquant si l’action est autorisée dans le contexte courant.
- `readableTripIds(UserEntity user)` : Retourne les identifiants de voyages lisibles par l’utilisateur courant.
- `requirePersonInReadableTrip(UUID tripId, UUID personId, UserEntity user)` : Méthode de garde : vérifie une condition de sécurité/droit et lève une exception si elle n’est pas respectée.

### `backend/src/main/java/fr/achabitation/application/EntityMapper.java`

**Type :** class `EntityMapper`.  
**Rôle :** Service de conversion entre entités JPA, modèles domaine et DTO API. Il applique aussi certaines règles de masquage du RAV.

**Méthodes :**
- `normalizeName(String input)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `money(BigDecimal value)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `positiveRate(BigDecimal value)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `moneyMap(Map<String, BigDecimal> input)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `stringSet(Set<String> input)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `toTripResponse(TripEntity trip)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `toPersonResponse(PersonEntity person)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `toPersonResponse(PersonEntity person, UserEntity viewer)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `toExpenseResponse(ExpenseEntity expense)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `toDomainPerson(PersonEntity person)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `toDomainExpense(ExpenseEntity expense)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `toAuditResponse(AuditLogEntity log)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.

### `backend/src/main/java/fr/achabitation/application/ExpenseService.java`

**Type :** class `ExpenseService`.  
**Rôle :** Service métier de gestion des dépenses : création, modification, suppression, validation, montants par contraintes.

**Méthodes :**
- `constructeur(ExpenseRepository expenseRepository, PersonRepository personRepository, PersonService personService, TripService tripService, EntityMapper mapper, AuditService auditService, AuthorizationService authorizationService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `create(UUID tripId, ExpenseCreateRequest request, UserEntity actor)` : Crée une ressource selon le contrôleur ou le service concerné, après validation et contrôle des droits.
- `update(UUID tripId, UUID expenseId, ExpenseCreateRequest request, UserEntity actor)` : Met à jour une ressource existante avec validation métier et contrôle des droits.
- `delete(UUID tripId, UUID expenseId, UserEntity actor)` : Supprime ou désactive une ressource, selon le cas, après contrôle des droits.
- `list(UUID tripId, UserEntity actor)` : Retourne une liste de ressources accessibles à l’utilisateur courant.
- `apply(ExpenseEntity expense, ExpenseCreateRequest request, UUID tripId)` : Applique des valeurs entrantes sur une entité ou un profil en respectant les règles métier.
- `normalizeCustomConstraintAmounts(TripEntity trip, Map<String, BigDecimal> input)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `validateExpenseDateWithinTrip(ExpenseEntity expense)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.
- `validateExpense(ExpenseEntity expense, UUID tripId)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.

### `backend/src/main/java/fr/achabitation/application/ExportService.java`

**Type :** class `ExportService`.  
**Rôle :** Service d’export CSV des dépenses et du résumé.

**Méthodes :**
- `constructeur(ExpenseRepository expenseRepository, TripService tripService, SummaryService summaryService, AuthorizationService authorizationService, AuditService auditService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `expensesCsv(UUID tripId, UserEntity actor)` : Construit l’export CSV des dépenses du voyage.
- `summaryCsv(UUID tripId, UserEntity actor)` : Construit l’export CSV du résumé financier et des remboursements.
- `number(BigDecimal value)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `cell(String value)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/application/PersonService.java`

**Type :** class `PersonService`.  
**Rôle :** Service métier des personnes du voyage : guests, personnes liées à un compte, présence, RAV, contraintes.

**Méthodes :**
- `constructeur(PersonRepository personRepository, TripService tripService, EntityMapper mapper, AuditService auditService, AuthorizationService authorizationService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `create(UUID tripId, PersonCreateRequest request, UserEntity viewer)` : Crée une ressource selon le contrôleur ou le service concerné, après validation et contrôle des droits.
- `update(UUID tripId, UUID personId, PersonUpdateRequest request, UserEntity viewer)` : Met à jour une ressource existante avec validation métier et contrôle des droits.
- `linkToCurrentUser(UUID tripId, UUID personId, UserEntity user)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `linkToCurrentUser(UUID tripId, UUID personId, UserEntity user, boolean applyProfileToGuest)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `disable(UUID tripId, UUID personId, UserEntity viewer)` : Désactive une personne sans supprimer l’historique des dépenses.
- `list(UUID tripId, UserEntity viewer)` : Retourne une liste de ressources accessibles à l’utilisateur courant.
- `getRequiredInTrip(UUID tripId, UUID personId)` : Retourne la valeur de l’attribut `requiredInTrip`.
- `canEditFinancialProfile(PersonEntity person, UserEntity viewer)` : Retourne un booléen indiquant si l’action est autorisée dans le contexte courant.
- `applyCreate(PersonEntity person, PersonCreateRequest request, String normalizedName)` : Applique des valeurs entrantes sur une entité ou un profil en respectant les règles métier.
- `applyUpdate(PersonEntity person, PersonUpdateRequest request, String normalizedName, boolean canEditFinancialProfile)` : Applique des valeurs entrantes sur une entité ou un profil en respectant les règles métier.
- `applyFinancialFields(PersonEntity person, WeightMode weightMode, boolean advancedLivingRest, BigDecimal livingRest, BigDecimal netIncomeAfterTax, BigDecimal rent, BigDecimal credits, BigDecimal fixedCharges, BigDecimal transport, BigDecimal insurance, BigDecimal otherMandatoryExpenses, BigDecimal menstrualProtection, boolean livingRestPublic, boolean vegetarian, boolean noAlcohol, Set<String> customConstraints)` : Applique des valeurs entrantes sur une entité ou un profil en respectant les règles métier.
- `replaceCustomConstraints(PersonEntity person, Set<String> constraints)` : Remplace une collection ou un groupe de champs en conservant la cohérence Hibernate/métier.
- `normalizeCustomConstraintsForTrip(TripEntity trip, Set<String> constraints)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `computeLivingRest(boolean advanced, BigDecimal requestLivingRest, PersonEntity person)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `replacePresencePeriods(PersonEntity person, List<PresencePeriodRequest> periods)` : Remplace une collection ou un groupe de champs en conservant la cohérence Hibernate/métier.
- `validatePresencePeriods(TripEntity trip, List<PresencePeriodRequest> periods)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.
- `validatePerson(PersonEntity person)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.

### `backend/src/main/java/fr/achabitation/application/SummaryService.java`

**Type :** class `SummaryService`.  
**Rôle :** Service qui construit le résumé d’un voyage à partir des personnes, dépenses et calculateur domaine.

**Méthodes :**
- `constructeur(PersonRepository personRepository, ExpenseRepository expenseRepository, TripService tripService, EntityMapper mapper, AuthorizationService authorizationService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `summary(UUID tripId, UserEntity actor)` : Retourne le résumé financier du voyage : soldes, remboursements et totaux.

### `backend/src/main/java/fr/achabitation/application/TripService.java`

**Type :** class `TripService`.  
**Rôle :** Service métier des voyages : création, contraintes du voyage, invitations, adhésion et liaison guest/compte.

**Méthodes :**
- `constructeur(TripRepository tripRepository, TripMemberRepository tripMemberRepository, TripInvitationRepository tripInvitationRepository, UserRepository userRepository, PersonRepository personRepository, EntityMapper mapper, AuditService auditService, AuthService authService, AuthorizationService authorizationService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `create(TripCreateRequest request, UserEntity owner)` : Crée une ressource selon le contrôleur ou le service concerné, après validation et contrôle des droits.
- `updateConstraints(UUID tripId, TripConstraintUpdateRequest request, UserEntity actor)` : Met à jour la liste officielle des contraintes personnalisées du voyage.
- `joinByCode(JoinTripByCodeRequest request, UserEntity user)` : Permet de rejoindre un voyage à partir d’un simple code d’invitation.
- `join(UUID tripId, JoinTripRequest request, UserEntity user)` : Permet à un utilisateur authentifié de rejoindre un voyage avec une invitation.
- `linkGuestToUser(UUID tripId, UUID personId, UserEntity user)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `linkGuestToUser(UUID tripId, UUID personId, UserEntity user, boolean applyProfileToGuest)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `createInvitation(UUID tripId, TripInvitationCreateRequest request, UserEntity actor)` : Crée un code d’invitation avec rôle et date d’expiration.
- `listInvitations(UUID tripId, UserEntity actor)` : Liste les invitations d’un voyage.
- `revokeInvitation(UUID tripId, UUID invitationId, UserEntity actor)` : Révoque une invitation pour empêcher son usage futur.
- `validateInvitationByCode(String code)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.
- `validateInvitation(UUID tripId, String code)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.
- `toInvitationResponse(TripInvitationEntity invitation)` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `generateInvitationCode()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `addMemberIfMissing(TripEntity trip, UserEntity user, TripRole role)` : Ajoute un élément ou une allocation dans une collection métier.
- `replaceTripConstraints(TripEntity trip, Set<String> constraints)` : Remplace une collection ou un groupe de champs en conservant la cohérence Hibernate/métier.
- `validateTripDates(TripCreateRequest request)` : Valide une règle métier et lève une exception explicite si la donnée est incohérente.
- `list(UserEntity user)` : Retourne une liste de ressources accessibles à l’utilisateur courant.
- `getRequired(UUID tripId)` : Retourne la valeur de l’attribut `required`.

### `backend/src/main/java/fr/achabitation/config/SecurityConfig.java`

**Type :** class `SecurityConfig`.  
**Rôle :** Configuration Spring Security et beans techniques de sécurité.

**Méthodes :**
- `passwordEncoder()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `securityFilterChain(HttpSecurity http)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/domain/BalanceCalculator.java`

**Type :** class `BalanceCalculator`.  
**Rôle :** Moteur de calcul métier pur : répartition au RAV, mode moyenne, dépenses globales/avancées, soldes et remboursements.

**Méthodes :**
- `calculateSharesForExpense(DomainExpense expense, List<DomainPerson> persons)` : Calcule les parts dues par personne pour une dépense donnée.
- `calculateBalances(List<DomainExpense> expenses, List<DomainPerson> persons)` : Agrège toutes les dépenses pour calculer payé, dû et solde de chaque personne.
- `calculateSettlements(List<Balance> balances)` : Transforme les soldes en remboursements minimaux entre débiteurs et créditeurs.
- `validateExpenseHasParticipants(DomainExpense expense, List<DomainPerson> persons)` : Vérifie qu’une dépense a bien au moins une personne concernée pour chaque bloc utile.
- `customAmountsInTripCurrency(DomainExpense expense, BigDecimal rate)` : Convertit les montants associés aux contraintes personnalisées dans la devise de référence du voyage.
- `safeCustomAmounts(DomainExpense expense)` : Sécurise une valeur potentiellement nulle ou invalide avec une valeur par défaut contrôlée.
- `addAllocation(Map<UUID, BigDecimal> shares, BigDecimal amount, List<DomainPerson> participants)` : Ajoute un élément ou une allocation dans une collection métier.
- `allocateProportionally(BigDecimal amount, List<DomainPerson> participants)` : Répartit un montant entre les personnes éligibles selon leur poids effectif.
- `calculateEffectiveWeights(List<DomainPerson> eligible)` : Calcule le poids de chaque participant en tenant compte du mode RAV ou du mode moyenne.
- `canParticipateInAllocation(DomainPerson person)` : Indique si une personne peut entrer dans une allocation, notamment avec un RAV positif ou un mode moyenne.
- `hasPositiveLivingRest(DomainPerson person)` : Indique si le RAV de la personne est strictement positif.
- `amountInTripCurrency(BigDecimal amount, BigDecimal exchangeRateToTripCurrency)` : Retourne ou convertit un montant monétaire utilisé par le calcul.
- `safeRate(BigDecimal rate)` : Sécurise une valeur potentiellement nulle ou invalide avec une valeur par défaut contrôlée.
- `safe(BigDecimal value)` : Sécurise une valeur potentiellement nulle ou invalide avec une valeur par défaut contrôlée.
- `scale(BigDecimal value)` : Applique l’arrondi monétaire au centime.
- `AllocationLine(UUID personId, long cents, BigDecimal remainder)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `remainder()` : Calcule le reste d’arrondi à redistribuer pour conserver le total exact.
- `MutableAmount(DomainPerson person, BigDecimal amount)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `amount()` : Retourne ou convertit un montant monétaire utilisé par le calcul.

### `backend/src/main/java/fr/achabitation/domain/model/Balance.java`

**Type :** record `Balance`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

**Records / DTO définis :**
- `Balance` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `DomainPerson person`
  - `BigDecimal totalPaid`
  - `BigDecimal totalOwed`
  - `BigDecimal balance`

**Méthodes :**
- `constructeur(DomainPerson person, BigDecimal totalPaid, BigDecimal totalOwed, BigDecimal balance)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/domain/model/DomainExpense.java`

**Type :** record `DomainExpense`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

**Records / DTO définis :**
- `DomainExpense` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID id`
  - `String title`
  - `LocalDate date`
  - `UUID payerId`
  - `BigDecimal totalAmount`
  - `BigDecimal meatAmount`
  - `BigDecimal alcoholAmount`
  - `Map<String, BigDecimal> customConstraintAmounts`
  - `ExpenseType type`
  - `boolean advancedMode`
  - `Set<UUID> manualParticipantIds`
  - `String currency`
  - `BigDecimal exchangeRateToTripCurrency`

**Méthodes :**
- `constructeur(UUID id, String title, LocalDate date, UUID payerId, BigDecimal totalAmount, BigDecimal meatAmount, BigDecimal alcoholAmount, Map<String, BigDecimal> customConstraintAmounts, ExpenseType type, boolean advancedMode, Set<UUID> manualParticipantIds, String currency, BigDecimal exchangeRateToTripCurrency)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/domain/model/DomainPerson.java`

**Type :** record `DomainPerson`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

**Records / DTO définis :**
- `DomainPerson` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `UUID id`
  - `String name`
  - `BigDecimal livingRest`
  - `WeightMode weightMode`
  - `boolean vegetarian`
  - `boolean noAlcohol`
  - `Set<String> customConstraints`
  - `boolean active`
  - `List<PresencePeriod> presencePeriods`

**Méthodes :**
- `constructeur(UUID id, String name, BigDecimal livingRest, WeightMode weightMode, boolean vegetarian, boolean noAlcohol, Set<String> customConstraints, boolean active, List<PresencePeriod> presencePeriods)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `isPresentOn(LocalDate date)` : Retourne l’état booléen `presentOn`.
- `usesAverageWeight()` : Indique si la personne utilise le mode poids moyen au lieu de son RAV.
- `hasCustomConstraint(String constraintName)` : Indique si la personne a coché une contrainte personnalisée.

### `backend/src/main/java/fr/achabitation/domain/model/ExpenseType.java`

**Type :** enum `ExpenseType`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

**Constantes enum :**
- `NORMAL`
- `GLOBAL`

Aucune méthode déclarée explicitement dans ce fichier. Son rôle est porté par son type, ses annotations ou ses constantes.

### `backend/src/main/java/fr/achabitation/domain/model/PresencePeriod.java`

**Type :** record `PresencePeriod`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

**Records / DTO définis :**
- `PresencePeriod` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `LocalDate startDate`
  - `LocalDate endDate`

**Méthodes :**
- `constructeur(LocalDate startDate, LocalDate endDate)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `contains(LocalDate date)` : Indique si une date donnée appartient à la période de présence.

### `backend/src/main/java/fr/achabitation/domain/model/Settlement.java`

**Type :** record `Settlement`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

**Records / DTO définis :**
- `Settlement` : structure immutable utilisée pour transporter des données. Champs principaux :
  - `DomainPerson from`
  - `DomainPerson to`
  - `BigDecimal amount`

**Méthodes :**
- `constructeur(DomainPerson from, DomainPerson to, BigDecimal amount)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/main/java/fr/achabitation/domain/model/WeightMode.java`

**Type :** enum `WeightMode`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

**Constantes enum :**
- `LIVING_REST`
- `AVERAGE`

Aucune méthode déclarée explicitement dans ce fichier. Son rôle est porté par son type, ses annotations ou ses constantes.

### `backend/src/main/java/fr/achabitation/domain/util/ConstraintNameUtils.java`

**Type :** classe `?`.  
**Rôle :** Utilitaire métier pur pour normaliser les contraintes personnalisées.

**Méthodes :**
- `ConstraintNameUtils()` : Transforme une exception technique ou métier en réponse HTTP structurée.
- `canonicalDisplayName(String input)` : Normalise un libellé de contrainte pour affichage : trim, espaces, casse.
- `key(String input)` : Produit une clé technique de comparaison insensible aux accents et à la casse.
- `normalizedRawKey(String input)` : Normalise une chaîne brute pour comparaison technique.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/AuditAction.java`

**Type :** enum `AuditAction`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Constantes enum :**
- `USER_REGISTERED`
- `TRIP_CREATED`
- `TRIP_UPDATED`
- `TRIP_MEMBER_JOINED`
- `PERSON_CREATED`
- `PERSON_UPDATED`
- `PERSON_DISABLED`
- `PERSON_LINKED_TO_USER`
- `EXPENSE_CREATED`
- `EXPENSE_UPDATED`
- `EXPENSE_DELETED`
- `INVITATION_CREATED`
- `INVITATION_REVOKED`
- `EXPORT_GENERATED`

### `backend/src/main/java/fr/achabitation/infrastructure/entity/AuditLogEntity.java`

**Type :** class `AuditLogEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `trip`, `actor`, `action`, `entityType`, `entityId`, `description`, `createdAt`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/ExpenseEntity.java`

**Type :** class `ExpenseEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `trip`, `title`, `date`, `payer`, `totalAmount`, `meatAmount`, `alcoholAmount`, `customConstraintAmounts`, `type`, `advancedMode`, `currency`, `exchangeRateToTripCurrency`, `manualParticipants`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/ExpenseParticipantEntity.java`

**Type :** class `ExpenseParticipantEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `expense`, `person`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/PersonEntity.java`

**Type :** class `PersonEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `trip`, `name`, `linkedUser`, `normalizedName`, `livingRest`, `weightMode`, `advancedLivingRest`, `netIncomeAfterTax`, `rent`, `credits`, `fixedCharges`, `transport`, `insurance`, `otherMandatoryExpenses`, `menstrualProtection`, `livingRestPublic`, `vegetarian`, `noAlcohol`, `customConstraints`, `active`, `presencePeriods`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/PresencePeriodEntity.java`

**Type :** class `PresencePeriodEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `person`, `startDate`, `endDate`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/TripEntity.java`

**Type :** class `TripEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `name`, `startDate`, `endDate`, `referenceCurrency`, `customConstraints`, `active`, `createdAt`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/TripInvitationEntity.java`

**Type :** class `TripInvitationEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `trip`, `createdBy`, `code`, `roleToGrant`, `createdAt`, `expiresAt`, `revokedAt`, `usable`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/TripMemberEntity.java`

**Type :** class `TripMemberEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `trip`, `user`, `role`, `joinedAt`.

### `backend/src/main/java/fr/achabitation/infrastructure/entity/TripRole.java`

**Type :** enum `TripRole`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Constantes enum :**
- `OWNER`
- `ADMIN`
- `PARTICIPANT`
- `READ_ONLY`

### `backend/src/main/java/fr/achabitation/infrastructure/entity/UserEntity.java`

**Type :** class `UserEntity`.  
**Rôle :** Entité JPA persistée en base. Elle décrit une table ou une relation utilisée par Hibernate.

**Getters / setters JPA :**
- Ces méthodes exposent et modifient les champs persistés par Hibernate. Elles sont nécessaires pour que JPA puisse hydrater les entités et pour que les services puissent appliquer les règles métier. Champs couverts : `id`, `email`, `displayName`, `passwordHash`, `sessionToken`, `sessionTokenIssuedAt`, `livingRest`, `weightMode`, `advancedLivingRest`, `netIncomeAfterTax`, `rent`, `credits`, `fixedCharges`, `transport`, `insurance`, `otherMandatoryExpenses`, `menstrualProtection`, `vegetarian`, `noAlcohol`, `livingRestPublic`, `knownCustomConstraints`, `customConstraints`, `createdAt`.

### `backend/src/main/java/fr/achabitation/infrastructure/repository/AuditLogRepository.java`

**Type :** interface `AuditLogRepository`.  
**Rôle :** Repository Spring Data JPA. Il fournit l’accès base et les requêtes dérivées à partir des noms de méthodes.

**Méthodes repository :**
- `findByTripIdOrderByCreatedAtDesc(UUID tripId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.

### `backend/src/main/java/fr/achabitation/infrastructure/repository/ExpenseRepository.java`

**Type :** interface `ExpenseRepository`.  
**Rôle :** Repository Spring Data JPA. Il fournit l’accès base et les requêtes dérivées à partir des noms de méthodes.

**Méthodes repository :**
- `findByTripIdOrderByDateAscTitleAsc(UUID tripId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.

### `backend/src/main/java/fr/achabitation/infrastructure/repository/PersonRepository.java`

**Type :** interface `PersonRepository`.  
**Rôle :** Repository Spring Data JPA. Il fournit l’accès base et les requêtes dérivées à partir des noms de méthodes.

**Méthodes repository :**
- `findByTripIdOrderByNameAsc(UUID tripId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `findByTripIdAndNormalizedName(UUID tripId, String normalizedName)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `existsByTripIdAndNormalizedName(UUID tripId, String normalizedName)` : Teste l’existence d’une donnée en base selon les critères indiqués dans le nom de méthode.
- `findByLinkedUserId(UUID userId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `findByTripIdAndLinkedUserId(UUID tripId, UUID userId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.

### `backend/src/main/java/fr/achabitation/infrastructure/repository/TripInvitationRepository.java`

**Type :** interface `TripInvitationRepository`.  
**Rôle :** Repository Spring Data JPA. Il fournit l’accès base et les requêtes dérivées à partir des noms de méthodes.

**Méthodes repository :**
- `findByCode(String code)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `findByTripIdOrderByCreatedAtDesc(UUID tripId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.

### `backend/src/main/java/fr/achabitation/infrastructure/repository/TripMemberRepository.java`

**Type :** interface `TripMemberRepository`.  
**Rôle :** Repository Spring Data JPA. Il fournit l’accès base et les requêtes dérivées à partir des noms de méthodes.

**Méthodes repository :**
- `findByUserId(UUID userId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `findByTripId(UUID tripId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `findByTripIdAndUserId(UUID tripId, UUID userId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `existsByTripIdAndUserId(UUID tripId, UUID userId)` : Teste l’existence d’une donnée en base selon les critères indiqués dans le nom de méthode.

### `backend/src/main/java/fr/achabitation/infrastructure/repository/TripRepository.java`

**Type :** interface `TripRepository`.  
**Rôle :** Repository Spring Data JPA. Il fournit l’accès base et les requêtes dérivées à partir des noms de méthodes.

**Méthodes repository :**
- `findByActiveTrueOrderByCreatedAtDesc()` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.

### `backend/src/main/java/fr/achabitation/infrastructure/repository/UserRepository.java`

**Type :** interface `UserRepository`.  
**Rôle :** Repository Spring Data JPA. Il fournit l’accès base et les requêtes dérivées à partir des noms de méthodes.

**Méthodes repository :**
- `findByEmailIgnoreCase(String email)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `existsByEmailIgnoreCase(String email)` : Teste l’existence d’une donnée en base selon les critères indiqués dans le nom de méthode.
- `findBySessionToken(String sessionToken)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `findAllByDisplayNameIgnoreCase(String displayName)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.

### `backend/src/main/java/fr/achabitation/web/AppPageController.java`

**Type :** class `AppPageController`.  
**Rôle :** Contrôleur web léger. Il redirige vers l’interface statique servie par Spring Boot.

**Méthodes :**
- `app()` : Redirige `/app` vers la page statique de l’interface web.

### `backend/src/test/java/fr/achabitation/api/AchabitationApiIntegrationTest.java`

**Type :** classe `?`.  
**Rôle :** Classe de tests automatisés. Elle valide le comportement du backend ou du domaine.

**Méthodes :**
- `registerUser(String email)` : Test automatisé : vérifie le scénario métier indiqué par le nom de la méthode.
- `createTrip(AuthSession owner)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `createPerson(AuthSession actor, UUID tripId, String json)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `createExpense(AuthSession actor, UUID tripId, String json)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `createInvitationCode(AuthSession actor, UUID tripId)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `simplePersonJson(String name, String livingRest)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `bearer(AuthSession session)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `read(MvcResult result)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `AuthSession(UUID userId, String token)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `backend/src/test/java/fr/achabitation/application/AuthServiceTest.java`

**Type :** classe `?`.  
**Rôle :** Service applicatif. Il orchestre les repositories, règles métier, droits et mapping de données.

Aucune méthode déclarée explicitement dans ce fichier. Son rôle est porté par son type, ses annotations ou ses constantes.

### `backend/src/test/java/fr/achabitation/application/EntityMapperTest.java`

**Type :** classe `?`.  
**Rôle :** Service applicatif. Il orchestre les repositories, règles métier, droits et mapping de données.

Aucune méthode déclarée explicitement dans ce fichier. Son rôle est porté par son type, ses annotations ou ses constantes.

### `backend/src/test/java/fr/achabitation/domain/BalanceCalculatorTest.java`

**Type :** classe `?`.  
**Rôle :** Moteur de calcul métier pur : répartition au RAV, mode moyenne, dépenses globales/avancées, soldes et remboursements.

**Méthodes :**
- `person(UUID id, String name, String livingRest, WeightMode weightMode, boolean vegetarian, boolean noAlcohol, boolean active, List<PresencePeriod> periods)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `expense(String title, LocalDate date, UUID payerId, String total, String meat, String alcohol, ExpenseType type, boolean advancedMode, Set<UUID> manualParticipantIds, String currency, String exchangeRate)` : Test automatisé : vérifie le scénario métier indiqué par le nom de la méthode.
- `period(LocalDate start, LocalDate end)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `fullStay()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `sum(Map<UUID, BigDecimal> shares)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `findBalance(List<Balance> balances, UUID personId)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.

### `backend/src/test/java/fr/achabitation/domain/model/PresencePeriodTest.java`

**Type :** classe `?`.  
**Rôle :** Modèle métier pur, indépendant de Spring et de la base. Utilisé par le moteur de calcul.

Aucune méthode déclarée explicitement dans ce fichier. Son rôle est porté par son type, ses annotations ou ses constantes.

### `desktop-legacy/src/com/vacances/ravtricount/AppState.java`

**Type :** class `AppState`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `getPersons()` : Retourne la valeur de l’attribut `persons`.
- `getExpenses()` : Retourne la valeur de l’attribut `expenses`.

### `desktop-legacy/src/com/vacances/ravtricount/Balance.java`

**Type :** class `Balance`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(Person person, BigDecimal totalPaid, BigDecimal totalOwed)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `getPerson()` : Retourne la valeur de l’attribut `person`.
- `getTotalPaid()` : Retourne la valeur de l’attribut `totalPaid`.
- `getTotalOwed()` : Retourne la valeur de l’attribut `totalOwed`.
- `getBalance()` : Retourne la valeur de l’attribut `balance`.

### `desktop-legacy/src/com/vacances/ravtricount/BalanceService.java`

**Type :** class `BalanceService`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `calculateSharesForExpense(Expense expense, List<Person> persons)` : Calcule les parts dues par personne pour une dépense donnée.
- `calculateBalances(List<Expense> expenses, List<Person> persons)` : Agrège toutes les dépenses pour calculer payé, dû et solde de chaque personne.
- `calculateSettlements(List<Balance> balances)` : Transforme les soldes en remboursements minimaux entre débiteurs et créditeurs.
- `findPersonById(List<Person> persons, String id)` : Recherche une entité ou une liste d’entités selon les critères indiqués dans le nom de méthode.
- `validateExpenseHasParticipants(Expense expense, List<Person> persons)` : Vérifie qu’une dépense a bien au moins une personne concernée pour chaque bloc utile.
- `describeExpenseParticipants(Expense expense, List<Person> persons)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `addAllocation(Map<String, BigDecimal> shares, BigDecimal amount, List<Person> participants)` : Ajoute un élément ou une allocation dans une collection métier.
- `allocateProportionally(BigDecimal amount, List<Person> participants)` : Répartit un montant entre les personnes éligibles selon leur poids effectif.
- `calculateEffectiveWeights(List<Person> eligible)` : Calcule le poids de chaque participant en tenant compte du mode RAV ou du mode moyenne.
- `canParticipateInAllocation(Person person)` : Indique si une personne peut entrer dans une allocation, notamment avec un RAV positif ou un mode moyenne.
- `hasPositiveLivingRest(Person person)` : Indique si le RAV de la personne est strictement positif.
- `safe(BigDecimal value)` : Sécurise une valeur potentiellement nulle ou invalide avec une valeur par défaut contrôlée.
- `scale(BigDecimal value)` : Applique l’arrondi monétaire au centime.
- `AllocationLine(String personId, long cents, BigDecimal remainder)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `personId()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `remainder()` : Calcule le reste d’arrondi à redistribuer pour conserver le total exact.
- `MutableAmount(Person person, BigDecimal amount)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `amount()` : Retourne ou convertit un montant monétaire utilisé par le calcul.

### `desktop-legacy/src/com/vacances/ravtricount/DataStore.java`

**Type :** class `DataStore`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(Path dataFile)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `load()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `save(AppState state)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `getDataFile()` : Retourne la valeur de l’attribut `dataFile`.

### `desktop-legacy/src/com/vacances/ravtricount/Expense.java`

**Type :** class `Expense`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(String title, LocalDate date, String payerId, BigDecimal totalAmount, BigDecimal meatAmount, BigDecimal alcoholAmount, ExpenseType type, boolean advancedMode, Set<String> manualParticipantIds)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `getId()` : Retourne la valeur de l’attribut `id`.
- `getTitle()` : Retourne la valeur de l’attribut `title`.
- `setTitle(String title)` : Modifie la valeur de l’attribut `title`.
- `getDate()` : Retourne la valeur de l’attribut `date`.
- `setDate(LocalDate date)` : Modifie la valeur de l’attribut `date`.
- `getPayerId()` : Retourne la valeur de l’attribut `payerId`.
- `setPayerId(String payerId)` : Modifie la valeur de l’attribut `payerId`.
- `getTotalAmount()` : Retourne la valeur de l’attribut `totalAmount`.
- `setTotalAmount(BigDecimal totalAmount)` : Modifie la valeur de l’attribut `totalAmount`.
- `getMeatAmount()` : Retourne la valeur de l’attribut `meatAmount`.
- `setMeatAmount(BigDecimal meatAmount)` : Modifie la valeur de l’attribut `meatAmount`.
- `getAlcoholAmount()` : Retourne la valeur de l’attribut `alcoholAmount`.
- `setAlcoholAmount(BigDecimal alcoholAmount)` : Modifie la valeur de l’attribut `alcoholAmount`.
- `getType()` : Retourne la valeur de l’attribut `type`.
- `setType(ExpenseType type)` : Modifie la valeur de l’attribut `type`.
- `isAdvancedMode()` : Retourne l’état booléen `advancedMode`.
- `setAdvancedMode(boolean advancedMode)` : Modifie la valeur de l’attribut `advancedMode`.
- `getManualParticipantIds()` : Retourne la valeur de l’attribut `manualParticipantIds`.
- `setManualParticipantIds(Set<String> manualParticipantIds)` : Modifie la valeur de l’attribut `manualParticipantIds`.

### `desktop-legacy/src/com/vacances/ravtricount/ExpensePanel.java`

**Type :** class `ExpensePanel`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(AppState state, BalanceService balanceService, Runnable onDataChanged)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `buildLayout()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `addRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field)` : Ajoute un élément ou une allocation dans une collection métier.
- `addComboRow(JPanel panel, GridBagConstraints c, int row, String label, JComboBox<?> combo)` : Ajoute un élément ou une allocation dans une collection métier.
- `configureSelection()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `addExpense()` : Ajoute un élément ou une allocation dans une collection métier.
- `updateSelectedExpense()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `deleteSelectedExpense()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `readExpenseFromForm(Expense existing)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `selectedParticipantIds()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `fillForm(Expense e)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `selectPayer(String payerId)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `selectParticipants(Set<String> ids)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `clearForm()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `updateAdvancedState()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `afterMutation()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `refresh()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `refreshPersonInputs()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `desktop-legacy/src/com/vacances/ravtricount/ExpenseTableModel.java`

**Type :** class `ExpenseTableModel`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(List<Expense> expenses, List<Person> persons, BalanceService balanceService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `getRowCount()` : Retourne la valeur de l’attribut `rowCount`.
- `getColumnCount()` : Retourne la valeur de l’attribut `columnCount`.
- `getColumnName(int column)` : Retourne la valeur de l’attribut `columnName`.
- `getValueAt(int rowIndex, int columnIndex)` : Retourne la valeur de l’attribut `valueAt`.
- `getExpenseAt(int row)` : Retourne la valeur de l’attribut `expenseAt`.
- `refresh()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `desktop-legacy/src/com/vacances/ravtricount/ExpenseType.java`

**Type :** enum `ExpenseType`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Constantes enum :**
- `NORMAL`
- `GLOBAL`

**Méthodes :**
- `getLabel()` : Retourne la valeur de l’attribut `label`.
- `toString()` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.

### `desktop-legacy/src/com/vacances/ravtricount/Main.java`

**Type :** class `Main`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `main(String[] args)` : Lance l’application Spring Boot.

### `desktop-legacy/src/com/vacances/ravtricount/MainFrame.java`

**Type :** class `MainFrame`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(AppState state, DataStore dataStore)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `buildLayout()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `buildMenuBar()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `onDataChanged()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `refreshAll()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `desktop-legacy/src/com/vacances/ravtricount/Person.java`

**Type :** class `Person`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(String name, BigDecimal livingRest, boolean vegetarian, boolean noAlcohol, LocalDate presenceStart, LocalDate presenceEnd)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `getId()` : Retourne la valeur de l’attribut `id`.
- `getName()` : Retourne la valeur de l’attribut `name`.
- `setName(String name)` : Modifie la valeur de l’attribut `name`.
- `getLivingRest()` : Retourne la valeur de l’attribut `livingRest`.
- `setLivingRest(BigDecimal livingRest)` : Modifie la valeur de l’attribut `livingRest`.
- `isAverageWeight()` : Retourne l’état booléen `averageWeight`.
- `setAverageWeight(boolean averageWeight)` : Modifie la valeur de l’attribut `averageWeight`.
- `isAdvancedLivingRest()` : Retourne l’état booléen `advancedLivingRest`.
- `setAdvancedLivingRest(boolean advancedLivingRest)` : Modifie la valeur de l’attribut `advancedLivingRest`.
- `getNetIncomeAfterTax()` : Retourne la valeur de l’attribut `netIncomeAfterTax`.
- `setNetIncomeAfterTax(BigDecimal netIncomeAfterTax)` : Modifie la valeur de l’attribut `netIncomeAfterTax`.
- `getRent()` : Retourne la valeur de l’attribut `rent`.
- `setRent(BigDecimal rent)` : Modifie la valeur de l’attribut `rent`.
- `getCredits()` : Retourne la valeur de l’attribut `credits`.
- `setCredits(BigDecimal credits)` : Modifie la valeur de l’attribut `credits`.
- `getFixedCharges()` : Retourne la valeur de l’attribut `fixedCharges`.
- `setFixedCharges(BigDecimal fixedCharges)` : Modifie la valeur de l’attribut `fixedCharges`.
- `getTransport()` : Retourne la valeur de l’attribut `transport`.
- `setTransport(BigDecimal transport)` : Modifie la valeur de l’attribut `transport`.
- `getInsurance()` : Retourne la valeur de l’attribut `insurance`.
- `setInsurance(BigDecimal insurance)` : Modifie la valeur de l’attribut `insurance`.
- `getOtherMandatoryExpenses()` : Retourne la valeur de l’attribut `otherMandatoryExpenses`.
- `setOtherMandatoryExpenses(BigDecimal otherMandatoryExpenses)` : Modifie la valeur de l’attribut `otherMandatoryExpenses`.
- `calculateAdvancedLivingRest()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `isVegetarian()` : Retourne l’état booléen `vegetarian`.
- `setVegetarian(boolean vegetarian)` : Modifie la valeur de l’attribut `vegetarian`.
- `isNoAlcohol()` : Retourne l’état booléen `noAlcohol`.
- `setNoAlcohol(boolean noAlcohol)` : Modifie la valeur de l’attribut `noAlcohol`.
- `getPresenceStart()` : Retourne la valeur de l’attribut `presenceStart`.
- `setPresenceStart(LocalDate presenceStart)` : Modifie la valeur de l’attribut `presenceStart`.
- `getPresenceEnd()` : Retourne la valeur de l’attribut `presenceEnd`.
- `setPresenceEnd(LocalDate presenceEnd)` : Modifie la valeur de l’attribut `presenceEnd`.
- `isActive()` : Retourne l’état booléen `active`.
- `setActive(boolean active)` : Modifie la valeur de l’attribut `active`.
- `isPresentOn(LocalDate date)` : Retourne l’état booléen `presentOn`.
- `toString()` : Convertit un objet vers une autre représentation, généralement entité → DTO ou entité → modèle domaine.
- `equals(Object o)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `hashCode()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `zero()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `scale(BigDecimal value)` : Applique l’arrondi monétaire au centime.

### `desktop-legacy/src/com/vacances/ravtricount/PersonPanel.java`

**Type :** class `PersonPanel`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(AppState state, Runnable onDataChanged)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `buildLayout()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `addRow(JPanel panel, GridBagConstraints c, int row, String label, JTextField field)` : Ajoute un élément ou une allocation dans une collection métier.
- `configureSelection()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `addPerson()` : Ajoute un élément ou une allocation dans une collection métier.
- `updateSelectedPerson()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `deactivateSelectedPerson()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `readPersonFromForm(Person existing)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `readAdvancedLivingRestInput(boolean advancedLivingRest)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `calculateAndDisplayLivingRest()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `ensureUniqueName(String name, Person currentPerson)` : Garantit une précondition métier avant de poursuivre le traitement.
- `normalizeDisplayName(String rawName)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `normalizeNameForComparison(String name)` : Nettoie et normalise une donnée pour éviter les doublons ou incohérences de comparaison.
- `fillForm(Person p)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `clearForm()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `updateLivingRestModeState()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `advancedFields()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `afterMutation()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `refresh()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `AdvancedLivingRestInput(BigDecimal netIncomeAfterTax, BigDecimal rent, BigDecimal credits, BigDecimal fixedCharges, BigDecimal transport, BigDecimal insurance, BigDecimal otherMandatoryExpenses)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `calculateLivingRest()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `desktop-legacy/src/com/vacances/ravtricount/PersonTableModel.java`

**Type :** class `PersonTableModel`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(List<Person> persons)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `getRowCount()` : Retourne la valeur de l’attribut `rowCount`.
- `getColumnCount()` : Retourne la valeur de l’attribut `columnCount`.
- `getColumnName(int column)` : Retourne la valeur de l’attribut `columnName`.
- `getValueAt(int rowIndex, int columnIndex)` : Retourne la valeur de l’attribut `valueAt`.
- `getPersonAt(int row)` : Retourne la valeur de l’attribut `personAt`.
- `refresh()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `desktop-legacy/src/com/vacances/ravtricount/Settlement.java`

**Type :** class `Settlement`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(Person from, Person to, BigDecimal amount)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `getFrom()` : Retourne la valeur de l’attribut `from`.
- `getTo()` : Retourne la valeur de l’attribut `to`.
- `getAmount()` : Retourne la valeur de l’attribut `amount`.

### `desktop-legacy/src/com/vacances/ravtricount/SummaryPanel.java`

**Type :** class `SummaryPanel`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `constructeur(AppState state, BalanceService balanceService)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `buildLayout()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `refresh()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `appendExpenseDetail(StringBuilder sb, Expense expense)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `formatSigned(BigDecimal value)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.

### `desktop-legacy/src/com/vacances/ravtricount/UiUtils.java`

**Type :** classe `?`.  
**Rôle :** Ancienne application desktop Swing conservée comme référence fonctionnelle locale.

**Méthodes :**
- `UiUtils()` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `parseMoney(String text, String fieldName)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `parseDate(String text, String fieldName)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `money(BigDecimal value)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `showError(Component parent, Exception e)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.
- `showInfo(Component parent, String message)` : Méthode interne du fichier. Elle participe au traitement métier ou technique décrit par la classe.


## 5. Méthodes et classes à savoir expliquer en priorité

Pour défendre le projet techniquement, il faut surtout maîtriser ces points :

1. `BalanceCalculator` : c’est le cœur métier. Il faut savoir expliquer la répartition au RAV, le mode moyenne, les exclusions végétarien/sans alcool/contraintes, les dépenses globales et le calcul des remboursements.
2. `TripService` : il porte les voyages, invitations, contraintes officielles du voyage et la liaison guest/compte.
3. `PersonService` : il porte les règles sur les personnes, périodes de présence, RAV, confidentialité et contraintes.
4. `AuthService` + `AuthorizationService` : ils portent l’authentification, le profil utilisateur et les droits par rôle.
5. `EntityMapper` : il évite de renvoyer directement les entités JPA à l’API et applique le masquage du RAV privé.
6. `ApiExceptionHandler` : il transforme les erreurs en réponses lisibles au lieu de laisser des `500` opaques.

## 6. Questions techniques probables et réponses courtes

**Pourquoi avoir séparé `domain` et `application` ?**  
Pour que le moteur de calcul reste indépendant du web, de Spring et de la base. Il devient testable et réutilisable.

**Pourquoi ne pas renvoyer directement les entités JPA en JSON ?**  
Parce que ça expose trop de données, crée des risques de lazy loading et mélange persistance et contrat API. Les DTO contrôlent ce qui sort.

**Pourquoi une personne peut être guest ou liée à un compte ?**  
Parce que dans un voyage, on peut ajouter quelqu’un avant qu’il crée un compte. Ensuite il peut rejoindre le voyage et lier son compte au guest existant.

**Pourquoi le profil utilisateur ne s’applique pas automatiquement aux voyages ?**  
Parce que le RAV ou les contraintes peuvent varier selon le contexte. L’utilisateur doit choisir explicitement les voyages à mettre à jour.

**Pourquoi les contraintes personnalisées sont liées au voyage ?**  
Pour éviter les doublons sémantiques comme `gluten free` et `sans gluten`. Le voyage définit une liste officielle, ensuite chaque personne coche seulement dans cette liste.

**Pourquoi H2 en local et PostgreSQL en prod ?**  
H2 simplifie les tests et le développement local. PostgreSQL est plus adapté à une vraie exploitation multi-utilisateur.

## 7. Limites connues à connaître

- L’interface web est volontairement simple et sans framework frontend lourd.
- L’authentification est suffisante pour une bêta fermée mais doit encore être durcie pour une production publique complète.
- Le mode RAV privé masque la valeur dans l’API, mais la valeur peut rester déductible indirectement par les soldes.
- Les exports sont en CSV ; PDF/Excel natifs restent des évolutions possibles.
- L’ancien desktop Swing est conservé mais ne représente plus l’architecture cible.

## 8. Commandes utiles

```powershell
cd achabitation-refonteackend
mvn clean test
mvn spring-boot:run
```

Smoke test :

```powershell
cd achabitation-refonte
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

Interface locale :

```text
http://localhost:8080/app
```

Console H2 :

```text
http://localhost:8080/h2-console
JDBC URL : jdbc:h2:file:./data/achabitation;AUTO_SERVER=TRUE;MODE=PostgreSQL
User     : sa
Password : vide
```

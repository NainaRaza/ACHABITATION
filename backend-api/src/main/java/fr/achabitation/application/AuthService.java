package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.AccountExportLinkedPerson;
import fr.achabitation.api.dto.AuthDtos.AccountExportResponse;
import fr.achabitation.api.dto.AuthDtos.AccountExportTrip;
import fr.achabitation.api.dto.AuthDtos.AccountExportInvitation;
import fr.achabitation.api.dto.AuthDtos.AccountExportAuditLog;
import fr.achabitation.api.dto.AuthDtos.AccountExportExpense;
import fr.achabitation.api.dto.AuthDtos.ApplyProfileToLinkedPersonsRequest;
import fr.achabitation.api.dto.AuthDtos.AccountUpdateRequest;
import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import fr.achabitation.api.dto.AuthDtos.LinkedProfilePersonResponse;
import fr.achabitation.api.dto.AuthDtos.LoginRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordChangeRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetRequest;
import fr.achabitation.api.dto.AuthDtos.OperationResponse;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationRequest;
import fr.achabitation.api.dto.AuthDtos.UserSessionResponse;
import fr.achabitation.api.dto.AuthDtos.RegisterRequest;
import fr.achabitation.api.dto.AuthDtos.UserProfileRequest;
import fr.achabitation.api.dto.AuthDtos.UserProfileResponse;
import fr.achabitation.domain.model.WeightMode;
import fr.achabitation.domain.util.ConstraintNameUtils;
import fr.achabitation.infrastructure.entity.AuditLogEntity;
import fr.achabitation.infrastructure.entity.ExpenseEntity;
import fr.achabitation.infrastructure.entity.PasswordResetTokenEntity;
import fr.achabitation.infrastructure.entity.EmailVerificationTokenEntity;
import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.TripInvitationEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.AuditLogRepository;
import fr.achabitation.infrastructure.repository.ExpenseRepository;
import fr.achabitation.infrastructure.repository.PasswordResetTokenRepository;
import fr.achabitation.infrastructure.repository.EmailVerificationTokenRepository;
import fr.achabitation.infrastructure.repository.PersonRepository;
import fr.achabitation.infrastructure.repository.TripInvitationRepository;
import fr.achabitation.infrastructure.repository.UserRepository;
import fr.achabitation.infrastructure.repository.TripMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityMapper mapper;
    private final SessionTokenService sessionTokenService;
    private final TripMemberRepository tripMemberRepository;
    private final AccountSessionService accountSessionService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final AccountEmailService accountEmailService;
    private final SecurityEventService securityEventService;
    private final boolean requireEmailVerification;
    private final TripInvitationRepository tripInvitationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ExpenseRepository expenseRepository;

    public AuthService(
            UserRepository userRepository,
            PersonRepository personRepository,
            PasswordEncoder passwordEncoder,
            EntityMapper mapper,
            SessionTokenService sessionTokenService,
            TripMemberRepository tripMemberRepository,
            AccountSessionService accountSessionService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            AccountEmailService accountEmailService,
            SecurityEventService securityEventService,
            @Value("${achabitation.auth.require-email-verification:false}") boolean requireEmailVerification,
            TripInvitationRepository tripInvitationRepository,
            AuditLogRepository auditLogRepository,
            ExpenseRepository expenseRepository
    ) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.sessionTokenService = sessionTokenService;
        this.tripMemberRepository = tripMemberRepository;
        this.accountSessionService = accountSessionService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.accountEmailService = accountEmailService;
        this.securityEventService = securityEventService;
        this.requireEmailVerification = requireEmailVerification;
        this.tripInvitationRepository = tripInvitationRepository;
        this.auditLogRepository = auditLogRepository;
        this.expenseRepository = expenseRepository;
    }

    private String normalizedEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizedDisplayName(String displayName) {
        return displayName == null ? "" : displayName.trim().replaceAll("\\s+", " ");
    }

    private void ensureValidDisplayName(String displayName) {
        String normalized = normalizedDisplayName(displayName);
        if (normalized.length() < 2 || normalized.length() > 120) {
            throw new IllegalArgumentException("Le nom affiché doit contenir entre 2 et 120 caractères.");
        }
    }

    private UserEntity findUserByEmailOrDisplayName(String identifier) {
        String raw = identifier == null ? "" : identifier.trim();
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }
        if (raw.contains("@")) {
            return userRepository.findByEmailIgnoreCase(normalizedEmail(raw))
                    .orElseThrow(() -> new IllegalArgumentException("Identifiants invalides."));
        }
        List<UserEntity> matches = userRepository.findAllByDisplayNameIgnoreCase(normalizedDisplayName(raw));
        if (matches == null || matches.size() != 1) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }
        return matches.get(0);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizedEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }
        String displayName = normalizedDisplayName(request.displayName());
        ensureValidDisplayName(displayName);
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        requestEmailVerificationFor(user);
        securityEventService.log(user, "account.registered");
        if (requireEmailVerification) {
            return toAuthResponse(user, null, "Compte créé. Vérifie ton email avant connexion.");
        }
        String rawToken = createSession(user, "Inscription");
        return toAuthResponse(user, rawToken, "Compte créé. Session ouverte. Vérification email envoyée.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = findUserByEmailOrDisplayName(request.email());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }
        if (requireEmailVerification && !user.isEmailVerified()) {
            throw new IllegalArgumentException("Email non vérifié. Vérifie ton email avant connexion.");
        }
        String rawToken = createSession(user, "Connexion");
        securityEventService.log(user, "account.login");
        return toAuthResponse(user, rawToken, "Connexion validée. Session ouverte.");
    }

    @Transactional
    public AuthResponse updateAccount(UserEntity user, AccountUpdateRequest request) {
        UUID currentUserId = user.getId();
        String email = normalizedEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new IllegalArgumentException("Un autre compte utilise déjà cet email.");
            }
        });
        String displayName = normalizedDisplayName(request.displayName());
        ensureValidDisplayName(displayName);
        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
        user.setEmail(email);
        user.setDisplayName(displayName);
        if (emailChanged) {
            user.setEmailVerifiedAt(null);
            user.setEmailVerificationRequestedAt(null);
        }
        UserEntity savedUser = userRepository.save(user);
        if (emailChanged) {
            requestEmailVerificationFor(savedUser);
        }
        securityEventService.log(savedUser, emailChanged ? "account.updated.email_changed" : "account.updated");
        return toAuthResponse(savedUser, null, emailChanged ? "Compte mis à jour. Vérification du nouvel email envoyée." : "Compte mis à jour.");
    }

    @Transactional
    public void logoutCurrent(UserEntity user, UUID currentSessionId) {
        accountSessionService.logoutCurrent(user, currentSessionId);
    }

    @Transactional
    public void logoutAll(UserEntity user) {
        accountSessionService.logoutAll(user);
    }

    @Transactional
    public AuthResponse changePassword(UserEntity user, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe actuel invalide.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        UserEntity savedUser = userRepository.save(user);
        logoutAll(savedUser);
        String rawToken = createSession(savedUser, "Mot de passe modifié");
        securityEventService.log(savedUser, "password.changed");
        return toAuthResponse(savedUser, rawToken, "Mot de passe modifié. Les anciennes sessions sont invalidées.");
    }

    @Transactional
    public OperationResponse requestEmailVerification(EmailVerificationRequest request) {
        String email = normalizedEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(this::requestEmailVerificationFor);
        if (email != null && !email.isBlank()) {
            securityEventService.logEmailOnly(email, "email.verification_requested_masked_response");
        }
        return new OperationResponse("Si le compte existe, un email de vérification a été envoyé.");
    }

    @Transactional
    public AuthResponse confirmEmailVerification(EmailVerificationConfirmRequest request) {
        EmailVerificationTokenEntity token = emailVerificationTokenRepository.findByTokenHash(sessionTokenService.hashToken(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Jeton de vérification invalide ou expiré."));
        if (!token.isUsable(Instant.now())) {
            throw new IllegalArgumentException("Jeton de vérification invalide ou expiré.");
        }
        UserEntity user = token.getUser();
        if (!normalizedEmail(user.getEmail()).equals(normalizedEmail(token.getEmail()))) {
            throw new IllegalArgumentException("Jeton de vérification invalide pour cet email.");
        }
        user.setEmailVerifiedAt(Instant.now());
        token.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(token);
        userRepository.save(user);
        securityEventService.log(user, "email.verified");
        String rawToken = createSession(user, "Email vérifié");
        return toAuthResponse(user, rawToken, "Email vérifié. Session ouverte.");
    }

    private void requestEmailVerificationFor(UserEntity user) {
        String rawToken = sessionTokenService.newRawToken();
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.setUser(user);
        token.setEmail(user.getEmail());
        token.setTokenHash(sessionTokenService.hashToken(rawToken));
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        emailVerificationTokenRepository.save(token);
        user.setEmailVerificationRequestedAt(Instant.now());
        userRepository.save(user);
        accountEmailService.sendEmailVerification(user.getEmail(), rawToken);
        securityEventService.log(user, "email.verification_requested");
    }

    @Transactional
    public OperationResponse requestPasswordReset(PasswordResetRequest request) {
        String email = normalizedEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = sessionTokenService.newRawToken();
            PasswordResetTokenEntity token = new PasswordResetTokenEntity();
            token.setUser(user);
            token.setTokenHash(sessionTokenService.hashToken(rawToken));
            token.setCreatedAt(Instant.now());
            token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
            passwordResetTokenRepository.save(token);
            accountEmailService.sendPasswordReset(user.getEmail(), rawToken);
            securityEventService.log(user, "password.reset_requested");
        });
        if (email != null && !email.isBlank()) {
            securityEventService.logEmailOnly(email, "password.reset_requested_masked_response");
        }
        return new OperationResponse("Si le compte existe, un email de réinitialisation a été envoyé.");
    }

    @Transactional
    public AuthResponse resetPassword(PasswordResetConfirmRequest request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(sessionTokenService.hashToken(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Jeton de réinitialisation invalide ou expiré."));
        if (!token.isUsable(Instant.now())) {
            throw new IllegalArgumentException("Jeton de réinitialisation invalide ou expiré.");
        }
        UserEntity user = token.getUser();
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        userRepository.save(user);
        logoutAll(user);
        String rawToken = createSession(user, "Réinitialisation mot de passe");
        securityEventService.log(user, "password.reset_confirmed");
        return toAuthResponse(user, rawToken, "Mot de passe réinitialisé. Les anciennes sessions sont invalidées.");
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> listSessions(UserEntity user, UUID currentSessionId) {
        return accountSessionService.listSessions(user, currentSessionId);
    }

    @Transactional
    public void revokeSession(UserEntity user, UUID sessionId) {
        accountSessionService.revokeSession(user, sessionId);
    }

    @Transactional(readOnly = true)
    public AccountExportResponse exportAccount(UserEntity user) {
        UserProfileResponse profile = toProfileResponse(user);
        List<AccountExportTrip> trips = tripMemberRepository.findByUserId(user.getId()).stream()
                .map(member -> new AccountExportTrip(
                        member.getTrip().getId(),
                        member.getTrip().getName(),
                        member.getRole() == null ? null : member.getRole().name(),
                        member.getJoinedAt()
                ))
                .toList();
        List<AccountExportLinkedPerson> linkedPersons = personRepository.findByLinkedUserId(user.getId()).stream()
                .map(person -> new AccountExportLinkedPerson(
                        person.getId(),
                        person.getName(),
                        person.getTrip().getId(),
                        person.getTrip().getName(),
                        person.isLivingRestPublic()
                ))
                .toList();
        List<AccountExportInvitation> invitations = tripInvitationRepository.findByCreatedByIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(invitation -> new AccountExportInvitation(
                        invitation.getId(),
                        invitation.getTrip().getId(),
                        invitation.getTrip().getName(),
                        invitation.getRoleToGrant() == null ? null : invitation.getRoleToGrant().name(),
                        invitation.getCreatedAt(),
                        invitation.getExpiresAt(),
                        invitation.getRevokedAt() != null
                ))
                .toList();
        List<AccountExportAuditLog> auditLogs = auditLogRepository.findByActorIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(log -> new AccountExportAuditLog(
                        log.getId(),
                        log.getTrip().getId(),
                        log.getTrip().getName(),
                        log.getAction() == null ? null : log.getAction().name(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getCreatedAt()
                ))
                .toList();
        List<AccountExportExpense> paidExpenses = expenseRepository.findByPayerLinkedUserId(user.getId()).stream()
                .map(expense -> new AccountExportExpense(
                        expense.getId(),
                        expense.getTrip().getId(),
                        expense.getTrip().getName(),
                        expense.getTitle(),
                        mapper.money(expense.getTotalAmount()),
                        expense.getCurrency()
                ))
                .toList();
        return new AccountExportResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                profile,
                trips,
                linkedPersons,
                invitations,
                auditLogs,
                paidExpenses,
                Instant.now()
        );
    }

    @Transactional
    public void deleteAccount(UserEntity user) {
        UUID userId = user.getId();
        for (PersonEntity person : personRepository.findByLinkedUserId(userId)) {
            person.setLinkedUser(null);
            String anonymizedName = "Utilisateur supprimé " + person.getId().toString().substring(0, 8);
            person.setName(anonymizedName);
            person.setNormalizedName(mapper.normalizeName(anonymizedName));
            person.setLivingRest(BigDecimal.ZERO);
            person.setNetIncomeAfterTax(BigDecimal.ZERO);
            person.setRent(BigDecimal.ZERO);
            person.setCredits(BigDecimal.ZERO);
            person.setFixedCharges(BigDecimal.ZERO);
            person.setTransport(BigDecimal.ZERO);
            person.setInsurance(BigDecimal.ZERO);
            person.setOtherMandatoryExpenses(BigDecimal.ZERO);
            person.setMenstrualProtection(BigDecimal.ZERO);
            person.setLivingRestPublic(false);
            person.getCustomConstraints().clear();
            personRepository.save(person);
        }

        securityEventService.log(user, "account.deletion_requested");
        user.setEmail("deleted-" + userId + "@deleted.achabitation.local");
        user.setDisplayName("Utilisateur supprimé");
        user.setPasswordHash(passwordEncoder.encode(sessionTokenService.newRawToken()));
        user.setSessionTokenHash(null);
        user.setSessionTokenIssuedAt(null);
        user.setLivingRest(BigDecimal.ZERO);
        user.setNetIncomeAfterTax(BigDecimal.ZERO);
        user.setRent(BigDecimal.ZERO);
        user.setCredits(BigDecimal.ZERO);
        user.setFixedCharges(BigDecimal.ZERO);
        user.setTransport(BigDecimal.ZERO);
        user.setInsurance(BigDecimal.ZERO);
        user.setOtherMandatoryExpenses(BigDecimal.ZERO);
        user.setMenstrualProtection(BigDecimal.ZERO);
        user.setVegetarian(false);
        user.setNoAlcohol(false);
        user.setLivingRestPublic(false);
        user.setEmailVerifiedAt(null);
        user.setEmailVerificationRequestedAt(null);
        user.setDeletedAt(Instant.now());
        user.getKnownCustomConstraints().clear();
        user.getCustomConstraints().clear();
        userRepository.save(user);
        logoutAll(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(UserEntity user) {
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UserEntity user, UserProfileRequest request) {
        if (request.displayName() != null && !request.displayName().isBlank()) {
            String displayName = normalizedDisplayName(request.displayName());
            ensureValidDisplayName(displayName);
            user.setDisplayName(displayName);
        }
        applyFinancialAndConstraintProfile(user, request);
        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse applyProfileToLinkedPersons(UserEntity user, ApplyProfileToLinkedPersonsRequest request) {
        ensureValidUserProfile(user);
        Set<UUID> requestedPersonIds = request == null || request.personIds() == null ? Set.of() : request.personIds();
        if (requestedPersonIds.isEmpty()) {
            throw new IllegalArgumentException("Sélectionne au moins un voyage lié à mettre à jour.");
        }

        List<PersonEntity> linkedPersons = personRepository.findByLinkedUserId(user.getId());
        Set<UUID> allowedPersonIds = linkedPersons.stream()
                .map(PersonEntity::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (UUID personId : requestedPersonIds) {
            if (!allowedPersonIds.contains(personId)) {
                throw new IllegalArgumentException("Une personne sélectionnée n'est pas liée à ton compte.");
            }
        }

        for (PersonEntity person : linkedPersons) {
            if (requestedPersonIds.contains(person.getId())) {
                applyUserProfileToPerson(user, person);
                personRepository.save(person);
            }
        }

        return toProfileResponse(user);
    }

    public void applyFinancialAndConstraintProfile(UserEntity user, UserProfileRequest request) {
        user.setWeightMode(request.weightMode() == null ? WeightMode.LIVING_REST : request.weightMode());
        user.setAdvancedLivingRest(request.advancedLivingRest());
        user.setNetIncomeAfterTax(mapper.money(request.netIncomeAfterTax()));
        user.setRent(mapper.money(request.rent()));
        user.setCredits(mapper.money(request.credits()));
        user.setFixedCharges(mapper.money(request.fixedCharges()));
        user.setTransport(mapper.money(request.transport()));
        user.setInsurance(mapper.money(request.insurance()));
        user.setOtherMandatoryExpenses(mapper.money(request.otherMandatoryExpenses()));
        user.setMenstrualProtection(mapper.money(request.menstrualProtection()));
        user.setLivingRest(computeLivingRest(user.isAdvancedLivingRest(), request.livingRest(), user));
        user.setVegetarian(request.vegetarian());
        user.setNoAlcohol(request.noAlcohol());
        user.setLivingRestPublic(request.livingRestPublic());
        replaceUserCustomConstraints(user, request.customConstraints());
        validateUserProfileForStorage(user);
    }

    public void applyUserProfileToPerson(UserEntity user, PersonEntity person) {
        person.setLinkedUser(user);
        person.setWeightMode(user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode());
        person.setAdvancedLivingRest(user.isAdvancedLivingRest());
        person.setNetIncomeAfterTax(mapper.money(user.getNetIncomeAfterTax()));
        person.setRent(mapper.money(user.getRent()));
        person.setCredits(mapper.money(user.getCredits()));
        person.setFixedCharges(mapper.money(user.getFixedCharges()));
        person.setTransport(mapper.money(user.getTransport()));
        person.setInsurance(mapper.money(user.getInsurance()));
        person.setOtherMandatoryExpenses(mapper.money(user.getOtherMandatoryExpenses()));
        person.setMenstrualProtection(mapper.money(user.getMenstrualProtection()));
        person.setLivingRest(mapper.money(user.getLivingRest()));
        person.setLivingRestPublic(user.isLivingRestPublic());
        person.setVegetarian(user.isVegetarian());
        person.setNoAlcohol(user.isNoAlcohol());
        person.getCustomConstraints().clear();
        person.getCustomConstraints().addAll(profileConstraintsAllowedInTrip(user, person));
    }

    private Set<String> profileConstraintsAllowedInTrip(UserEntity user, PersonEntity person) {
        Set<String> allowedKeys = new LinkedHashSet<>();
        if (person.getTrip() != null && person.getTrip().getCustomConstraints() != null) {
            person.getTrip().getCustomConstraints().forEach(value -> allowedKeys.add(ConstraintNameUtils.key(value)));
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (user.getCustomConstraints() == null) {
            return result;
        }
        for (String value : user.getCustomConstraints()) {
            if (allowedKeys.contains(ConstraintNameUtils.key(value))) {
                result.add(ConstraintNameUtils.canonicalDisplayName(value));
            }
        }
        return result;
    }

    public Set<String> normalizeConstraints(Set<String> constraints) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (constraints == null) {
            return normalized;
        }
        Set<String> seenKeys = new LinkedHashSet<>();
        for (String rawConstraint : constraints) {
            String displayName = ConstraintNameUtils.canonicalDisplayName(rawConstraint);
            String key = ConstraintNameUtils.key(displayName);
            if (key.isBlank()) {
                continue;
            }
            if (displayName.length() > 120) {
                throw new IllegalArgumentException("Le nom d'une contrainte personnalisée ne doit pas dépasser 120 caractères.");
            }
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("Une contrainte personnalisée est renseignée plusieurs fois.");
            }
            normalized.add(displayName);
        }
        return normalized;
    }

    private void replaceUserCustomConstraints(UserEntity user, Set<String> requested) {
        Set<String> normalized = normalizeConstraints(requested);
        user.getKnownCustomConstraints().addAll(normalized);
        user.getCustomConstraints().clear();
        user.getCustomConstraints().addAll(normalized);
    }

    private BigDecimal computeLivingRest(boolean advanced, BigDecimal requestLivingRest, UserEntity user) {
        if (!advanced) {
            return mapper.money(requestLivingRest);
        }
        return mapper.money(user.getNetIncomeAfterTax())
                .subtract(mapper.money(user.getRent()))
                .subtract(mapper.money(user.getCredits()))
                .subtract(mapper.money(user.getFixedCharges()))
                .subtract(mapper.money(user.getTransport()))
                .subtract(mapper.money(user.getInsurance()))
                .subtract(mapper.money(user.getOtherMandatoryExpenses()))
                .subtract(mapper.money(user.getMenstrualProtection()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void ensureValidUserProfile(UserEntity user) {
        WeightMode mode = user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode();
        if (mode != WeightMode.AVERAGE && mapper.money(user.getLivingRest()).signum() <= 0) {
            throw new IllegalArgumentException("Le reste à vivre du profil doit être strictement positif pour être appliqué à un voyage.");
        }
    }

    private void validateUserProfileForStorage(UserEntity user) {
        WeightMode mode = user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode();
        if (mode != WeightMode.AVERAGE && mapper.money(user.getLivingRest()).signum() < 0) {
            throw new IllegalArgumentException("Le reste à vivre du profil ne peut pas être négatif.");
        }
    }

    private String createSession(UserEntity user, String deviceLabel) {
        return accountSessionService.createSession(user, deviceLabel);
    }

    private AuthResponse toAuthResponse(UserEntity user, String rawAccessToken, String note) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEmailVerified(), rawAccessToken, note);
    }

    private List<LinkedProfilePersonResponse> linkedPersons(UserEntity user) {
        return personRepository.findByLinkedUserId(user.getId()).stream()
                .map(person -> new LinkedProfilePersonResponse(
                        person.getId(),
                        person.getName(),
                        person.getTrip().getId(),
                        person.getTrip().getName()
                ))
                .toList();
    }

    private UserProfileResponse toProfileResponse(UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEmailVerified(),
                mapper.money(user.getLivingRest()),
                (user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode()),
                user.isAdvancedLivingRest(),
                mapper.money(user.getNetIncomeAfterTax()),
                mapper.money(user.getRent()),
                mapper.money(user.getCredits()),
                mapper.money(user.getFixedCharges()),
                mapper.money(user.getTransport()),
                mapper.money(user.getInsurance()),
                mapper.money(user.getOtherMandatoryExpenses()),
                mapper.money(user.getMenstrualProtection()),
                user.isVegetarian(),
                user.isNoAlcohol(),
                user.isLivingRestPublic(),
                mapper.stringSet(user.getKnownCustomConstraints()),
                mapper.stringSet(user.getCustomConstraints()),
                linkedPersons(user)
        );
    }
}

package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.AccountExportLinkedPerson;
import fr.achabitation.api.dto.AuthDtos.AccountExportResponse;
import fr.achabitation.api.dto.AuthDtos.AccountExportTrip;
import fr.achabitation.api.dto.AuthDtos.AccountExportInvitation;
import fr.achabitation.api.dto.AuthDtos.AccountExportAuditLog;
import fr.achabitation.api.dto.AuthDtos.AccountExportExpense;
import fr.achabitation.api.dto.AuthDtos.AccountUpdateRequest;
import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import fr.achabitation.api.dto.AuthDtos.LoginRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordChangeRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetRequest;
import fr.achabitation.api.dto.AuthDtos.OperationResponse;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationRequest;
import fr.achabitation.api.dto.AuthDtos.UserSessionResponse;
import fr.achabitation.api.dto.AuthDtos.UserProfileResponse;
import fr.achabitation.api.dto.AuthDtos.RegisterRequest;
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
import java.util.List;
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
    private final AccountIdentityService accountIdentityService;
    private final UserProfileService userProfileService;

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
            ExpenseRepository expenseRepository,
            AccountIdentityService accountIdentityService,
            UserProfileService userProfileService
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
        this.accountIdentityService = accountIdentityService;
        this.userProfileService = userProfileService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = accountIdentityService.normalizedEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }
        String displayName = accountIdentityService.normalizedDisplayName(request.displayName());
        accountIdentityService.ensureValidDisplayName(displayName);
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
        UserEntity user = accountIdentityService.findUserByEmailOrDisplayName(request.email());
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
        String email = accountIdentityService.normalizedEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new IllegalArgumentException("Un autre compte utilise déjà cet email.");
            }
        });
        String displayName = accountIdentityService.normalizedDisplayName(request.displayName());
        accountIdentityService.ensureValidDisplayName(displayName);
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
        String email = accountIdentityService.normalizedEmail(request.email());
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
        if (!accountIdentityService.normalizedEmail(user.getEmail()).equals(accountIdentityService.normalizedEmail(token.getEmail()))) {
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
        String email = accountIdentityService.normalizedEmail(request.email());
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
        UserProfileResponse profile = userProfileService.profile(user);
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

    private String createSession(UserEntity user, String deviceLabel) {
        return accountSessionService.createSession(user, deviceLabel);
    }

    private AuthResponse toAuthResponse(UserEntity user, String rawAccessToken, String note) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEmailVerified(), rawAccessToken, note);
    }


}

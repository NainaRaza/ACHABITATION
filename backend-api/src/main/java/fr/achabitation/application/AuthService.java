package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.AccountExportResponse;
import fr.achabitation.api.dto.AuthDtos.AccountUpdateRequest;
import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationRequest;
import fr.achabitation.api.dto.AuthDtos.LoginRequest;
import fr.achabitation.api.dto.AuthDtos.OperationResponse;
import fr.achabitation.api.dto.AuthDtos.PasswordChangeRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetRequest;
import fr.achabitation.api.dto.AuthDtos.RegisterRequest;
import fr.achabitation.api.dto.AuthDtos.UserSessionResponse;
import fr.achabitation.infrastructure.entity.EmailVerificationTokenEntity;
import fr.achabitation.infrastructure.entity.PasswordResetTokenEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.EmailVerificationTokenRepository;
import fr.achabitation.infrastructure.repository.PasswordResetTokenRepository;
import fr.achabitation.infrastructure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;
    private final AccountSessionService accountSessionService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final AccountEmailService accountEmailService;
    private final SecurityEventService securityEventService;
    private final boolean requireEmailVerification;
    private final AccountIdentityService accountIdentityService;
    private final AccountDataService accountDataService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SessionTokenService sessionTokenService,
            AccountSessionService accountSessionService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            AccountEmailService accountEmailService,
            SecurityEventService securityEventService,
            @Value("${achabitation.auth.require-email-verification:false}") boolean requireEmailVerification,
            AccountIdentityService accountIdentityService,
            AccountDataService accountDataService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
        this.accountSessionService = accountSessionService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.accountEmailService = accountEmailService;
        this.securityEventService = securityEventService;
        this.requireEmailVerification = requireEmailVerification;
        this.accountIdentityService = accountIdentityService;
        this.accountDataService = accountDataService;
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
        return accountDataService.exportAccount(user);
    }

    @Transactional
    public void deleteAccount(UserEntity user) {
        accountDataService.deleteAccount(user);
    }

    private String createSession(UserEntity user, String deviceLabel) {
        return accountSessionService.createSession(user, deviceLabel);
    }

    private AuthResponse toAuthResponse(UserEntity user, String rawAccessToken, String note) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEmailVerified(), rawAccessToken, rawAccessToken != null, note);
    }


}

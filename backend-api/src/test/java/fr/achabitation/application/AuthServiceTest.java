package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import fr.achabitation.api.dto.AuthDtos.LoginRequest;
import fr.achabitation.api.dto.AuthDtos.RegisterRequest;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.UserRepository;
import fr.achabitation.infrastructure.repository.PersonRepository;
import fr.achabitation.infrastructure.repository.UserSessionRepository;
import fr.achabitation.infrastructure.repository.PasswordResetTokenRepository;
import fr.achabitation.infrastructure.repository.EmailVerificationTokenRepository;
import fr.achabitation.infrastructure.repository.TripInvitationRepository;
import fr.achabitation.infrastructure.repository.TripMemberRepository;
import fr.achabitation.infrastructure.repository.AuditLogRepository;
import fr.achabitation.infrastructure.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private EntityMapper mapper;

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private AccountSessionService accountSessionService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private AccountEmailService accountEmailService;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private TripInvitationRepository tripInvitationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserProfileService userProfileService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                sessionTokenService,
                accountSessionService,
                passwordResetTokenRepository,
                emailVerificationTokenRepository,
                accountEmailService,
                securityEventService,
                false,
                new AccountIdentityService(userRepository),
                new AccountDataService(
                        userRepository,
                        personRepository,
                        mapper,
                        tripMemberRepository,
                        tripInvitationRepository,
                        auditLogRepository,
                        expenseRepository,
                        userProfileService,
                        passwordEncoder,
                        sessionTokenService,
                        accountSessionService,
                        securityEventService
                )
        );
    }

    @Test
    void registerNormalizesEmailAndStoresHashedPassword() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCase("joey@example.com")).thenReturn(false);
        when(passwordEncoder.encode("motdepassefort")).thenReturn("hashed-password");
        when(sessionTokenService.newRawToken()).thenReturn("raw-email-token");
        when(sessionTokenService.hashToken("raw-email-token")).thenReturn("hashed-email-token");
        when(accountSessionService.createSession(any(UserEntity.class), any())).thenReturn("raw-access-token");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        AuthResponse response = authService.register(new RegisterRequest("  Joey@Example.COM  ", " Joey ", "motdepassefort"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        UserEntity saved = userCaptor.getAllValues().get(userCaptor.getAllValues().size() - 1);
        assertThat(saved.getEmail()).isEqualTo("joey@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Joey");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("joey@example.com");
        assertThat(response.accessToken()).isEqualTo("raw-access-token");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("joey@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("joey@example.com", "Joey", "motdepassefort")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginAcceptsValidCredentials() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("joey@example.com");
        user.setDisplayName("Joey");
        user.setPasswordHash("hashed-password");
        when(userRepository.findByEmailIgnoreCase("joey@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("motdepassefort", "hashed-password")).thenReturn(true);
        when(accountSessionService.createSession(any(UserEntity.class), any())).thenReturn("login-access-token");

        AuthResponse response = authService.login(new LoginRequest("joey@example.com", "motdepassefort"));

        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo("joey@example.com");
        assertThat(response.displayName()).isEqualTo("Joey");
        assertThat(response.accessToken()).isEqualTo("login-access-token");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "motdepassefort")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identifiants invalides");
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserEntity user = new UserEntity();
        user.setEmail("joey@example.com");
        user.setPasswordHash("hashed-password");
        when(userRepository.findByEmailIgnoreCase("joey@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("joey@example.com", "wrong-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identifiants invalides");
    }
}

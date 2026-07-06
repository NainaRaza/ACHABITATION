package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import fr.achabitation.api.dto.AuthDtos.LoginRequest;
import fr.achabitation.api.dto.AuthDtos.RegisterRequest;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.UserRepository;
import fr.achabitation.infrastructure.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailAndStoresHashedPassword() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCase("joey@example.com")).thenReturn(false);
        when(passwordEncoder.encode("motdepassefort")).thenReturn("hashed-password");
        when(sessionTokenService.newRawToken()).thenReturn("raw-access-token");
        when(sessionTokenService.hashToken("raw-access-token")).thenReturn("hashed-access-token");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        AuthResponse response = authService.register(new RegisterRequest("  Joey@Example.COM  ", " Joey ", "motdepassefort"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("joey@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Joey");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getSessionTokenHash()).isEqualTo("hashed-access-token");
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
        when(sessionTokenService.newRawToken()).thenReturn("login-access-token");
        when(sessionTokenService.hashToken("login-access-token")).thenReturn("login-access-token-hash");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("joey@example.com", "motdepassefort"));

        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.email()).isEqualTo("joey@example.com");
        assertThat(response.displayName()).isEqualTo("Joey");
        ArgumentCaptor<UserEntity> loginUserCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(loginUserCaptor.capture());
        assertThat(loginUserCaptor.getValue().getSessionTokenHash()).isEqualTo("login-access-token-hash");
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

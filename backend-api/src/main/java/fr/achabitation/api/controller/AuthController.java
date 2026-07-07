package fr.achabitation.api.controller;

import fr.achabitation.api.dto.AuthDtos.ApplyProfileToLinkedPersonsRequest;
import fr.achabitation.api.dto.AuthDtos.AccountUpdateRequest;
import fr.achabitation.api.dto.AuthDtos.AccountExportResponse;
import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.EmailVerificationRequest;
import fr.achabitation.api.dto.AuthDtos.LoginRequest;
import fr.achabitation.api.dto.AuthDtos.OperationResponse;
import fr.achabitation.api.dto.AuthDtos.PasswordChangeRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetConfirmRequest;
import fr.achabitation.api.dto.AuthDtos.PasswordResetRequest;
import fr.achabitation.api.dto.AuthDtos.RegisterRequest;
import fr.achabitation.api.dto.AuthDtos.UserProfileRequest;
import fr.achabitation.api.dto.AuthDtos.UserProfileResponse;
import fr.achabitation.api.dto.AuthDtos.UserSessionResponse;
import fr.achabitation.application.AuthContextService;
import fr.achabitation.application.AuthService;
import fr.achabitation.application.LoginRateLimiter;
import fr.achabitation.application.SessionCookieService;
import fr.achabitation.application.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthContextService authContextService;
    private final LoginRateLimiter loginRateLimiter;
    private final SessionCookieService sessionCookieService;
    private final UserProfileService userProfileService;

    public AuthController(AuthService authService, AuthContextService authContextService, LoginRateLimiter loginRateLimiter, SessionCookieService sessionCookieService, UserProfileService userProfileService) {
        this.authService = authService;
        this.authContextService = authContextService;
        this.loginRateLimiter = loginRateLimiter;
        this.sessionCookieService = sessionCookieService;
        this.userProfileService = userProfileService;
    }

    @PostMapping("/register")
    public AuthResponse register(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @Valid @RequestBody RegisterRequest request) {
        loginRateLimiter.check(httpRequest, "register");
        return withSessionCookie(httpResponse, authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @Valid @RequestBody LoginRequest request) {
        loginRateLimiter.check(httpRequest, "login");
        return withSessionCookie(httpResponse, authService.login(request));
    }

    @PostMapping("/email/verification-request")
    public OperationResponse requestEmailVerification(HttpServletRequest httpRequest, @Valid @RequestBody EmailVerificationRequest request) {
        loginRateLimiter.check(httpRequest, "email-verification-request");
        return authService.requestEmailVerification(request);
    }

    @PostMapping("/email/verify")
    public AuthResponse confirmEmailVerification(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @Valid @RequestBody EmailVerificationConfirmRequest request) {
        loginRateLimiter.check(httpRequest, "email-verification-confirm");
        return withSessionCookie(httpResponse, authService.confirmEmailVerification(request));
    }

    @PostMapping("/password/reset-request")
    public OperationResponse requestPasswordReset(HttpServletRequest httpRequest, @Valid @RequestBody PasswordResetRequest request) {
        loginRateLimiter.check(httpRequest, "password-reset-request");
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password/reset")
    public AuthResponse resetPassword(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @Valid @RequestBody PasswordResetConfirmRequest request) {
        loginRateLimiter.check(httpRequest, "password-reset-confirm");
        return withSessionCookie(httpResponse, authService.resetPassword(request));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logoutCurrent(
                authContextService.requiredUser(request),
                authContextService.currentSessionId(request).orElse(null)
        );
        sessionCookieService.clearSessionCookie(response);
    }

    @GetMapping("/sessions")
    public List<UserSessionResponse> sessions(HttpServletRequest request) {
        return authService.listSessions(
                authContextService.requiredUser(request),
                authContextService.currentSessionId(request).orElse(null)
        );
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void revokeSession(HttpServletRequest request, @PathVariable UUID sessionId) {
        authService.revokeSession(authContextService.requiredUser(request), sessionId);
    }

    @DeleteMapping("/sessions")
    public void logoutAll(HttpServletRequest request, HttpServletResponse response) {
        authService.logoutAll(authContextService.requiredUser(request));
        sessionCookieService.clearSessionCookie(response);
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(HttpServletRequest request) {
        return userProfileService.profile(authContextService.requiredUser(request));
    }

    @PutMapping("/account")
    public AuthResponse updateAccount(HttpServletRequest request, @Valid @RequestBody AccountUpdateRequest accountRequest) {
        return authService.updateAccount(authContextService.requiredUser(request), accountRequest);
    }

    @PutMapping("/password")
    public AuthResponse changePassword(HttpServletRequest request, HttpServletResponse response, @Valid @RequestBody PasswordChangeRequest passwordRequest) {
        return withSessionCookie(response, authService.changePassword(authContextService.requiredUser(request), passwordRequest));
    }

    @GetMapping("/export")
    public AccountExportResponse exportAccount(HttpServletRequest request) {
        return authService.exportAccount(authContextService.requiredUser(request));
    }

    @DeleteMapping("/account")
    public void deleteAccount(HttpServletRequest request, HttpServletResponse response) {
        authService.deleteAccount(authContextService.requiredUser(request));
        sessionCookieService.clearSessionCookie(response);
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(HttpServletRequest request, @Valid @RequestBody UserProfileRequest profileRequest) {
        return userProfileService.updateProfile(authContextService.requiredUser(request), profileRequest);
    }

    @PostMapping("/profile/apply-to-linked-persons")
    public UserProfileResponse applyProfileToLinkedPersons(HttpServletRequest request, @Valid @RequestBody ApplyProfileToLinkedPersonsRequest applyRequest) {
        return userProfileService.applyProfileToLinkedPersons(authContextService.requiredUser(request), applyRequest);
    }

    @GetMapping("/csrf")
    public OperationResponse csrf(CsrfToken token) {
        token.getToken();
        return new OperationResponse("CSRF prêt.");
    }

    private AuthResponse withSessionCookie(HttpServletResponse response, AuthResponse authResponse) {
        sessionCookieService.writeSessionCookie(response, authResponse);
        return authResponse;
    }
}

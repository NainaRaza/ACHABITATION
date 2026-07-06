package fr.achabitation.api.controller;

import fr.achabitation.api.dto.AuthDtos.ApplyProfileToLinkedPersonsRequest;
import fr.achabitation.api.dto.AuthDtos.AccountUpdateRequest;
import fr.achabitation.api.dto.AuthDtos.AuthResponse;
import fr.achabitation.api.dto.AuthDtos.LoginRequest;
import fr.achabitation.api.dto.AuthDtos.RegisterRequest;
import fr.achabitation.api.dto.AuthDtos.UserProfileRequest;
import fr.achabitation.api.dto.AuthDtos.UserProfileResponse;
import fr.achabitation.application.AuthContextService;
import fr.achabitation.application.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthContextService authContextService;

    public AuthController(AuthService authService, AuthContextService authContextService) {
        this.authService = authService;
        this.authContextService = authContextService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/profile")
    public UserProfileResponse profile(HttpServletRequest request) {
        return authService.profile(authContextService.requiredUser(request));
    }

    @PutMapping("/account")
    public AuthResponse updateAccount(HttpServletRequest request, @Valid @RequestBody AccountUpdateRequest accountRequest) {
        return authService.updateAccount(authContextService.requiredUser(request), accountRequest);
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(HttpServletRequest request, @RequestBody UserProfileRequest profileRequest) {
        return authService.updateProfile(authContextService.requiredUser(request), profileRequest);
    }

    @PostMapping("/profile/apply-to-linked-persons")
    public UserProfileResponse applyProfileToLinkedPersons(HttpServletRequest request, @RequestBody ApplyProfileToLinkedPersonsRequest applyRequest) {
        return authService.applyProfileToLinkedPersons(authContextService.requiredUser(request), applyRequest);
    }
}

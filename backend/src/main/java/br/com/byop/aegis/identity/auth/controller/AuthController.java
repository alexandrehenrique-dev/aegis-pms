package br.com.byop.aegis.identity.auth.controller;

import br.com.byop.aegis.identity.auth.dto.AuthForgotPasswordRequest;
import br.com.byop.aegis.identity.auth.dto.AuthInviteValidationResponse;
import br.com.byop.aegis.identity.auth.dto.AuthLoginRequest;
import br.com.byop.aegis.identity.auth.dto.AuthLogoutRequest;
import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.dto.AuthPasswordActionRequest;
import br.com.byop.aegis.identity.auth.dto.AuthPasswordResetRequest;
import br.com.byop.aegis.identity.auth.dto.AuthRefreshRequest;
import br.com.byop.aegis.identity.auth.dto.AuthActionTokenRequest;
import br.com.byop.aegis.identity.auth.dto.AuthTokenResponse;
import br.com.byop.aegis.identity.auth.service.AuthActivationService;
import br.com.byop.aegis.identity.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthActivationService authActivationService;

    public AuthController(
            AuthService authService,
            AuthActivationService authActivationService
    ) {
        this.authService = authService;
        this.authActivationService = authActivationService;
    }

    @PostMapping("/login")
    public AuthTokenResponse login(
            @Valid @RequestBody AuthLoginRequest request
    ) {
        return authService.login(
                request.username(),
                request.password()
        );
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(
            @Valid @RequestBody AuthRefreshRequest request
    ) {
        return authService.refresh(
                request.refreshToken()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody AuthLogoutRequest request
    ) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/forgot-password")
    public AuthMessageResponse forgotPassword(
            @Valid @RequestBody AuthForgotPasswordRequest request
    ) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/invite/validate")
    public AuthInviteValidationResponse validateInvite(
            @Valid @RequestBody AuthActionTokenRequest request
    ) {
        return authActivationService.validateInvite(request.token());
    }

    @PostMapping("/activate")
    public AuthMessageResponse activate(
            @Valid @RequestBody AuthPasswordActionRequest request
    ) {
        return authActivationService.activate(request.token(), request.password());
    }

    @PostMapping("/reset-password/request")
    public AuthMessageResponse requestPasswordReset(
            @Valid @RequestBody AuthPasswordResetRequest request
    ) {
        return authActivationService.requestPasswordReset(request.email());
    }

    @PostMapping("/reset-password/confirm")
    public AuthMessageResponse confirmPasswordReset(
            @Valid @RequestBody AuthPasswordActionRequest request
    ) {
        return authActivationService.confirmPasswordReset(request.token(), request.password());
    }
}

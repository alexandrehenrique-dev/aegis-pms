package br.com.byop.aegis.identity.auth.controller;

import br.com.byop.aegis.identity.auth.exception.AuthActionTokenExpiredException;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenNotFoundException;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenUsedException;
import br.com.byop.aegis.identity.auth.exception.AuthRateLimitExceededException;
import br.com.byop.aegis.identity.auth.exception.AccountDisabledException;
import br.com.byop.aegis.identity.auth.exception.InvalidCredentialsException;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.auth.exception.RefreshTokenExpiredException;
import br.com.byop.aegis.identity.auth.exception.WeakPasswordException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public AuthErrorResponse handleInvalidCredentials() {
        log.warn("handleInvalidCredentials: tentativa de login com credenciais invalidas");
        return new AuthErrorResponse("INVALID_CREDENTIALS");
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public AuthErrorResponse handleRefreshTokenExpired() {
        return new AuthErrorResponse("REFRESH_TOKEN_EXPIRED");
    }

    @ExceptionHandler(KeycloakAuthenticationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public AuthErrorResponse handleKeycloakAuthentication(
            KeycloakAuthenticationException exception
    ) {
        log.error("Keycloak authentication error", exception);
        return new AuthErrorResponse("KEYCLOAK_AUTHENTICATION_ERROR");
    }

    @ExceptionHandler(AccountDisabledException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public AuthErrorResponse handleAccountDisabled() {
        return new AuthErrorResponse("ACCOUNT_DISABLED");
    }

    @ExceptionHandler(AuthActionTokenUsedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AuthActionErrorResponse handleTokenUsed() {
        return new AuthActionErrorResponse("TOKEN_ALREADY_USED");
    }

    @ExceptionHandler(AuthActionTokenExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public AuthActionErrorResponse handleTokenExpired() {
        return new AuthActionErrorResponse("TOKEN_EXPIRED");
    }

    @ExceptionHandler(AuthActionTokenNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public AuthActionErrorResponse handleTokenNotFound() {
        return new AuthActionErrorResponse("TOKEN_NOT_FOUND");
    }

    @ExceptionHandler(WeakPasswordException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public AuthActionErrorResponse handleWeakPassword(WeakPasswordException exception) {
        return new AuthActionErrorResponse("WEAK_PASSWORD", exception.getMessage());
    }

    @ExceptionHandler(AuthRateLimitExceededException.class)
    public ResponseEntity<AuthActionErrorResponse> handleRateLimit(AuthRateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
                .body(new AuthActionErrorResponse("TOO_MANY_REQUESTS"));
    }
}

package br.com.byop.aegis.identity.auth.controller;

import br.com.byop.aegis.identity.auth.exception.AccountDisabledException;
import br.com.byop.aegis.identity.auth.exception.InvalidCredentialsException;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.auth.exception.RefreshTokenExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public AuthErrorResponse handleInvalidCredentials() {
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
        LOGGER.error("Keycloak authentication error", exception);
        return new AuthErrorResponse("KEYCLOAK_AUTHENTICATION_ERROR");
    }

    @ExceptionHandler(AccountDisabledException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public AuthErrorResponse handleAccountDisabled() {
        return new AuthErrorResponse("ACCOUNT_DISABLED");
    }
}

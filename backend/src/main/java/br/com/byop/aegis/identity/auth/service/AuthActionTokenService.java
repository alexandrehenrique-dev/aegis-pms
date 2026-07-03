package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.api.IdentityActionInviteCommand;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.auth.domain.AuthActionStatus;
import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.domain.AuthActionType;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenExpiredException;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenNotFoundException;
import br.com.byop.aegis.identity.auth.exception.AuthActionTokenUsedException;
import br.com.byop.aegis.identity.auth.exception.AuthRateLimitExceededException;
import br.com.byop.aegis.identity.auth.repository.AuthActionTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthActionTokenService {

    private static final Duration INVITE_EXPIRATION = Duration.ofHours(48);
    private static final Duration PASSWORD_RESET_EXPIRATION = Duration.ofHours(1);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final long RATE_LIMIT_MAX_TOKENS = 3;
    private static final long RETRY_AFTER_SECONDS = RATE_LIMIT_WINDOW.toSeconds();
    private static final TypeReference<List<String>> PRODUCT_NAMES_TYPE = new TypeReference<>() {
    };

    private final AuthActionTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AuthActionTokenService(AuthActionTokenRepository tokenRepository, ObjectMapper objectMapper) {
        this(tokenRepository, objectMapper, Clock.systemUTC());
    }

    AuthActionTokenService(AuthActionTokenRepository tokenRepository, ObjectMapper objectMapper, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public AuthActionToken createInvite(IdentityActionInviteCommand command) {
        Instant createdAt = clock.instant();
        AuthActionToken token = new AuthActionToken(
                command.keycloakId(),
                normalizeEmail(command.userEmail()),
                command.userName(),
                AuthActionType.INVITE,
                createdAt,
                createdAt.plus(INVITE_EXPIRATION)
        );
        token.addInviteContext(
                command.tenantId(),
                command.tenantName(),
                writeProductNames(command.productNames()),
                command.role(),
                command.inviterName()
        );
        return tokenRepository.save(token);
    }

    @Transactional
    public AuthActionToken createPasswordReset(IdentityUser user) {
        assertPasswordResetAllowed(user.email());
        expirePendingPasswordResetTokens(user.id());
        Instant createdAt = clock.instant();
        AuthActionToken token = new AuthActionToken(
                user.id(),
                normalizeEmail(user.email()),
                user.displayName(),
                AuthActionType.PASSWORD_RESET,
                createdAt,
                createdAt.plus(PASSWORD_RESET_EXPIRATION)
        );
        return tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public AuthActionToken validateInvite(UUID tokenId) {
        return validate(tokenId, AuthActionType.INVITE, false);
    }

    @Transactional
    public AuthActionToken consumeInvite(UUID tokenId) {
        AuthActionToken token = validate(tokenId, AuthActionType.INVITE, true);
        token.markUsed(clock.instant());
        return tokenRepository.save(token);
    }

    @Transactional
    public AuthActionToken consumePasswordReset(UUID tokenId) {
        AuthActionToken token = validate(tokenId, AuthActionType.PASSWORD_RESET, true);
        token.markUsed(clock.instant());
        return tokenRepository.save(token);
    }

    public List<String> productNames(AuthActionToken token) {
        if (token.getProductNames() == null || token.getProductNames().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(token.getProductNames(), PRODUCT_NAMES_TYPE);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to read auth action token product names", exception);
        }
    }

    private AuthActionToken validate(UUID tokenId, AuthActionType expectedType, boolean markExpired) {
        AuthActionToken token = tokenRepository.findById(tokenId)
                .filter(candidate -> candidate.getType() == expectedType)
                .orElseThrow(AuthActionTokenNotFoundException::new);
        if (token.getStatus() == AuthActionStatus.USED) {
            throw new AuthActionTokenUsedException();
        }
        if (token.getStatus() == AuthActionStatus.EXPIRED || token.isExpired(clock.instant())) {
            if (markExpired && token.getStatus() == AuthActionStatus.PENDING) {
                token.markExpired();
                tokenRepository.save(token);
            }
            throw new AuthActionTokenExpiredException();
        }
        return token;
    }

    private void assertPasswordResetAllowed(String email) {
        Instant createdAtAfter = clock.instant().minus(RATE_LIMIT_WINDOW);
        long tokensInWindow = tokenRepository.countByUserEmailIgnoreCaseAndTypeAndCreatedAtAfter(
                normalizeEmail(email),
                AuthActionType.PASSWORD_RESET,
                createdAtAfter
        );
        if (tokensInWindow >= RATE_LIMIT_MAX_TOKENS) {
            throw new AuthRateLimitExceededException(RETRY_AFTER_SECONDS);
        }
    }

    private void expirePendingPasswordResetTokens(String keycloakId) {
        tokenRepository.findAllByKeycloakIdAndTypeAndStatus(
                        keycloakId,
                        AuthActionType.PASSWORD_RESET,
                        AuthActionStatus.PENDING
                )
                .forEach(token -> {
                    token.markExpired();
                    tokenRepository.save(token);
                });
    }

    private String writeProductNames(List<String> productNames) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNullElse(productNames, List.of()));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to write auth action token product names", exception);
        }
    }

    private String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email is required").trim().toLowerCase(Locale.ROOT);
    }
}

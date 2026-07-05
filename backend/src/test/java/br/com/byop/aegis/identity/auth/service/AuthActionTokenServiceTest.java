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
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthActionTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");
    private static final UUID TOKEN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final AuthActionTokenRepository tokenRepository = mock(AuthActionTokenRepository.class);
    private final AuthActionTokenService service = new AuthActionTokenService(
            tokenRepository,
            new ObjectMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldCreateInviteTokenWithContext() {
        when(tokenRepository.save(any(AuthActionToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        IdentityActionInviteCommand command = new IdentityActionInviteCommand(
                "user-id",
                "USER@BYOP.DEV",
                "User",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "BYOP",
                List.of("Aegis"),
                null,
                "EDITOR",
                "Admin"
        );

        AuthActionToken token = service.createInvite(command);

        assertThat(token.getUserEmail()).isEqualTo("user@byop.dev");
        assertThat(token.getType()).isEqualTo(AuthActionType.INVITE);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plusSeconds(172800));
        assertThat(service.productNames(token)).containsExactly("Aegis");
    }

    @Test
    void shouldCreatePasswordResetAndExpirePreviousPendingTokens() {
        AuthActionToken pending = token(AuthActionType.PASSWORD_RESET, AuthActionStatus.PENDING, NOW.plusSeconds(60));
        when(tokenRepository.countByUserEmailIgnoreCaseAndTypeAndCreatedAtAfter(
                "user@byop.dev",
                AuthActionType.PASSWORD_RESET,
                NOW.minusSeconds(900)
        )).thenReturn(2L);
        when(tokenRepository.findAllByKeycloakIdAndTypeAndStatus(
                "user-id",
                AuthActionType.PASSWORD_RESET,
                AuthActionStatus.PENDING
        )).thenReturn(List.of(pending));
        when(tokenRepository.save(any(AuthActionToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthActionToken token = service.createPasswordReset(new IdentityUser(
                "user-id",
                "user",
                "USER@BYOP.DEV",
                "User",
                "Name"
        ));

        assertThat(token.getType()).isEqualTo(AuthActionType.PASSWORD_RESET);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(pending.getStatus()).isEqualTo(AuthActionStatus.EXPIRED);
        verify(tokenRepository).save(pending);
    }

    @Test
    void shouldRejectPasswordResetRateLimit() {
        when(tokenRepository.countByUserEmailIgnoreCaseAndTypeAndCreatedAtAfter(
                "user@byop.dev",
                AuthActionType.PASSWORD_RESET,
                NOW.minusSeconds(900)
        )).thenReturn(3L);
        IdentityUser user = new IdentityUser("user-id", "user", "user@byop.dev", null, null);

        assertThatThrownBy(() -> service.createPasswordReset(user))
                .isInstanceOf(AuthRateLimitExceededException.class);
    }

    @Test
    void shouldValidateAndConsumeInvite() {
        AuthActionToken token = token(AuthActionType.INVITE, AuthActionStatus.PENDING, NOW.plusSeconds(60));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(tokenRepository.save(token)).thenReturn(token);

        assertThat(service.validateInvite(TOKEN_ID)).isEqualTo(token);
        AuthActionToken consumed = service.consumeInvite(TOKEN_ID);

        assertThat(consumed.getStatus()).isEqualTo(AuthActionStatus.USED);
        assertThat(consumed.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldConsumePasswordReset() {
        AuthActionToken token = token(AuthActionType.PASSWORD_RESET, AuthActionStatus.PENDING, NOW.plusSeconds(60));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(tokenRepository.save(token)).thenReturn(token);

        AuthActionToken consumed = service.consumePasswordReset(TOKEN_ID);

        assertThat(consumed.getStatus()).isEqualTo(AuthActionStatus.USED);
        assertThat(consumed.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectMissingUsedAndExpiredTokens() {
        AuthActionToken used = token(AuthActionType.INVITE, AuthActionStatus.USED, NOW.plusSeconds(60));
        AuthActionToken expired = token(AuthActionType.INVITE, AuthActionStatus.PENDING, NOW.minusSeconds(1));
        AuthActionToken alreadyExpired = token(AuthActionType.INVITE, AuthActionStatus.EXPIRED, NOW.plusSeconds(60));
        AuthActionToken wrongType = token(AuthActionType.PASSWORD_RESET, AuthActionStatus.PENDING, NOW.plusSeconds(60));
        UUID missingTokenId = TOKEN_ID;
        UUID usedTokenId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID expiredTokenId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID alreadyExpiredTokenId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID wrongTypeTokenId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(tokenRepository.findById(missingTokenId)).thenReturn(Optional.empty());
        when(tokenRepository.findById(usedTokenId)).thenReturn(Optional.of(used));
        when(tokenRepository.findById(expiredTokenId)).thenReturn(Optional.of(expired));
        when(tokenRepository.findById(alreadyExpiredTokenId)).thenReturn(Optional.of(alreadyExpired));
        when(tokenRepository.findById(wrongTypeTokenId)).thenReturn(Optional.of(wrongType));

        assertThatThrownBy(() -> service.validateInvite(missingTokenId))
                .isInstanceOf(AuthActionTokenNotFoundException.class);
        assertThatThrownBy(() -> service.validateInvite(usedTokenId))
                .isInstanceOf(AuthActionTokenUsedException.class);
        assertThatThrownBy(() -> service.consumeInvite(expiredTokenId))
                .isInstanceOf(AuthActionTokenExpiredException.class);
        assertThatThrownBy(() -> service.validateInvite(expiredTokenId))
                .isInstanceOf(AuthActionTokenExpiredException.class);
        assertThatThrownBy(() -> service.consumeInvite(alreadyExpiredTokenId))
                .isInstanceOf(AuthActionTokenExpiredException.class);
        assertThatThrownBy(() -> service.validateInvite(wrongTypeTokenId))
                .isInstanceOf(AuthActionTokenNotFoundException.class);
        assertThat(expired.getStatus()).isEqualTo(AuthActionStatus.EXPIRED);
        verify(tokenRepository).save(expired);
    }

    @Test
    void shouldReturnEmptyProductNamesWhenContextIsMissing() {
        AuthActionToken token = token(AuthActionType.INVITE, AuthActionStatus.PENDING, NOW.plusSeconds(60));

        assertThat(service.productNames(token)).isEmpty();

        org.springframework.test.util.ReflectionTestUtils.setField(token, "productNames", " ");

        assertThat(service.productNames(token)).isEmpty();
    }

    @Test
    void shouldRejectUnreadableProductNames() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AuthActionTokenService brokenService = new AuthActionTokenService(
                tokenRepository,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        AuthActionToken token = token(AuthActionType.INVITE, AuthActionStatus.PENDING, NOW.plusSeconds(60));
        org.springframework.test.util.ReflectionTestUtils.setField(token, "productNames", "not-json");
        when(objectMapper.readValue(org.mockito.Mockito.eq("not-json"), org.mockito.ArgumentMatchers.any(TypeReference.class)))
                .thenThrow(new JacksonException("invalid") {
                });

        assertThatThrownBy(() -> brokenService.productNames(token))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectUnwritableProductNames() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        AuthActionTokenService brokenService = new AuthActionTokenService(
                tokenRepository,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        List<String> productNames = List.of("Aegis");
        IdentityActionInviteCommand command = new IdentityActionInviteCommand(
                "user-id",
                "user@byop.dev",
                "User",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "BYOP",
                productNames,
                null,
                "EDITOR",
                "Admin"
        );
        when(objectMapper.writeValueAsString(productNames))
                .thenThrow(new JacksonException("invalid") {
                });

        assertThatThrownBy(() -> brokenService.createInvite(command))
                .isInstanceOf(IllegalStateException.class);
    }

    private AuthActionToken token(AuthActionType type, AuthActionStatus status, Instant expiresAt) {
        AuthActionToken token = new AuthActionToken("user-id", "user@byop.dev", "User", type, NOW, expiresAt);
        if (status == AuthActionStatus.USED) {
            token.markUsed(NOW);
        } else if (status == AuthActionStatus.EXPIRED) {
            token.markExpired();
        }
        return token;
    }
}

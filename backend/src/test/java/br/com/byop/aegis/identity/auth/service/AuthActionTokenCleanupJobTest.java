package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.domain.AuthActionStatus;
import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.domain.AuthActionType;
import br.com.byop.aegis.identity.auth.repository.AuthActionTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthActionTokenCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-07-03T04:00:00Z");

    private final AuthActionTokenRepository tokenRepository = mock(AuthActionTokenRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldExpirePendingTokens() {
        AuthActionToken token = new AuthActionToken(
                "user-id",
                "user@byop.dev",
                "User",
                AuthActionType.INVITE,
                NOW.minusSeconds(120),
                NOW.minusSeconds(60)
        );
        when(tokenRepository.findExpiredPending(AuthActionStatus.PENDING, NOW)).thenReturn(List.of(token));

        new AuthActionTokenCleanupJob(tokenRepository, clock).expirePendingTokens();

        assertThat(token.getStatus()).isEqualTo(AuthActionStatus.EXPIRED);
        verify(tokenRepository).save(token);
    }
}

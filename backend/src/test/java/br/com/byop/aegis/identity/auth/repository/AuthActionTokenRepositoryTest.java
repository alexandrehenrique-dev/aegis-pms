package br.com.byop.aegis.identity.auth.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.identity.auth.domain.AuthActionStatus;
import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.domain.AuthActionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuthActionTokenRepositoryTest extends RepositoryTestSupport {

    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");

    @Autowired
    private AuthActionTokenRepository tokenRepository;

    @Test
    void shouldCountRecentTokensAndListPendingByUser() {
        AuthActionToken token = passwordResetToken("User@Byop.Dev", NOW);
        tokenRepository.saveAndFlush(token);

        assertThat(tokenRepository.countByUserEmailIgnoreCaseAndTypeAndCreatedAtAfter(
                "user@byop.dev",
                AuthActionType.PASSWORD_RESET,
                NOW.minusSeconds(1)
        )).isEqualTo(1);
        assertThat(tokenRepository.findAllByKeycloakIdAndTypeAndStatus(
                "user-id",
                AuthActionType.PASSWORD_RESET,
                AuthActionStatus.PENDING
        )).extracting(AuthActionToken::getId).containsExactly(token.getId());
    }

    @Test
    void shouldFindOnlyExpiredPendingTokens() {
        AuthActionToken expired = passwordResetToken("expired@byop.dev", NOW.minusSeconds(120));
        AuthActionToken used = passwordResetToken("used@byop.dev", NOW.minusSeconds(120));
        used.markUsed(NOW.minusSeconds(30));
        AuthActionToken valid = passwordResetToken("valid@byop.dev", NOW);
        tokenRepository.saveAllAndFlush(java.util.List.of(expired, used, valid));

        assertThat(tokenRepository.findExpiredPending(AuthActionStatus.PENDING, NOW))
                .extracting(AuthActionToken::getId)
                .contains(expired.getId())
                .doesNotContain(used.getId(), valid.getId());
    }

    private AuthActionToken passwordResetToken(String email, Instant createdAt) {
        return new AuthActionToken(
                "user-id",
                email,
                "User",
                AuthActionType.PASSWORD_RESET,
                createdAt,
                createdAt.plusSeconds(60)
        );
    }
}

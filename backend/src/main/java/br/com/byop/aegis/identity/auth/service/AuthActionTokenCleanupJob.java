package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.domain.AuthActionStatus;
import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.repository.AuthActionTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class AuthActionTokenCleanupJob {

    private final AuthActionTokenRepository tokenRepository;
    private final Clock clock;

    @Autowired
    public AuthActionTokenCleanupJob(AuthActionTokenRepository tokenRepository) {
        this(tokenRepository, Clock.systemUTC());
    }

    AuthActionTokenCleanupJob(AuthActionTokenRepository tokenRepository, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void expirePendingTokens() {
        Instant now = clock.instant();
        tokenRepository.findExpiredPending(AuthActionStatus.PENDING, now)
                .forEach(this::expire);
    }

    private void expire(AuthActionToken token) {
        token.markExpired();
        tokenRepository.save(token);
    }
}

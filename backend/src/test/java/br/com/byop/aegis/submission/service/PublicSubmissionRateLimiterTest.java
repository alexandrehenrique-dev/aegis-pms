package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.submission.exception.TooManySubmissionsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicSubmissionRateLimiterTest {

    private static final UUID FORM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int MAX_SUBMISSIONS = 10;

    @Test
    void shouldAllowTenSubmissionsAndRejectEleventhInSameWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-29T12:00:00Z"));
        PublicSubmissionRateLimiter limiter = new PublicSubmissionRateLimiter(clock);

        for (int index = 0; index < MAX_SUBMISSIONS; index++) {
            limiter.assertAllowed("127.0.0.1", FORM_ID);
        }

        assertThatThrownBy(() -> limiter.assertAllowed("127.0.0.1", FORM_ID))
                .isInstanceOf(TooManySubmissionsException.class);
    }

    @Test
    void shouldResetCounterAfterTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-29T12:00:00Z"));
        PublicSubmissionRateLimiter limiter = new PublicSubmissionRateLimiter(clock);
        for (int index = 0; index < MAX_SUBMISSIONS; index++) {
            limiter.assertAllowed("127.0.0.1", FORM_ID);
        }

        clock.advance(Duration.ofHours(1).plusSeconds(1));

        limiter.assertAllowed("127.0.0.1", FORM_ID);
    }

    @Test
    void shouldUseDifferentCountersByIpAndForm() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-29T12:00:00Z"));
        PublicSubmissionRateLimiter limiter = new PublicSubmissionRateLimiter(clock);
        for (int index = 0; index < MAX_SUBMISSIONS; index++) {
            limiter.assertAllowed("127.0.0.1", FORM_ID);
        }

        limiter.assertAllowed("127.0.0.2", FORM_ID);
        limiter.assertAllowed("127.0.0.1", UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

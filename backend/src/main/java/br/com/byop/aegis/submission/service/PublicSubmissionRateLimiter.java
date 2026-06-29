package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.submission.exception.TooManySubmissionsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PublicSubmissionRateLimiter {

    private static final int MAX_SUBMISSIONS = 10;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public PublicSubmissionRateLimiter() {
        this(Clock.systemUTC());
    }

    PublicSubmissionRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void assertAllowed(String ipAddress, UUID formId) {
        String key = ipAddress + ":" + formId;
        Instant now = clock.instant();
        WindowCounter counter = counters.compute(key, (ignored, existing) -> activeCounter(existing, now));
        if (counter.increment() > MAX_SUBMISSIONS) {
            throw new TooManySubmissionsException();
        }
    }

    private WindowCounter activeCounter(WindowCounter existing, Instant now) {
        if (existing == null || !existing.activeAt(now)) {
            return new WindowCounter(now.plus(WINDOW));
        }
        return existing;
    }

    private record WindowCounter(Instant resetAt, AtomicInteger count) {

        WindowCounter(Instant resetAt) {
            this(resetAt, new AtomicInteger());
        }

        boolean activeAt(Instant now) {
            return now.isBefore(resetAt);
        }

        int increment() {
            return count.incrementAndGet();
        }
    }
}

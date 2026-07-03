package br.com.byop.aegis.identity.auth.exception;

public class AuthRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public AuthRateLimitExceededException(long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}

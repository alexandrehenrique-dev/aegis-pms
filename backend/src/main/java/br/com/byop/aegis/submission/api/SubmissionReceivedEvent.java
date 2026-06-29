package br.com.byop.aegis.submission.api;

import java.time.Instant;
import java.util.UUID;

public record SubmissionReceivedEvent(UUID formId, UUID productId, Instant receivedAt) {
}

package br.com.byop.aegis.submission.api;

import java.time.OffsetDateTime;

public record SubmissionAnalyticsResponse(
        long total,
        long recent,
        OffsetDateTime lastSubmittedAt
) {
}

package br.com.byop.aegis.content.api;

import java.time.OffsetDateTime;

public record ContentAnalyticsResponse(
        long total,
        long published,
        long pendingReview,
        long drafts,
        long stalePendingReview,
        OffsetDateTime lastUpdatedAt
) {
}

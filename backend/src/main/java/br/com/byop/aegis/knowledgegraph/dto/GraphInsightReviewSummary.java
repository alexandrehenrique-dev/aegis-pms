package br.com.byop.aegis.knowledgegraph.dto;

import java.util.UUID;

public record GraphInsightReviewSummary(
        UUID id,
        String text,
        boolean reviewed
) {
}

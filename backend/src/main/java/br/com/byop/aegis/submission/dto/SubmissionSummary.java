package br.com.byop.aegis.submission.dto;

import java.util.UUID;

public record SubmissionSummary(
        UUID id,
        String date,
        String name,
        String email,
        String source,
        String status,
        String owner,
        String score
) {
}

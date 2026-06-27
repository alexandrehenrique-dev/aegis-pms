package br.com.byop.aegis.submission.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record SubmissionDetail(
        UUID id,
        UUID formId,
        OffsetDateTime date,
        String name,
        String email,
        String source,
        String status,
        String owner,
        Integer score,
        Map<String, Object> answersJson,
        OffsetDateTime createdAt
) {
}

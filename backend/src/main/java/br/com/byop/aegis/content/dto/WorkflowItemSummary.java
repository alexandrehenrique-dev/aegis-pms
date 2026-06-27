package br.com.byop.aegis.content.dto;

import java.util.UUID;

public record WorkflowItemSummary(
        UUID id,
        String title,
        String type,
        String lang,
        String author,
        String status,
        String version
) {
}

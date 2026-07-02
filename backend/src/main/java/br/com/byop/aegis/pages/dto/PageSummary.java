package br.com.byop.aegis.pages.dto;

import java.util.UUID;

public record PageSummary(
        UUID id,
        String slug,
        String title,
        String locale,
        String status,
        int version
) {
}

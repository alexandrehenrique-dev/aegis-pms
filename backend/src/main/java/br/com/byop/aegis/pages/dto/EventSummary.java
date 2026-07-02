package br.com.byop.aegis.pages.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventSummary(
        UUID id,
        String title,
        LocalDateTime datetime,
        String location,
        String type,
        String visibility
) {
}

package br.com.byop.aegis.pages.dto;

import java.util.Map;
import java.util.UUID;

public record PageSectionResponse(
        UUID id,
        String type,
        String variant,
        int order,
        Map<String, Object> content,
        Map<String, Object> settings
) {
}

package br.com.byop.aegis.form.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FormDetail(
        UUID id,
        String name,
        String type,
        String status,
        List<Map<String, Object>> fields,
        List<Map<String, Object>> deliveryChannels,
        String publication,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

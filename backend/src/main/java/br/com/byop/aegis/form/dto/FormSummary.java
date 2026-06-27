package br.com.byop.aegis.form.dto;

import java.util.UUID;

public record FormSummary(
        UUID id,
        String name,
        String type,
        String status,
        String responses,
        String conversion,
        String lastActivity,
        String publication
) {
}

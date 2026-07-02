package br.com.byop.aegis.pages.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateSectionRequest(
        @NotBlank String type,
        String variant,
        @NotNull Integer order,
        @NotNull Map<String, Object> content,
        Map<String, Object> settings
) {
}

package br.com.byop.aegis.form.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record UpdateFormDefinitionRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotNull List<Map<String, Object>> fields
) {
}

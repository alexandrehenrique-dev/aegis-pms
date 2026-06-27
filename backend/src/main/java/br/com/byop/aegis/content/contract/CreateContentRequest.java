package br.com.byop.aegis.content.contract;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateContentRequest(
        @NotBlank String title,
        @NotBlank String type,
        @NotBlank String lang,
        String body,
        String summary,
        String difficultyLevel,
        String category,
        String topic,
        Map<String, Object> metadata
) {
}

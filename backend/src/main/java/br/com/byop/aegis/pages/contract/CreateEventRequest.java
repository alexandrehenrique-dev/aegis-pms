package br.com.byop.aegis.pages.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateEventRequest(
        @NotBlank String title,
        @NotNull LocalDateTime datetime,
        @NotBlank String location,
        @NotBlank String type,
        @NotBlank String visibility,
        String description,
        UUID imageAssetId
) {
}

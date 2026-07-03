package br.com.byop.aegis.feedback.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFeedbackRequest(
        UUID productId,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 40) String priority,
        @NotBlank @Size(max = 4000) String description,
        @Size(max = 240) String screenName,
        UUID attachmentAssetId
) {
}

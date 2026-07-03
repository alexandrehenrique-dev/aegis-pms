package br.com.byop.aegis.feedback.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFeedbackStatusRequest(
        @NotBlank @Size(max = 40) String status
) {
}

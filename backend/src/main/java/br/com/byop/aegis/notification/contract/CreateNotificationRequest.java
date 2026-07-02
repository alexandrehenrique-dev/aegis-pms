package br.com.byop.aegis.notification.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateNotificationRequest(
        @NotBlank String type,
        @NotBlank String title,
        @NotBlank String bodyMarkdown,
        @NotBlank String presentationMode,
        @NotNull @Valid Target target
) {

    public record Target(
            @NotBlank String type,
            UUID tenantId,
            List<String> userIds
    ) {
    }
}

package br.com.byop.aegis.pages.contract;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderSectionsRequest(
        @NotEmpty List<UUID> sectionIds
) {
}

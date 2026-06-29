package br.com.byop.aegis.knowledgegraph.contract;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ResolveGraphOrphansRequest(
        @NotEmpty List<UUID> ids,
        String action
) {
}

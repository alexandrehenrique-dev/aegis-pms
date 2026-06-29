package br.com.byop.aegis.knowledgegraph.contract;

import jakarta.validation.constraints.NotBlank;

public record ResolveGraphOrphanRequest(@NotBlank String action) {
}

package br.com.byop.aegis.knowledgegraph.contract;

import jakarta.validation.constraints.NotBlank;

public record ReviewGraphInsightRequest(@NotBlank String text) {
}

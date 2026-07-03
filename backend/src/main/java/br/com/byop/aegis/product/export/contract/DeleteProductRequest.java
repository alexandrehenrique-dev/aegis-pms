package br.com.byop.aegis.product.export.contract;

import jakarta.validation.constraints.NotBlank;

public record DeleteProductRequest(
        @NotBlank
        String confirmationText
) {
}

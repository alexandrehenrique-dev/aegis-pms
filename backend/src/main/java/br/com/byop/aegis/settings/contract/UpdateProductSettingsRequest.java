package br.com.byop.aegis.settings.contract;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code PUT /products/{productId}/settings}. O artefato da etapa
 * 17 e o mock do frontend ({@code ProductSettings.tsx}) nao definem um
 * payload concreto — esta etapa cobre apenas o campo que ja existe e e
 * editavel em {@link br.com.byop.aegis.product.domain.Product} (nome);
 * branding/SEO/publicacao ficam como retrofit pendente.
 */
public record UpdateProductSettingsRequest(
        @NotBlank String name
) {
}

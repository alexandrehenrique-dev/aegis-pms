package br.com.byop.aegis.product.api;

import java.util.UUID;

/**
 * Publicado apos um {@code Product} ser criado, com a estrategia de storage
 * de assets escolhida no momento da criacao. Permite que o modulo {@code asset}
 * provisione a estrutura de pastas do produto sem que {@code product} precise
 * depender do modulo {@code asset} (evita dependencia ciclica entre modulos).
 *
 * <p>{@code productType} (nome do enum {@code ProductTypeKey}, ex. {@code
 * "SITE_INSTITUCIONAL"}) e {@code defaultLocale} foram adicionados na Etapa 26
 * para que o modulo {@code pages} possa criar o esqueleto de paginas/secoes do
 * tipo de produto (ADR-0017) sem depender de {@code product.domain} — mesmo
 * principio de desacoplamento por evento ja usado pelo modulo {@code asset}.
 */
public record ProductCreatedEvent(
        UUID tenantId,
        UUID productId,
        AssetStorageStrategy assetStorageStrategy,
        String productType,
        String defaultLocale
) {
}

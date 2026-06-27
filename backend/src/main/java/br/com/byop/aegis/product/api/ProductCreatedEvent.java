package br.com.byop.aegis.product.api;

import java.util.UUID;

/**
 * Publicado apos um {@code Product} ser criado, com a estrategia de storage
 * de assets escolhida no momento da criacao. Permite que o modulo {@code asset}
 * provisione a estrutura de pastas do produto sem que {@code product} precise
 * depender do modulo {@code asset} (evita dependencia ciclica entre modulos).
 */
public record ProductCreatedEvent(
        UUID tenantId,
        UUID productId,
        AssetStorageStrategy assetStorageStrategy
) {
}

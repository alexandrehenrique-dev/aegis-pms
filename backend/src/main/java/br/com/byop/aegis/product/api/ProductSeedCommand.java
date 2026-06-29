package br.com.byop.aegis.product.api;

import java.util.Set;
import java.util.UUID;

/**
 * Comando publico usado pelo seed local para garantir um produto e seus modulos.
 */
public record ProductSeedCommand(
        UUID tenantId,
        String key,
        String name,
        String type,
        String status,
        Set<ModuleKey> enabledModules
) {
}

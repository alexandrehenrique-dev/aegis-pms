package br.com.byop.aegis.tenant.api;

/**
 * Comando publico usado pelo seed local para garantir um tenant de demonstracao.
 */
public record TenantSeedCommand(
        String key,
        String name,
        String plan,
        String status
) {
}

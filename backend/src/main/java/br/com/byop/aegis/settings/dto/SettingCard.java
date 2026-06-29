package br.com.byop.aegis.settings.dto;

/**
 * Um card da grade de {@code SettingsOverview.tsx} — {@code GET
 * .../settings/overview} retorna um {@code SettingCard[]} (Produto, Tenant,
 * Equipe, Permissoes, Integracoes, Seguranca, Auditoria, SEO).
 */
public record SettingCard(
        String name,
        String description,
        String status,
        String lastUpdated,
        String owner,
        String risk
) {
}

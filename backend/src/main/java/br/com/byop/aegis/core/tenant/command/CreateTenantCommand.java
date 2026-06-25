package br.com.byop.aegis.core.tenant.command;

public record CreateTenantCommand(
        String key,
        String name
) {
}

package br.com.byop.aegis.tenant.command;

public record CreateTenantCommand(
        String key,
        String name
) {
}

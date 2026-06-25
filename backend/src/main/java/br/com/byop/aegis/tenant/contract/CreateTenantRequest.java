package br.com.byop.aegis.tenant.contract;

import br.com.byop.aegis.tenant.command.CreateTenantCommand;
import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
        @NotBlank String key,
        @NotBlank String name
) {

    public CreateTenantCommand toCommand() {
        return new CreateTenantCommand(key, name);
    }
}

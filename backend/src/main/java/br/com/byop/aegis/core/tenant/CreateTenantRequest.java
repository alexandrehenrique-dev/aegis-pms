package br.com.byop.aegis.core.tenant;

import br.com.byop.aegis.core.tenant.command.CreateTenantCommand;
import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
        @NotBlank String key,
        @NotBlank String name
) {

    public CreateTenantCommand toCommand() {
        return new CreateTenantCommand(key, name);
    }
}

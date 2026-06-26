package br.com.byop.aegis.tenant.contract;

import br.com.byop.aegis.tenant.command.CreateTenantCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
        @NotBlank String key,
        @NotBlank String name,
        String plan,
        @Email String initialAdminEmail
) {

    public CreateTenantCommand toCommand() {
        return new CreateTenantCommand(key, name, plan, initialAdminEmail);
    }
}

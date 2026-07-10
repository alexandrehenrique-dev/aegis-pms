package br.com.byop.aegis.product.contract;

import br.com.byop.aegis.product.command.UpdateProductCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateProductRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String status,
        List<String> modules
) {

    public UpdateProductCommand toCommand() {
        return new UpdateProductCommand(name, type, status, modules);
    }
}

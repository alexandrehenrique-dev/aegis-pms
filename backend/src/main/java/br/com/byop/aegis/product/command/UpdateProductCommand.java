package br.com.byop.aegis.product.command;

import java.util.List;

public record UpdateProductCommand(
        String name,
        String type,
        String status,
        List<String> modules
) {
    public UpdateProductCommand(String name, String type, String status) {
        this(name, type, status, null);
    }
}

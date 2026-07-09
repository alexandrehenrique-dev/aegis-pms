package br.com.byop.aegis.product.command;

public record UpdateProductCommand(
        String name,
        String type,
        String status
) {
}

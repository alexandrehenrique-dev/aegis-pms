package br.com.byop.aegis.product.export.dto;

/**
 * Destinatário de um e-mail de exportação de produto.
 *
 * @param email endereço de e-mail do destinatário
 * @param name  nome de exibição usado na saudação do template
 */
public record ExportRecipient(String email, String name) {
}

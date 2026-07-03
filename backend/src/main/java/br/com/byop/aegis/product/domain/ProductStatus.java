package br.com.byop.aegis.product.domain;

/**
 * Status de ciclo de vida de um produto administrado pelo Aegis.
 */
public enum ProductStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED,
    DELETING,
    DELETED,
    EXPORT_FAILED,
    DELETE_FAILED
}

package br.com.byop.aegis.product.repository;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.domain.ProductModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link ProductModule}, catalogo de modulos habilitados ou
 * desabilitados por produto.
 */
@Repository
public interface ProductModuleRepository extends JpaRepository<ProductModule, UUID> {

    /**
     * Lista todos os modulos configurados para um produto.
     *
     * @param productId identificador do produto
     * @return modulos associados ao produto
     */
    List<ProductModule> findAllByProductId(UUID productId);

    /**
     * Busca a configuracao de um modulo especifico em um produto.
     *
     * @param productId identificador do produto
     * @param moduleKey chave do modulo
     * @return modulo encontrado, ou {@link Optional#empty()} quando inexistente
     */
    Optional<ProductModule> findByProductIdAndModuleKey(UUID productId, ModuleKey moduleKey);

    /**
     * Verifica se um modulo esta habilitado em um produto.
     *
     * @param productId identificador do produto
     * @param moduleKey chave do modulo
     * @return {@code true} quando o modulo existe e esta habilitado
     */
    boolean existsByProductIdAndModuleKeyAndEnabledTrue(UUID productId, ModuleKey moduleKey);

    /**
     * Conta os modulos habilitados de um produto.
     *
     * @param productId identificador do produto
     * @return numero de modulos com {@code enabled = true}
     */
    long countByProductIdAndEnabledTrue(UUID productId);
}

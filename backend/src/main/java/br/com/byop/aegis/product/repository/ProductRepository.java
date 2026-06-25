package br.com.byop.aegis.product.repository;

import br.com.byop.aegis.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Product}, unidade central Product First do Aegis.
 * Produtos pertencem a exatamente um tenant e possuem chave unica por tenant.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Lista produtos vinculados a um tenant.
     *
     * @param tenantId identificador do tenant proprietario
     * @return produtos do tenant informado
     */
    List<Product> findAllByTenantId(UUID tenantId);

    /**
     * Busca um produto pela chave unica dentro de um tenant.
     *
     * @param tenantId identificador do tenant proprietario
     * @param key chave semantica do produto
     * @return produto encontrado, ou {@link Optional#empty()} quando inexistente
     */
    Optional<Product> findByTenantIdAndKey(UUID tenantId, String key);

    /**
     * Verifica se a chave de produto ja existe dentro do tenant.
     *
     * @param tenantId identificador do tenant proprietario
     * @param key chave semantica do produto
     * @return {@code true} quando a chave ja esta em uso no tenant
     */
    boolean existsByTenantIdAndKey(UUID tenantId, String key);
}

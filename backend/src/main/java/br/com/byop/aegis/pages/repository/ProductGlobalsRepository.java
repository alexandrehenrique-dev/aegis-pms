package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.pages.domain.ProductGlobals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link ProductGlobals}, relacao 1:1 com produto (ADR-0013) —
 * navbar, footer e redes sociais configurados uma unica vez por produto.
 */
@Repository
public interface ProductGlobalsRepository extends JpaRepository<ProductGlobals, UUID> {

    /**
     * Busca a configuracao global de um produto.
     *
     * @param productId identificador do produto proprietario
     * @return configuracao encontrada, ou {@link Optional#empty()} quando o
     *         produto ainda nao tem {@link ProductGlobals} configurado
     */
    Optional<ProductGlobals> findByProductId(UUID productId);
}

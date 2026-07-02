package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.pages.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de {@link Page}, sempre escopado por produto — pagina institucional
 * composta por secoes/blocos visuais tipados, navegada por URL propria (ADR-0012).
 */
@Repository
public interface PageRepository extends JpaRepository<Page, UUID> {

    /**
     * Lista as paginas de um produto.
     *
     * @param productId identificador do produto proprietario
     * @return paginas pertencentes ao produto informado
     */
    List<Page> findAllByProductId(UUID productId);

    /**
     * Busca uma pagina pelo identificador dentro de um produto, garantindo o
     * isolamento por produto (pagina de outro produto nunca e retornada).
     *
     * @param productId identificador do produto proprietario
     * @param id identificador da pagina
     * @return pagina encontrada, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<Page> findByProductIdAndId(UUID productId, UUID id);

    /**
     * Busca uma pagina pelo {@code slug} dentro de um produto — usada para
     * garantir unicidade de slug por produto antes da criacao/edicao.
     *
     * @param productId identificador do produto proprietario
     * @param slug slug da pagina
     * @return pagina encontrada, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<Page> findByProductIdAndSlug(UUID productId, String slug);
}

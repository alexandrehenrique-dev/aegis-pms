package br.com.byop.aegis.knowledgegraph.repository;

import br.com.byop.aegis.knowledgegraph.domain.GraphInsightReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de revisoes de insights textuais do Knowledge Graph, escopadas
 * por produto para manter a curadoria idempotente.
 */
@Repository
public interface GraphInsightReviewRepository extends JpaRepository<GraphInsightReview, UUID> {

    /**
     * Busca a revisao de um insight pelo hash do texto dentro de um produto.
     *
     * @param productId identificador do produto proprietario
     * @param textHash hash SHA-256 do texto revisado
     * @return revisao encontrada, ou {@link Optional#empty()} quando ainda nao revisada
     */
    Optional<GraphInsightReview> findByProductIdAndTextHash(UUID productId, String textHash);
}

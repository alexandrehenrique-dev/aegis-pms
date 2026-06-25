package br.com.byop.aegis.knowledgegraph.repository;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de arestas do Knowledge Graph, sempre escopadas por tenant e produto.
 */
@Repository
public interface GraphEdgeRepository extends JpaRepository<GraphEdge, UUID> {

    /**
     * Lista todas as arestas de um tenant.
     *
     * @param tenantId identificador do tenant proprietario
     * @return arestas pertencentes ao tenant informado
     */
    List<GraphEdge> findAllByTenantId(UUID tenantId);

    /**
     * Lista todas as arestas de um produto.
     *
     * @param productId identificador do produto proprietario
     * @return arestas pertencentes ao produto informado
     */
    List<GraphEdge> findAllByProductId(UUID productId);

    /**
     * Lista arestas de um produto que partem de um no de origem.
     *
     * @param productId identificador do produto proprietario
     * @param sourceNodeId identificador do no de origem
     * @return arestas do produto originadas pelo no informado
     */
    List<GraphEdge> findAllByProductIdAndSourceNodeId(UUID productId, UUID sourceNodeId);

    /**
     * Lista arestas de um produto que chegam a um no de destino.
     *
     * @param productId identificador do produto proprietario
     * @param targetNodeId identificador do no de destino
     * @return arestas do produto direcionadas ao no informado
     */
    List<GraphEdge> findAllByProductIdAndTargetNodeId(UUID productId, UUID targetNodeId);

    /**
     * Lista arestas que partem de um no de origem.
     *
     * @param sourceNodeId identificador do no de origem
     * @return arestas originadas pelo no informado
     */
    List<GraphEdge> findAllBySourceNodeId(UUID sourceNodeId);

    /**
     * Lista arestas que chegam a um no de destino.
     *
     * @param targetNodeId identificador do no de destino
     * @return arestas direcionadas ao no informado
     */
    List<GraphEdge> findAllByTargetNodeId(UUID targetNodeId);

    /**
     * Lista arestas de um tipo semantico.
     *
     * @param edgeType tipo semantico da relacao
     * @return arestas do tipo informado
     */
    List<GraphEdge> findAllByEdgeType(GraphEdgeType edgeType);

    /**
     * Verifica se uma relacao ja existe entre dois nos.
     *
     * @param sourceNodeId identificador do no de origem
     * @param targetNodeId identificador do no de destino
     * @param edgeType tipo semantico da relacao
     * @return {@code true} quando a relacao ja existe
     */
    boolean existsBySourceNodeIdAndTargetNodeIdAndEdgeType(UUID sourceNodeId, UUID targetNodeId,
                                                           GraphEdgeType edgeType);
}

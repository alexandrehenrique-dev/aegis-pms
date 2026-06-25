package br.com.byop.aegis.knowledgegraph.repository;

import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de nos do Knowledge Graph, sempre escopados por tenant e produto.
 */
@Repository
public interface GraphNodeRepository extends JpaRepository<GraphNode, UUID> {

    /**
     * Lista todos os nos de um tenant.
     *
     * @param tenantId identificador do tenant proprietario
     * @return nos pertencentes ao tenant informado
     */
    List<GraphNode> findAllByTenantId(UUID tenantId);

    /**
     * Lista todos os nos de um produto.
     *
     * @param productId identificador do produto proprietario
     * @return nos pertencentes ao produto informado
     */
    List<GraphNode> findAllByProductId(UUID productId);

    /**
     * Lista nos de um produto cujo rotulo contenha o termo informado, sem diferenciar maiusculas.
     *
     * @param productId identificador do produto proprietario
     * @param label trecho do rotulo pesquisado
     * @return nos do produto cujo rotulo corresponde ao termo
     */
    List<GraphNode> findAllByProductIdAndLabelContainingIgnoreCase(UUID productId, String label);

    /**
     * Lista todos os nos de um tipo semantico.
     *
     * @param nodeType tipo semantico do no no grafo
     * @return nos do tipo informado
     */
    List<GraphNode> findAllByNodeType(GraphNodeType nodeType);

    /**
     * Lista nos associados a uma referencia externa.
     *
     * @param refType tipo da entidade referenciada
     * @param refId identificador da entidade referenciada
     * @return nos vinculados a referencia informada
     */
    List<GraphNode> findAllByRefTypeAndRefId(String refType, String refId);

    /**
     * Busca um no pela referencia unica dentro de um produto.
     *
     * @param productId identificador do produto proprietario
     * @param refType tipo da entidade referenciada
     * @param refId identificador da entidade referenciada
     * @return no encontrado, ou {@link Optional#empty()} quando inexistente
     */
    Optional<GraphNode> findByProductIdAndRefTypeAndRefId(UUID productId, String refType, String refId);

    /**
     * Busca um no pelo identificador dentro de um produto.
     *
     * @param productId identificador do produto proprietario
     * @param id identificador do no
     * @return no encontrado, ou {@link Optional#empty()} quando inexistente no produto
     */
    Optional<GraphNode> findByProductIdAndId(UUID productId, UUID id);

    /**
     * Verifica se uma referencia ja possui no dentro de um produto.
     *
     * @param productId identificador do produto proprietario
     * @param refType tipo da entidade referenciada
     * @param refId identificador da entidade referenciada
     * @return {@code true} quando a referencia ja esta vinculada a um no do produto
     */
    boolean existsByProductIdAndRefTypeAndRefId(UUID productId, String refType, String refId);
}

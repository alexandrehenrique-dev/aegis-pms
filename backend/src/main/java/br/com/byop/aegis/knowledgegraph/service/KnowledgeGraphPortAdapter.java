package br.com.byop.aegis.knowledgegraph.service;

import br.com.byop.aegis.knowledgegraph.api.KnowledgeGraphPort;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.repository.GraphEdgeRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementacao de {@link KnowledgeGraphPort} — unico ponto de entrada que outros
 * modulos usam para integrar com o Knowledge Graph, escondendo a logica interna de
 * idempotencia (criar-se-ausente) atras de operacoes simples.
 */
@Component
public class KnowledgeGraphPortAdapter implements KnowledgeGraphPort {

    private static final String CONTENT_REF_TYPE = "CONTENT";

    private final GraphNodeRepository nodeRepository;
    private final GraphEdgeRepository edgeRepository;
    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphPortAdapter(GraphNodeRepository nodeRepository, GraphEdgeRepository edgeRepository,
                                     KnowledgeGraphService knowledgeGraphService) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nodeExists(UUID productId, UUID nodeId) {
        return nodeRepository.findByProductIdAndId(productId, nodeId).isPresent();
    }

    @Override
    @Transactional
    public UUID ensureContentNode(UUID productId, String refId, String label) {
        return nodeRepository.findByProductIdAndRefTypeAndRefId(productId, CONTENT_REF_TYPE, refId)
                .map(GraphNode::getId)
                .orElseGet(() -> knowledgeGraphService.createNode(productId, new CreateGraphNodeRequest(
                        GraphNodeType.CONTENT, CONTENT_REF_TYPE, refId, label, null, null
                )).id());
    }

    @Override
    @Transactional
    public void ensureRelatedToEdge(UUID productId, UUID sourceNodeId, UUID targetNodeId) {
        if (edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO)) {
            return;
        }
        try {
            knowledgeGraphService.createEdge(productId, new CreateGraphEdgeRequest(
                    sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO, null, null
            ));
        } catch (DuplicateGraphEdgeException _) {
            // condicao de corrida: edge criada concorrentemente — idempotente, nada a fazer
        }
    }
}

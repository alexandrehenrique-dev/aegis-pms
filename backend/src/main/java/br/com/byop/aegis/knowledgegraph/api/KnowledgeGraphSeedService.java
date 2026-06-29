package br.com.byop.aegis.knowledgegraph.api;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.repository.GraphEdgeRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

/**
 * Porta publica do modulo Knowledge Graph para seeds locais idempotentes.
 */
@Service
public class KnowledgeGraphSeedService {

    private static final String EMPTY_JSON = "{}";

    private final ProductReferenceService productReferenceService;
    private final GraphNodeRepository nodeRepository;
    private final GraphEdgeRepository edgeRepository;

    public KnowledgeGraphSeedService(ProductReferenceService productReferenceService,
                                     GraphNodeRepository nodeRepository,
                                     GraphEdgeRepository edgeRepository) {
        this.productReferenceService = productReferenceService;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Transactional
    public GraphSeedNodeReference ensureNode(GraphSeedNodeCommand command) {
        GraphNode node = nodeRepository.findByProductIdAndRefTypeAndRefId(
                command.productId(),
                command.refType(),
                command.refId()
        ).orElseGet(() -> createNode(command));
        node.reposition(command.x(), command.y());
        node.replaceMetadataJson(normalizedMetadata(command.metadataJson()));
        GraphNode saved = nodeRepository.save(node);
        return new GraphSeedNodeReference(
                saved.getId(),
                saved.getProductId(),
                saved.getRefType(),
                saved.getRefId(),
                saved.getLabel()
        );
    }

    @Transactional
    public void ensureEdge(GraphSeedEdgeCommand command) {
        GraphEdgeType edgeType = GraphEdgeType.valueOf(normalize(command.edgeType()));
        if (edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(
                command.sourceNodeId(),
                command.targetNodeId(),
                edgeType
        )) {
            return;
        }
        ProductReference product = productReferenceService.getRequiredReference(command.productId());
        edgeRepository.save(new GraphEdge(
                product.tenantId(),
                command.productId(),
                command.sourceNodeId(),
                command.targetNodeId(),
                edgeType
        ));
    }

    private GraphNode createNode(GraphSeedNodeCommand command) {
        ProductReference product = productReferenceService.getRequiredReference(command.productId());
        return new GraphNode(new GraphNode.Creation(
                product.tenantId(),
                command.productId(),
                GraphNodeType.valueOf(normalize(command.nodeType())),
                command.refType(),
                command.refId(),
                command.label(),
                command.slug(),
                normalizedMetadata(command.metadataJson()),
                command.x(),
                command.y()
        ));
    }

    private String normalizedMetadata(String metadataJson) {
        return metadataJson == null || metadataJson.isBlank() ? EMPTY_JSON : metadataJson;
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}

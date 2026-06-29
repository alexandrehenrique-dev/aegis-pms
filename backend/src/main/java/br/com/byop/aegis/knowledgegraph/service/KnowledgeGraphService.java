package br.com.byop.aegis.knowledgegraph.service;

import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.knowledgegraph.api.GraphNodeContentPreview;
import br.com.byop.aegis.knowledgegraph.api.GraphNodeContentPreviewPort;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.contract.ResolveGraphOrphanRequest;
import br.com.byop.aegis.knowledgegraph.contract.ResolveGraphOrphansRequest;
import br.com.byop.aegis.knowledgegraph.contract.ReviewGraphInsightRequest;
import br.com.byop.aegis.knowledgegraph.contract.UpdateGraphNodePositionRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphInsightReview;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphInsightReviewSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNeighborSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodePreview;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphRelatedSummary;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphNodeException;
import br.com.byop.aegis.knowledgegraph.exception.GraphNodeNotFoundException;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphOrphanActionException;
import br.com.byop.aegis.knowledgegraph.mapper.GraphEdgeMapper;
import br.com.byop.aegis.knowledgegraph.mapper.GraphNodeMapper;
import br.com.byop.aegis.knowledgegraph.repository.GraphEdgeRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphInsightReviewRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class KnowledgeGraphService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeGraphService.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };
    private static final String ACTION_ARCHIVE = "ARQUIVAR";
    private static final String ACTION_ASSOCIATE = "ASSOCIAR";
    private static final String ACTION_LINK = "VINCULAR";
    private static final String ACTION_MERGE = "MESCLAR";
    private static final String ACTION_REVIEW = "REVISAR";
    private static final String METADATA_KEY_ORPHAN_RESOLVED = "orphanResolved";
    private static final String METADATA_KEY_ORPHAN_ACTION = "orphanAction";
    private static final String METADATA_KEY_STATUS = "status";
    private static final String METADATA_KEY_SUMMARY = "summary";
    private static final String METADATA_KEY_DIFFICULTY = "difficulty";
    private static final String METADATA_KEY_THUMBNAIL = "thumbnail";
    private static final String STATUS_ARCHIVED = "arquivado";
    private static final String STATUS_ASSOCIATED = "associado";
    private static final String STATUS_LINKED = "vinculado";
    private static final String STATUS_MERGED = "mesclado";
    private static final String STATUS_REVIEWED = "revisado";
    private static final String EMPTY_JSON = "{}";

    private final ProductReferenceService productReferenceService;
    private final GraphNodeContentPreviewPort contentPreviewPort;
    private final GraphNodeRepository nodeRepository;
    private final GraphEdgeRepository edgeRepository;
    private final GraphInsightReviewRepository insightReviewRepository;
    private final GraphNodeMapper nodeMapper;
    private final GraphEdgeMapper edgeMapper;
    private final GraphConsistencyPolicy consistencyPolicy;

    public KnowledgeGraphService(ProductReferenceService productReferenceService,
                                 GraphNodeContentPreviewPort contentPreviewPort,
                                 GraphNodeRepository nodeRepository,
                                 GraphEdgeRepository edgeRepository,
                                 GraphInsightReviewRepository insightReviewRepository,
                                 GraphNodeMapper nodeMapper,
                                 GraphEdgeMapper edgeMapper, GraphConsistencyPolicy consistencyPolicy) {
        this.productReferenceService = productReferenceService;
        this.contentPreviewPort = contentPreviewPort;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.insightReviewRepository = insightReviewRepository;
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.consistencyPolicy = consistencyPolicy;
    }

    @Transactional
    public GraphNodeDetail createNode(UUID productId, CreateGraphNodeRequest request) {
        ProductReference product = productReferenceService.getRequiredReference(productId);
        if (nodeRepository.existsByProductIdAndRefTypeAndRefId(productId, request.refType(), request.refId())) {
            throw new DuplicateGraphNodeException(productId, request.refType(), request.refId());
        }

        GraphNode node = nodeMapper.toEntity(request, product.tenantId(), productId);
        return nodeMapper.toDetail(nodeRepository.save(node));
    }

    @Transactional(readOnly = true)
    public GraphNodeDetail findNode(UUID productId, UUID nodeId) {
        return nodeMapper.toDetail(findNodeInProduct(productId, nodeId));
    }

    @Transactional
    public GraphNodeDetail updatePosition(UUID productId, UUID nodeId, UpdateGraphNodePositionRequest request) {
        GraphNode node = findNodeInProduct(productId, nodeId);
        node.reposition(request.x(), request.y());
        return nodeMapper.toDetail(nodeRepository.save(node));
    }

    @Transactional(readOnly = true)
    public List<GraphNodeSummary> searchNodes(UUID productId, String q) {
        productReferenceService.getRequiredReference(productId);
        List<GraphNode> nodes = nodeRepository.findAllByProductId(productId);
        if (!isBlank(q)) {
            String term = q.trim().toLowerCase();
            nodes = nodes.stream()
                    .filter(node -> matchesNodeSearch(node, term))
                    .toList();
        }

        return nodes.stream()
                .map(nodeMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GraphNodeSummary> findOrphans(UUID productId) {
        productReferenceService.getRequiredReference(productId);
        return nodeRepository.findOrphansByProductId(productId)
                .stream()
                .filter(this::isActionableOrphan)
                .map(nodeMapper::toSummary)
                .toList();
    }

    @Transactional
    public GraphNodeDetail resolveOrphan(UUID productId, UUID nodeId, ResolveGraphOrphanRequest request) {
        String action = normalizedAction(request.action());
        return resolveOrphanNode(productId, nodeId, action);
    }

    @Transactional
    public List<GraphNodeDetail> resolveOrphans(UUID productId, ResolveGraphOrphansRequest request) {
        String action = request.action() == null || request.action().isBlank() ? ACTION_REVIEW : request.action();
        String normalizedAction = normalizedAction(action);
        return request.ids()
                .stream()
                .map(nodeId -> resolveOrphanNode(productId, nodeId, normalizedAction))
                .toList();
    }

    @Transactional(readOnly = true)
    public GraphNodePreview previewNode(UUID productId, UUID nodeId) {
        GraphNode node = findNodeInProduct(productId, nodeId);
        Optional<GraphNodeContentPreview> contentPreview = contentPreviewPort.findPreview(
                productId,
                node.getId(),
                node.getRefId()
        );
        if (contentPreview.isPresent()) {
            GraphNodeContentPreview preview = contentPreview.get();
            return new GraphNodePreview(
                    node.getId(),
                    node.getLabel(),
                    node.getNodeType().name(),
                    preview.summary(),
                    preview.difficulty(),
                    preview.thumbnail()
            );
        }
        Map<String, Object> metadata = metadataFrom(node.getMetadataJson());
        return new GraphNodePreview(
                node.getId(),
                node.getLabel(),
                node.getNodeType().name(),
                stringValue(metadata.get(METADATA_KEY_SUMMARY)),
                stringValue(metadata.get(METADATA_KEY_DIFFICULTY)),
                stringValue(metadata.get(METADATA_KEY_THUMBNAIL))
        );
    }

    @Transactional
    public GraphInsightReviewSummary reviewInsight(UUID productId, ReviewGraphInsightRequest request) {
        ProductReference product = productReferenceService.getRequiredReference(productId);
        String text = request.text().trim();
        String textHash = deterministicTextHash(text);
        GraphInsightReview review = insightReviewRepository.findByProductIdAndTextHash(productId, textHash)
                .orElseGet(() -> insightReviewRepository.save(
                        new GraphInsightReview(product.tenantId(), productId, textHash, text)
                ));
        return new GraphInsightReviewSummary(review.getId(), review.getText(), review.isReviewed());
    }

    @Transactional
    public GraphEdgeDetail createEdge(UUID productId, CreateGraphEdgeRequest request) {
        productReferenceService.getRequiredReference(productId);
        GraphNode sourceNode = nodeRepository.findById(request.sourceNodeId())
                .orElseThrow(() -> new GraphNodeNotFoundException(request.sourceNodeId()));
        GraphNode targetNode = nodeRepository.findById(request.targetNodeId())
                .orElseThrow(() -> new GraphNodeNotFoundException(request.targetNodeId()));
        consistencyPolicy.validateEdgeNodes(productId, sourceNode, targetNode);

        if (edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(
                request.sourceNodeId(),
                request.targetNodeId(),
                request.edgeType()
        )) {
            throw new DuplicateGraphEdgeException(request.sourceNodeId(), request.targetNodeId(), request.edgeType());
        }

        GraphEdge edge = edgeMapper.toEntity(request, sourceNode.getTenantId(), productId);
        return edgeMapper.toDetail(edgeRepository.save(edge));
    }

    @Transactional(readOnly = true)
    public List<GraphNeighborSummary> findNeighbors(UUID productId, UUID nodeId) {
        findNodeInProduct(productId, nodeId);

        return edgeRepository.findAllByProductIdAndSourceNodeId(productId, nodeId)
                .stream()
                .map(edge -> new GraphNeighborSummary(
                        nodeMapper.toSummary(findNodeInProduct(productId, edge.getTargetNodeId())),
                        edgeMapper.toSummary(edge)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GraphRelatedSummary> findRelated(UUID productId, UUID nodeId) {
        findNodeInProduct(productId, nodeId);

        return edgeRepository.findAllByProductIdAndTargetNodeId(productId, nodeId)
                .stream()
                .map(edge -> new GraphRelatedSummary(
                        nodeMapper.toSummary(findNodeInProduct(productId, edge.getSourceNodeId())),
                        edgeMapper.toSummary(edge)
                ))
                .toList();
    }

    private GraphNode findNodeInProduct(UUID productId, UUID nodeId) {
        productReferenceService.getRequiredReference(productId);
        return nodeRepository.findByProductIdAndId(productId, nodeId)
                .orElseThrow(() -> new GraphNodeNotFoundException(nodeId));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean matchesNodeSearch(GraphNode node, String term) {
        return node.getLabel().toLowerCase().contains(term)
                || node.getNodeType().name().toLowerCase().contains(term);
    }

    private boolean isActionableOrphan(GraphNode node) {
        Object resolved = metadataFrom(node.getMetadataJson()).get(METADATA_KEY_ORPHAN_RESOLVED);
        return !Boolean.parseBoolean(String.valueOf(resolved));
    }

    private GraphNodeDetail resolveOrphanNode(UUID productId, UUID nodeId, String action) {
        GraphNode node = findNodeInProduct(productId, nodeId);
        Map<String, Object> metadata = metadataFrom(node.getMetadataJson());
        metadata.put(METADATA_KEY_ORPHAN_RESOLVED, true);
        metadata.put(METADATA_KEY_ORPHAN_ACTION, action);
        metadata.put(METADATA_KEY_STATUS, statusFor(action));
        node.replaceMetadataJson(metadataToJson(metadata));
        return nodeMapper.toDetail(nodeRepository.save(node));
    }

    private String normalizedAction(String action) {
        String normalized = action.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case ACTION_ARCHIVE, ACTION_ASSOCIATE, ACTION_LINK, ACTION_MERGE, ACTION_REVIEW -> normalized;
            default -> throw new InvalidGraphOrphanActionException(action);
        };
    }

    private String statusFor(String action) {
        return switch (action) {
            case ACTION_ARCHIVE -> STATUS_ARCHIVED;
            case ACTION_ASSOCIATE -> STATUS_ASSOCIATED;
            case ACTION_LINK -> STATUS_LINKED;
            case ACTION_MERGE -> STATUS_MERGED;
            case ACTION_REVIEW -> STATUS_REVIEWED;
            default -> throw new InvalidGraphOrphanActionException(action);
        };
    }

    private Map<String, Object> metadataFrom(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new java.util.LinkedHashMap<>();
        }
        try {
            return new java.util.LinkedHashMap<>(JSON_MAPPER.readValue(metadataJson, METADATA_TYPE));
        } catch (JsonProcessingException exception) {
            LOGGER.debug("Ignoring invalid graph node metadata JSON while applying graph rule", exception);
            return new java.util.LinkedHashMap<>();
        }
    }

    private String metadataToJson(Map<String, Object> metadata) {
        try {
            return JSON_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            LOGGER.debug("Falling back to empty graph node metadata JSON after serialization failure", exception);
            return EMPTY_JSON;
        }
    }

    private String stringValue(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private String deterministicTextHash(String text) {
        return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)).toString();
    }
}

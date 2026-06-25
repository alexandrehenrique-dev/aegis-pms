package br.com.byop.aegis.knowledgegraph.service;

import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNeighborSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphRelatedSummary;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphNodeException;
import br.com.byop.aegis.knowledgegraph.exception.GraphNodeNotFoundException;
import br.com.byop.aegis.knowledgegraph.mapper.GraphEdgeMapper;
import br.com.byop.aegis.knowledgegraph.mapper.GraphNodeMapper;
import br.com.byop.aegis.knowledgegraph.repository.GraphEdgeRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeGraphService {

    private final ProductReferenceService productReferenceService;
    private final GraphNodeRepository nodeRepository;
    private final GraphEdgeRepository edgeRepository;
    private final GraphNodeMapper nodeMapper;
    private final GraphEdgeMapper edgeMapper;
    private final GraphConsistencyPolicy consistencyPolicy;

    public KnowledgeGraphService(ProductReferenceService productReferenceService, GraphNodeRepository nodeRepository,
                                 GraphEdgeRepository edgeRepository, GraphNodeMapper nodeMapper,
                                 GraphEdgeMapper edgeMapper, GraphConsistencyPolicy consistencyPolicy) {
        this.productReferenceService = productReferenceService;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
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
}

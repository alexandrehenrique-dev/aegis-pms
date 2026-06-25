package br.com.byop.aegis.knowledgegraph.service;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.dto.*;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphNodeException;
import br.com.byop.aegis.knowledgegraph.exception.GraphNodeNotFoundException;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.mapper.GraphEdgeMapper;
import br.com.byop.aegis.knowledgegraph.mapper.GraphNodeMapper;
import br.com.byop.aegis.knowledgegraph.repository.GraphEdgeRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private ProductReferenceService productReferenceService;

    @Mock
    private GraphNodeRepository nodeRepository;

    @Mock
    private GraphEdgeRepository edgeRepository;

    @Mock
    private GraphNodeMapper nodeMapper;

    @Mock
    private GraphEdgeMapper edgeMapper;

    private KnowledgeGraphService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeGraphService(
                productReferenceService,
                nodeRepository,
                edgeRepository,
                nodeMapper,
                edgeMapper,
                new GraphConsistencyPolicy()
        );
    }

    @Test
    void shouldCreateValidNode() {
        ProductReference product = product();
        CreateGraphNodeRequest request = nodeRequest("ARTICLE", "article-1", "Article 1");
        GraphNode unsaved = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "article-1");
        GraphNode saved = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "article-1");
        GraphNodeDetail detail = nodeDetail(saved);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.existsByProductIdAndRefTypeAndRefId(product.productId(), "ARTICLE", "article-1")).thenReturn(false);
        when(nodeMapper.toEntity(request, product.tenantId(), product.productId())).thenReturn(unsaved);
        when(nodeRepository.save(unsaved)).thenReturn(saved);
        when(nodeMapper.toDetail(saved)).thenReturn(detail);

        GraphNodeDetail result = service.createNode(product.productId(), request);

        assertThat(result).isEqualTo(detail);
        verify(nodeRepository).save(unsaved);
    }

    @Test
    void shouldRejectDuplicateNodeByProductRefTypeAndRefId() {
        ProductReference product = product();
        UUID productId = product.productId();
        CreateGraphNodeRequest request = nodeRequest("ARTICLE", "article-1", "Article 1");
        when(productReferenceService.getRequiredReference(productId)).thenReturn(product);
        when(nodeRepository.existsByProductIdAndRefTypeAndRefId(productId, "ARTICLE", "article-1")).thenReturn(true);

        assertThatThrownBy(() -> service.createNode(productId, request))
                .isInstanceOf(DuplicateGraphNodeException.class)
                .hasMessage("Graph node already exists for product " + productId + " and reference ARTICLE/article-1");
    }

    @Test
    void shouldRejectCreateNodeWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        CreateGraphNodeRequest request = nodeRequest("ARTICLE", "article-1", "Article 1");
        when(productReferenceService.getRequiredReference(productId)).thenThrow(new ProductNotFoundException(productId));

        assertThatThrownBy(() -> service.createNode(productId, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldCreateValidEdge() {
        ProductReference product = product();
        GraphNode source = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "source");
        GraphNode target = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "target");
        GraphEdge edge = edge(product.tenantId(), product.productId(), source.getId(), target.getId());
        GraphEdgeDetail detail = edgeDetail(edge);
        CreateGraphEdgeRequest request = edgeRequest(source.getId(), target.getId(), GraphEdgeType.RELATED_TO);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(nodeRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(source.getId(), target.getId(), GraphEdgeType.RELATED_TO))
                .thenReturn(false);
        when(edgeMapper.toEntity(request, product.tenantId(), product.productId())).thenReturn(edge);
        when(edgeRepository.save(edge)).thenReturn(edge);
        when(edgeMapper.toDetail(edge)).thenReturn(detail);

        GraphEdgeDetail result = service.createEdge(product.productId(), request);

        assertThat(result).isEqualTo(detail);
        ArgumentCaptor<GraphEdge> edgeCaptor = ArgumentCaptor.forClass(GraphEdge.class);
        verify(edgeRepository).save(edgeCaptor.capture());
        assertThat(edgeCaptor.getValue()).isEqualTo(edge);
    }

    @Test
    void shouldRejectEdgeWithMissingSource() {
        ProductReference product = product();
        UUID productId = product.productId();
        UUID sourceNodeId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();
        CreateGraphEdgeRequest request = edgeRequest(sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO);
        when(productReferenceService.getRequiredReference(productId)).thenReturn(product);
        when(nodeRepository.findById(sourceNodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createEdge(productId, request))
                .isInstanceOf(GraphNodeNotFoundException.class)
                .hasMessage("Graph node not found: " + sourceNodeId);
    }

    @Test
    void shouldRejectEdgeWithMissingTarget() {
        ProductReference product = product();
        UUID productId = product.productId();
        GraphNode source = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "source");
        UUID targetNodeId = UUID.randomUUID();
        CreateGraphEdgeRequest request = edgeRequest(source.getId(), targetNodeId, GraphEdgeType.RELATED_TO);
        when(productReferenceService.getRequiredReference(productId)).thenReturn(product);
        when(nodeRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(nodeRepository.findById(targetNodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createEdge(productId, request))
                .isInstanceOf(GraphNodeNotFoundException.class)
                .hasMessage("Graph node not found: " + targetNodeId);
    }

    @Test
    void shouldRejectEdgeAcrossTenants() {
        ProductReference product = product();
        UUID productId = product.productId();
        GraphNode source = node(UUID.randomUUID(), product.productId(), GraphNodeType.ARTICLE, "source");
        GraphNode target = node(UUID.randomUUID(), product.productId(), GraphNodeType.TOPIC, "target");
        CreateGraphEdgeRequest request = edgeRequest(source.getId(), target.getId(), GraphEdgeType.RELATED_TO);
        when(productReferenceService.getRequiredReference(productId)).thenReturn(product);
        when(nodeRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(nodeRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.createEdge(productId, request))
                .isInstanceOf(InvalidGraphEdgeException.class)
                .hasMessage("Graph edge nodes must belong to the same tenant");
    }

    @Test
    void shouldRejectEdgeAcrossProducts() {
        ProductReference product = product();
        UUID productId = product.productId();
        UUID tenantId = product.tenantId();
        GraphNode source = node(tenantId, product.productId(), GraphNodeType.ARTICLE, "source");
        GraphNode target = node(tenantId, UUID.randomUUID(), GraphNodeType.TOPIC, "target");
        CreateGraphEdgeRequest request = edgeRequest(source.getId(), target.getId(), GraphEdgeType.RELATED_TO);
        when(productReferenceService.getRequiredReference(productId)).thenReturn(product);
        when(nodeRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(nodeRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.createEdge(productId, request))
                .isInstanceOf(InvalidGraphEdgeException.class)
                .hasMessage("Graph edge nodes must belong to the same product");
    }

    @Test
    void shouldRejectDuplicateEdge() {
        ProductReference product = product();
        UUID productId = product.productId();
        GraphNode source = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "source");
        GraphNode target = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "target");
        CreateGraphEdgeRequest request = edgeRequest(source.getId(), target.getId(), GraphEdgeType.RELATED_TO);
        when(productReferenceService.getRequiredReference(productId)).thenReturn(product);
        when(nodeRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(nodeRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(source.getId(), target.getId(), GraphEdgeType.RELATED_TO))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createEdge(productId, request))
                .isInstanceOf(DuplicateGraphEdgeException.class)
                .hasMessage("Graph edge already exists: " + source.getId() + " -> " + target.getId() + " (RELATED_TO)");
    }

    @Test
    void shouldListNeighbors() {
        ProductReference product = product();
        GraphNode source = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "source");
        GraphNode target = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "target");
        GraphEdge edge = edge(product.tenantId(), product.productId(), source.getId(), target.getId());
        GraphNodeSummary targetSummary = nodeSummary(target);
        GraphEdgeSummary edgeSummary = edgeSummary(edge);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findByProductIdAndId(product.productId(), source.getId())).thenReturn(Optional.of(source));
        when(edgeRepository.findAllByProductIdAndSourceNodeId(product.productId(), source.getId())).thenReturn(List.of(edge));
        when(nodeRepository.findByProductIdAndId(product.productId(), target.getId())).thenReturn(Optional.of(target));
        when(nodeMapper.toSummary(target)).thenReturn(targetSummary);
        when(edgeMapper.toSummary(edge)).thenReturn(edgeSummary);

        List<GraphNeighborSummary> result = service.findNeighbors(product.productId(), source.getId());

        assertThat(result).containsExactly(new GraphNeighborSummary(targetSummary, edgeSummary));
    }

    @Test
    void shouldListRelated() {
        ProductReference product = product();
        GraphNode source = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "source");
        GraphNode target = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "target");
        GraphEdge edge = edge(product.tenantId(), product.productId(), source.getId(), target.getId());
        GraphNodeSummary sourceSummary = nodeSummary(source);
        GraphEdgeSummary edgeSummary = edgeSummary(edge);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findByProductIdAndId(product.productId(), target.getId())).thenReturn(Optional.of(target));
        when(edgeRepository.findAllByProductIdAndTargetNodeId(product.productId(), target.getId())).thenReturn(List.of(edge));
        when(nodeRepository.findByProductIdAndId(product.productId(), source.getId())).thenReturn(Optional.of(source));
        when(nodeMapper.toSummary(source)).thenReturn(sourceSummary);
        when(edgeMapper.toSummary(edge)).thenReturn(edgeSummary);

        List<GraphRelatedSummary> result = service.findRelated(product.productId(), target.getId());

        assertThat(result).containsExactly(new GraphRelatedSummary(sourceSummary, edgeSummary));
    }

    @Test
    void shouldSearchNodesWithQuery() {
        ProductReference product = product();
        GraphNode node = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "spring");
        GraphNodeSummary summary = nodeSummary(node);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findAllByProductId(product.productId())).thenReturn(List.of(node));
        when(nodeMapper.toSummary(node)).thenReturn(summary);

        List<GraphNodeSummary> result = service.searchNodes(product.productId(), " spring ");

        assertThat(result).containsExactly(summary);
    }

    @Test
    void shouldSearchNodesByNodeTypeIgnoringCase() {
        ProductReference product = product();
        GraphNode article = node(product.tenantId(), product.productId(), GraphNodeType.ARTICLE, "article");
        GraphNode topic = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "semantic");
        GraphNodeSummary summary = nodeSummary(topic);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findAllByProductId(product.productId())).thenReturn(List.of(article, topic));
        when(nodeMapper.toSummary(topic)).thenReturn(summary);

        List<GraphNodeSummary> result = service.searchNodes(product.productId(), "tOpIc");

        assertThat(result).containsExactly(summary);
    }

    @Test
    void shouldSearchNodesWithoutQuery() {
        ProductReference product = product();
        GraphNode node = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "spring");
        GraphNodeSummary summary = nodeSummary(node);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findAllByProductId(product.productId())).thenReturn(List.of(node));
        when(nodeMapper.toSummary(node)).thenReturn(summary);

        List<GraphNodeSummary> result = service.searchNodes(product.productId(), " ");

        assertThat(result).containsExactly(summary);
    }

    @Test
    void shouldSearchNodesWithoutQueryWhenQueryIsNull() {
        ProductReference product = product();
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findAllByProductId(product.productId())).thenReturn(List.of());

        List<GraphNodeSummary> result = service.searchNodes(product.productId(), null);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindNodeInProduct() {
        ProductReference product = product();
        GraphNode node = node(product.tenantId(), product.productId(), GraphNodeType.TOPIC, "spring");
        GraphNodeDetail detail = nodeDetail(node);
        when(productReferenceService.getRequiredReference(product.productId())).thenReturn(product);
        when(nodeRepository.findByProductIdAndId(product.productId(), node.getId())).thenReturn(Optional.of(node));
        when(nodeMapper.toDetail(node)).thenReturn(detail);

        GraphNodeDetail result = service.findNode(product.productId(), node.getId());

        assertThat(result).isEqualTo(detail);
    }

    @Test
    void shouldRejectFindNodeWhenNodeDoesNotExistInProduct() {
        ProductReference product = product();
        UUID productId = product.productId();
        UUID nodeId = UUID.randomUUID();
        when(productReferenceService.getRequiredReference(productId)).thenReturn(product);
        when(nodeRepository.findByProductIdAndId(productId, nodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findNode(productId, nodeId))
                .isInstanceOf(GraphNodeNotFoundException.class)
                .hasMessage("Graph node not found: " + nodeId);
    }

    @Test
    void shouldRejectMissingProductWhenSearching() {
        UUID productId = UUID.randomUUID();
        when(productReferenceService.getRequiredReference(productId)).thenThrow(new ProductNotFoundException(productId));

        assertThatThrownBy(() -> service.searchNodes(productId, null))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    private ProductReference product() {
        return new ProductReference(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("11111111-1111-1111-1111-111111111111")
        );
    }

    private CreateGraphNodeRequest nodeRequest(String refType, String refId, String label) {
        return new CreateGraphNodeRequest(GraphNodeType.ARTICLE, refType, refId, label, null, null);
    }

    private CreateGraphEdgeRequest edgeRequest(UUID sourceNodeId, UUID targetNodeId, GraphEdgeType edgeType) {
        return new CreateGraphEdgeRequest(sourceNodeId, targetNodeId, edgeType, BigDecimal.ONE, "{}");
    }

    private GraphNode node(UUID tenantId, UUID productId, GraphNodeType nodeType, String refId) {
        GraphNode node = new GraphNode(new GraphNode.Creation(
                tenantId,
                productId,
                nodeType,
                nodeType.name(),
                refId,
                "Label " + refId,
                refId,
                "{}"
        ));
        ReflectionTestUtils.setField(node, "id", UUID.nameUUIDFromBytes((productId + refId).getBytes()));
        ReflectionTestUtils.setField(node, "createdAt", OffsetDateTime.parse("2026-06-25T17:00:00-03:00"));
        ReflectionTestUtils.setField(node, "updatedAt", OffsetDateTime.parse("2026-06-25T17:10:00-03:00"));
        return node;
    }

    private GraphEdge edge(UUID tenantId, UUID productId, UUID sourceNodeId, UUID targetNodeId) {
        GraphEdge edge = new GraphEdge(tenantId, productId, sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO);
        ReflectionTestUtils.setField(edge, "id", UUID.nameUUIDFromBytes((sourceNodeId.toString() + targetNodeId).getBytes()));
        ReflectionTestUtils.setField(edge, "createdAt", OffsetDateTime.parse("2026-06-25T17:20:00-03:00"));
        ReflectionTestUtils.setField(edge, "updatedAt", OffsetDateTime.parse("2026-06-25T17:30:00-03:00"));
        return edge;
    }

    private GraphNodeSummary nodeSummary(GraphNode node) {
        return new GraphNodeSummary(
                node.getId(),
                node.getTenantId(),
                node.getProductId(),
                node.getNodeType(),
                node.getRefType(),
                node.getRefId(),
                node.getLabel(),
                node.getSlug(),
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }

    private GraphNodeDetail nodeDetail(GraphNode node) {
        return new GraphNodeDetail(
                node.getId(),
                node.getTenantId(),
                node.getProductId(),
                node.getNodeType(),
                node.getRefType(),
                node.getRefId(),
                node.getLabel(),
                node.getSlug(),
                node.getMetadataJson(),
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }

    private GraphEdgeSummary edgeSummary(GraphEdge edge) {
        return new GraphEdgeSummary(
                edge.getId(),
                edge.getTenantId(),
                edge.getProductId(),
                edge.getSourceNodeId(),
                edge.getTargetNodeId(),
                edge.getEdgeType(),
                edge.getWeight(),
                edge.getCreatedAt(),
                edge.getUpdatedAt()
        );
    }

    private GraphEdgeDetail edgeDetail(GraphEdge edge) {
        return new GraphEdgeDetail(
                edge.getId(),
                edge.getTenantId(),
                edge.getProductId(),
                edge.getSourceNodeId(),
                edge.getTargetNodeId(),
                edge.getEdgeType(),
                edge.getWeight(),
                edge.getMetadataJson(),
                edge.getCreatedAt(),
                edge.getUpdatedAt()
        );
    }
}

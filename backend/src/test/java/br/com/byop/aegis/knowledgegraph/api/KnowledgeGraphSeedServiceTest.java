package br.com.byop.aegis.knowledgegraph.api;

import br.com.byop.aegis.knowledgegraph.domain.GraphEdge;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.repository.GraphEdgeRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphSeedServiceTest {

    @Mock
    private ProductReferenceService productReferenceService;

    @Mock
    private GraphNodeRepository nodeRepository;

    @Mock
    private GraphEdgeRepository edgeRepository;

    @InjectMocks
    private KnowledgeGraphSeedService service;

    @Test
    void shouldCreateNodeWhenReferenceIsMissing() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        when(nodeRepository.findByProductIdAndRefTypeAndRefId(productId, "ARTICLE", "spring-boot"))
                .thenReturn(Optional.empty());
        when(productReferenceService.getRequiredReference(productId)).thenReturn(new ProductReference(productId, tenantId));
        when(nodeRepository.save(any(GraphNode.class))).thenAnswer(invocation -> {
            GraphNode node = invocation.getArgument(0);
            ReflectionTestUtils.setField(node, "id", nodeId);
            return node;
        });

        GraphSeedNodeReference reference = service.ensureNode(new GraphSeedNodeCommand(
                productId,
                "ARTICLE",
                "ARTICLE",
                "spring-boot",
                "Artigo Spring Boot",
                "spring-boot",
                "",
                10.0,
                20.0
        ));

        assertThat(reference.id()).isEqualTo(nodeId);
        assertThat(reference.label()).isEqualTo("Artigo Spring Boot");
    }

    @Test
    void shouldUpdateExistingNodeMetadataAndPosition() {
        UUID productId = UUID.randomUUID();
        GraphNode node = new GraphNode(new GraphNode.Creation(
                UUID.randomUUID(),
                productId,
                GraphNodeType.ARTICLE,
                "ARTICLE",
                "jpa",
                "Artigo JPA",
                "jpa",
                "{}"
        ));
        UUID nodeId = UUID.randomUUID();
        ReflectionTestUtils.setField(node, "id", nodeId);
        when(nodeRepository.findByProductIdAndRefTypeAndRefId(productId, "ARTICLE", "jpa"))
                .thenReturn(Optional.of(node));
        when(nodeRepository.save(node)).thenReturn(node);

        GraphSeedNodeReference reference = service.ensureNode(new GraphSeedNodeCommand(
                productId,
                "ARTICLE",
                "ARTICLE",
                "jpa",
                "Artigo JPA",
                "jpa",
                "{\"seed\":\"sprint-22\"}",
                30.0,
                40.0
        ));

        assertThat(reference.id()).isEqualTo(nodeId);
        assertThat(node.getMetadataJson()).isEqualTo("{\"seed\":\"sprint-22\"}");
        assertThat(node.getX()).isEqualTo(30.0);
        assertThat(node.getY()).isEqualTo(40.0);
    }

    @Test
    void shouldNormalizeNullMetadataToEmptyJson() {
        UUID productId = UUID.randomUUID();
        GraphNode node = new GraphNode(new GraphNode.Creation(
                UUID.randomUUID(),
                productId,
                GraphNodeType.TOPIC,
                "TOPIC",
                "java",
                "Topico Java",
                "java",
                "{}"
        ));
        when(nodeRepository.findByProductIdAndRefTypeAndRefId(productId, "TOPIC", "java"))
                .thenReturn(Optional.of(node));
        when(nodeRepository.save(node)).thenReturn(node);

        service.ensureNode(new GraphSeedNodeCommand(
                productId,
                "TOPIC",
                "TOPIC",
                "java",
                "Topico Java",
                "java",
                null,
                0.0,
                0.0
        ));

        assertThat(node.getMetadataJson()).isEqualTo("{}");
    }

    @Test
    void shouldCreateEdgeWhenMissing() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(
                sourceId,
                targetId,
                GraphEdgeType.RELATED_TO
        )).thenReturn(false);
        when(productReferenceService.getRequiredReference(productId)).thenReturn(new ProductReference(productId, tenantId));

        service.ensureEdge(new GraphSeedEdgeCommand(productId, sourceId, targetId, "RELATED_TO"));

        verify(edgeRepository).save(org.mockito.ArgumentMatchers.argThat(edge ->
                edge.getTenantId().equals(tenantId)
                        && edge.getProductId().equals(productId)
                        && edge.getSourceNodeId().equals(sourceId)
                        && edge.getTargetNodeId().equals(targetId)
                        && edge.getEdgeType() == GraphEdgeType.RELATED_TO
        ));
    }

    @Test
    void shouldSkipEdgeWhenItAlreadyExists() {
        UUID productId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(
                sourceId,
                targetId,
                GraphEdgeType.CONTAINS
        )).thenReturn(true);

        service.ensureEdge(new GraphSeedEdgeCommand(productId, sourceId, targetId, "CONTAINS"));

        verify(edgeRepository, never()).save(any(GraphEdge.class));
    }
}

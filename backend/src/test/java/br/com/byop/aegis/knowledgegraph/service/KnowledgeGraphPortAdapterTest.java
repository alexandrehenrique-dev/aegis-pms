package br.com.byop.aegis.knowledgegraph.service;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.exception.DuplicateGraphEdgeException;
import br.com.byop.aegis.knowledgegraph.repository.GraphEdgeRepository;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphPortAdapterTest {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-06-26T10:00:00-03:00");

    @Mock
    private GraphNodeRepository nodeRepository;

    @Mock
    private GraphEdgeRepository edgeRepository;

    @Mock
    private KnowledgeGraphService knowledgeGraphService;

    @InjectMocks
    private KnowledgeGraphPortAdapter adapter;

    @Test
    void shouldReturnTrueWhenNodeExists() {
        UUID productId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        when(nodeRepository.findByProductIdAndId(productId, nodeId)).thenReturn(Optional.of(node(nodeId)));

        assertThat(adapter.nodeExists(productId, nodeId)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNodeDoesNotExist() {
        UUID productId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        when(nodeRepository.findByProductIdAndId(productId, nodeId)).thenReturn(Optional.empty());

        assertThat(adapter.nodeExists(productId, nodeId)).isFalse();
    }

    @Test
    void shouldReuseExistingNodeWhenAlreadyLinkedToReference() {
        UUID productId = UUID.randomUUID();
        UUID existingNodeId = UUID.randomUUID();
        when(nodeRepository.findByProductIdAndRefTypeAndRefId(productId, "CONTENT", "content-1"))
                .thenReturn(Optional.of(node(existingNodeId)));

        UUID result = adapter.ensureContentNode(productId, "content-1", "Artigo");

        assertThat(result).isEqualTo(existingNodeId);
        verify(knowledgeGraphService, never()).createNode(any(), any());
    }

    @Test
    void shouldCreateNodeWhenReferenceHasNoneYet() {
        UUID productId = UUID.randomUUID();
        UUID createdNodeId = UUID.randomUUID();
        when(nodeRepository.findByProductIdAndRefTypeAndRefId(productId, "CONTENT", "content-1"))
                .thenReturn(Optional.empty());
        when(knowledgeGraphService.createNode(eq(productId), any(CreateGraphNodeRequest.class)))
                .thenReturn(nodeDetail(createdNodeId));

        UUID result = adapter.ensureContentNode(productId, "content-1", "Artigo");

        assertThat(result).isEqualTo(createdNodeId);
        ArgumentCaptor<CreateGraphNodeRequest> captor = ArgumentCaptor.forClass(CreateGraphNodeRequest.class);
        verify(knowledgeGraphService).createNode(eq(productId), captor.capture());
        assertThat(captor.getValue().nodeType()).isEqualTo(GraphNodeType.CONTENT);
        assertThat(captor.getValue().refType()).isEqualTo("CONTENT");
        assertThat(captor.getValue().refId()).isEqualTo("content-1");
        assertThat(captor.getValue().label()).isEqualTo("Artigo");
    }

    @Test
    void shouldSkipEdgeCreationWhenEdgeAlreadyExists() {
        UUID productId = UUID.randomUUID();
        UUID sourceNodeId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();
        when(edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO))
                .thenReturn(true);

        adapter.ensureRelatedToEdge(productId, sourceNodeId, targetNodeId);

        verify(knowledgeGraphService, never()).createEdge(any(), any());
    }

    @Test
    void shouldCreateEdgeWhenAbsent() {
        UUID productId = UUID.randomUUID();
        UUID sourceNodeId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();
        when(edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO))
                .thenReturn(false);
        when(knowledgeGraphService.createEdge(eq(productId), any(CreateGraphEdgeRequest.class)))
                .thenReturn(edgeDetail());

        adapter.ensureRelatedToEdge(productId, sourceNodeId, targetNodeId);

        ArgumentCaptor<CreateGraphEdgeRequest> captor = ArgumentCaptor.forClass(CreateGraphEdgeRequest.class);
        verify(knowledgeGraphService).createEdge(eq(productId), captor.capture());
        assertThat(captor.getValue().sourceNodeId()).isEqualTo(sourceNodeId);
        assertThat(captor.getValue().targetNodeId()).isEqualTo(targetNodeId);
        assertThat(captor.getValue().edgeType()).isEqualTo(GraphEdgeType.RELATED_TO);
    }

    @Test
    void shouldIgnoreConcurrentDuplicateEdgeCreation() {
        UUID productId = UUID.randomUUID();
        UUID sourceNodeId = UUID.randomUUID();
        UUID targetNodeId = UUID.randomUUID();
        when(edgeRepository.existsBySourceNodeIdAndTargetNodeIdAndEdgeType(sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO))
                .thenReturn(false);
        when(knowledgeGraphService.createEdge(eq(productId), any(CreateGraphEdgeRequest.class)))
                .thenThrow(new DuplicateGraphEdgeException(sourceNodeId, targetNodeId, GraphEdgeType.RELATED_TO));

        assertThatCode(() -> adapter.ensureRelatedToEdge(productId, sourceNodeId, targetNodeId)).doesNotThrowAnyException();
    }

    private GraphNode node(UUID id) {
        GraphNode node = new GraphNode(new GraphNode.Creation(
                UUID.randomUUID(), UUID.randomUUID(), GraphNodeType.CONTENT, "CONTENT", "content-1", "Artigo", "artigo", "{}"
        ));
        ReflectionTestUtils.setField(node, "id", id);
        return node;
    }

    private GraphNodeDetail nodeDetail(UUID id) {
        return new GraphNodeDetail(id, UUID.randomUUID(), UUID.randomUUID(), GraphNodeType.CONTENT,
                "CONTENT", "content-1", "Artigo", "artigo", "{}", FIXED_TIME, FIXED_TIME);
    }

    private GraphEdgeDetail edgeDetail() {
        return new GraphEdgeDetail(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), GraphEdgeType.RELATED_TO, null, "{}", FIXED_TIME, FIXED_TIME);
    }
}

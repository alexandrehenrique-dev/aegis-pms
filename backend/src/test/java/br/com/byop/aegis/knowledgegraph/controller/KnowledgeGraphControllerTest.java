package br.com.byop.aegis.knowledgegraph.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.knowledgegraph.domain.GraphEdgeType;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphInsightReviewSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNeighborSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodePreview;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphRelatedSummary;
import br.com.byop.aegis.knowledgegraph.exception.GraphNodeNotFoundException;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphNodeException;
import br.com.byop.aegis.knowledgegraph.exception.InvalidGraphOrphanActionException;
import br.com.byop.aegis.knowledgegraph.service.KnowledgeGraphService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KnowledgeGraphController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        KnowledgeGraphExceptionHandler.class
})
class KnowledgeGraphControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NODE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TARGET_NODE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID EDGE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-25T10:00:00-03:00");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-06-25T10:10:00-03:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeGraphService knowledgeGraphService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldCreateNode() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.createNode(any(), any())).thenReturn(nodeDetail());

        mockMvc.perform(post("/api/v1/products/{productId}/graph/nodes", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(validNodeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$.nodeType").value("ARTICLE"))
                .andExpect(jsonPath("$.metadataJson").value("{\"lang\":\"pt-BR\"}"));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldSearchNodesWithoutQuery() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.searchNodes(PRODUCT_ID, null)).thenReturn(List.of(nodeSummary()));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$[0].label").value("Article 1"));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldSearchNodesWithQuery() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.searchNodes(PRODUCT_ID, "Article")).thenReturn(List.of(nodeSummary()));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes", PRODUCT_ID)
                        .queryParam("q", "Article")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].refId").value("article-1"));
    }

    @Test
    void shouldFindNodeById() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.findNode(PRODUCT_ID, NODE_ID)).thenReturn(nodeDetail());

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes/{nodeId}", PRODUCT_ID, NODE_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()));
    }

    @Test
    void shouldUpdateNodePosition() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.updatePosition(any(), any(), any())).thenReturn(positionedNodeDetail());

        mockMvc.perform(patch("/api/v1/products/{productId}/graph/nodes/{nodeId}/position", PRODUCT_ID, NODE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "x": 120.5,
                                  "y": 340.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x").value(120.5))
                .andExpect(jsonPath("$.y").value(340.0));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldRejectInvalidPositionRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/products/{productId}/graph/nodes/{nodeId}/position", PRODUCT_ID, NODE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "x": null,
                                  "y": 340.0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindOrphans() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.findOrphans(PRODUCT_ID)).thenReturn(List.of(nodeSummary()));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/orphans", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$[0].type").value("ARTICLE"));
    }

    @Test
    void shouldResolveOrphan() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.resolveOrphan(any(), any(), any())).thenReturn(nodeDetail());

        mockMvc.perform(post("/api/v1/products/{productId}/graph/orphans/{nodeId}/resolve", PRODUCT_ID, NODE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("{\"action\":\"Revisar\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NODE_ID.toString()));
    }

    @Test
    void shouldResolveOrphans() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.resolveOrphans(any(), any())).thenReturn(List.of(nodeDetail()));

        mockMvc.perform(post("/api/v1/products/{productId}/graph/orphans/resolve", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": ["33333333-3333-3333-3333-333333333333"],
                                  "action": "Revisar"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NODE_ID.toString()));
    }

    @Test
    void shouldPreviewNode() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.previewNode(PRODUCT_ID, NODE_ID)).thenReturn(new GraphNodePreview(
                NODE_ID,
                "Article 1",
                "ARTICLE",
                "Resumo",
                "beginner",
                "cover.png"
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes/{nodeId}/preview", PRODUCT_ID, NODE_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Resumo"))
                .andExpect(jsonPath("$.difficulty").value("beginner"))
                .andExpect(jsonPath("$.thumbnail").value("cover.png"));
    }

    @Test
    void shouldReviewInsight() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.reviewInsight(any(), any())).thenReturn(new GraphInsightReviewSummary(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "Conectar A e B",
                true
        ));

        mockMvc.perform(post("/api/v1/products/{productId}/graph/insights/review", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("{\"text\":\"Conectar A e B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewed").value(true));
    }

    @Test
    void shouldCreateEdge() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.createEdge(any(), any())).thenReturn(edgeDetail());

        mockMvc.perform(post("/api/v1/products/{productId}/graph/edges", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(validEdgeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(EDGE_ID.toString()))
                .andExpect(jsonPath("$.edgeType").value("RELATED_TO"))
                .andExpect(jsonPath("$.weight").value(1.5));
    }

    @Test
    void shouldListEdges() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.listEdges(PRODUCT_ID)).thenReturn(List.of(edgeSummary()));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/edges", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceNodeId").value(NODE_ID.toString()))
                .andExpect(jsonPath("$[0].targetNodeId").value(TARGET_NODE_ID.toString()))
                .andExpect(jsonPath("$[0].edgeType").value("RELATED_TO"));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldFindNeighbors() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.findNeighbors(PRODUCT_ID, NODE_ID))
                .thenReturn(List.of(new GraphNeighborSummary(targetNodeSummary(), edgeSummary())));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes/{nodeId}/neighbors", PRODUCT_ID, NODE_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].node.id").value(TARGET_NODE_ID.toString()))
                .andExpect(jsonPath("$[0].edge.edgeType").value("RELATED_TO"));
    }

    @Test
    void shouldFindRelated() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.findRelated(PRODUCT_ID, TARGET_NODE_ID))
                .thenReturn(List.of(new GraphRelatedSummary(nodeSummary(), edgeSummary())));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes/{nodeId}/related", PRODUCT_ID, TARGET_NODE_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].node.id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$[0].edge.targetNodeId").value(TARGET_NODE_ID.toString()));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/graph/nodes", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeType": null,
                                  "refType": "",
                                  "refId": "",
                                  "label": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenProductIsOutsideAuthenticatedScope() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID))
                .when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldMapServiceExceptionToClearError() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(knowledgeGraphService.findNode(PRODUCT_ID, NODE_ID)).thenThrow(new GraphNodeNotFoundException(NODE_ID));

        mockMvc.perform(get("/api/v1/products/{productId}/graph/nodes/{nodeId}", PRODUCT_ID, NODE_ID)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GRAPH_NODE_NOT_FOUND"));
    }

    @Test
    void shouldRequireKnowledgeGraphModule() {
        RequireModule requireModule = KnowledgeGraphController.class.getAnnotation(RequireModule.class);

        assertThat(requireModule).isNotNull();
        assertThat(requireModule.value()).isEqualTo(ModuleKey.KNOWLEDGE_GRAPH);
    }

    @Test
    void shouldReturnKnowledgeGraphErrorCodesFromExceptionHandler() {
        KnowledgeGraphExceptionHandler handler = new KnowledgeGraphExceptionHandler();

        assertThat(handler.handleDuplicateGraphNode().error()).isEqualTo("GRAPH_NODE_ALREADY_EXISTS");
        assertThat(handler.handleDuplicateGraphEdge().error()).isEqualTo("GRAPH_EDGE_ALREADY_EXISTS");
        assertThat(handler.handleGraphNodeNotFound().error()).isEqualTo("GRAPH_NODE_NOT_FOUND");
        assertThat(handler.handleInvalidGraphNode().error()).isEqualTo("INVALID_GRAPH_NODE");
        assertThat(handler.handleInvalidGraphEdge().error()).isEqualTo("INVALID_GRAPH_EDGE");
        assertThat(handler.handleInvalidGraphOrphanAction().error()).isEqualTo("INVALID_GRAPH_ORPHAN_ACTION");
        assertThat(new InvalidGraphNodeException("invalid graph node")).hasMessage("invalid graph node");
        assertThat(new InvalidGraphOrphanActionException("Ignorar")).hasMessage("Invalid graph orphan action: Ignorar");
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(
                "user-1",
                "user@example.com",
                "user",
                "User",
                Set.of("ROLE_EDITOR")
        );
    }

    private String validNodeJson() {
        return """
                {
                  "nodeType": "ARTICLE",
                  "refType": "ARTICLE",
                  "refId": "article-1",
                  "label": "Article 1",
                  "slug": "article-1",
                  "metadataJson": "{\\"lang\\":\\"pt-BR\\"}"
                }
                """;
    }

    private String validEdgeJson() {
        return """
                {
                  "sourceNodeId": "33333333-3333-3333-3333-333333333333",
                  "targetNodeId": "44444444-4444-4444-4444-444444444444",
                  "edgeType": "RELATED_TO",
                  "weight": 1.5,
                  "metadataJson": "{\\"origin\\":\\"manual\\"}"
                }
                """;
    }

    private GraphNodeDetail nodeDetail() {
        return new GraphNodeDetail(
                NODE_ID,
                TENANT_ID,
                PRODUCT_ID,
                GraphNodeType.ARTICLE,
                "ARTICLE",
                "article-1",
                "Article 1",
                "article-1",
                "{\"lang\":\"pt-BR\"}",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private GraphNodeDetail positionedNodeDetail() {
        return new GraphNodeDetail(
                NODE_ID,
                TENANT_ID,
                PRODUCT_ID,
                GraphNodeType.ARTICLE,
                "ARTICLE",
                "article-1",
                "Article 1",
                "article-1",
                "ARTICLE",
                "ativo",
                120.5,
                340.0,
                List.of(),
                "{\"lang\":\"pt-BR\"}",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private GraphNodeSummary nodeSummary() {
        return new GraphNodeSummary(
                NODE_ID,
                TENANT_ID,
                PRODUCT_ID,
                GraphNodeType.ARTICLE,
                "ARTICLE",
                "article-1",
                "Article 1",
                "article-1",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private GraphNodeSummary targetNodeSummary() {
        return new GraphNodeSummary(
                TARGET_NODE_ID,
                TENANT_ID,
                PRODUCT_ID,
                GraphNodeType.TOPIC,
                "TOPIC",
                "topic-1",
                "Topic 1",
                "topic-1",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private GraphEdgeDetail edgeDetail() {
        return new GraphEdgeDetail(
                EDGE_ID,
                TENANT_ID,
                PRODUCT_ID,
                NODE_ID,
                TARGET_NODE_ID,
                GraphEdgeType.RELATED_TO,
                BigDecimal.valueOf(1.5),
                "{\"origin\":\"manual\"}",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private GraphEdgeSummary edgeSummary() {
        return new GraphEdgeSummary(
                EDGE_ID,
                TENANT_ID,
                PRODUCT_ID,
                NODE_ID,
                TARGET_NODE_ID,
                GraphEdgeType.RELATED_TO,
                BigDecimal.valueOf(1.5),
                CREATED_AT,
                UPDATED_AT
        );
    }
}

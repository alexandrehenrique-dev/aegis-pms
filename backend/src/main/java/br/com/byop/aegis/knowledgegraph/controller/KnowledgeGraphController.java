package br.com.byop.aegis.knowledgegraph.controller;

import br.com.byop.aegis.knowledgegraph.contract.CreateGraphEdgeRequest;
import br.com.byop.aegis.knowledgegraph.contract.CreateGraphNodeRequest;
import br.com.byop.aegis.knowledgegraph.contract.ResolveGraphOrphanRequest;
import br.com.byop.aegis.knowledgegraph.contract.ResolveGraphOrphansRequest;
import br.com.byop.aegis.knowledgegraph.contract.ReviewGraphInsightRequest;
import br.com.byop.aegis.knowledgegraph.contract.UpdateGraphNodePositionRequest;
import br.com.byop.aegis.knowledgegraph.dto.GraphEdgeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphInsightReviewSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNeighborSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeDetail;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodePreview;
import br.com.byop.aegis.knowledgegraph.dto.GraphNodeSummary;
import br.com.byop.aegis.knowledgegraph.dto.GraphRelatedSummary;
import br.com.byop.aegis.knowledgegraph.service.KnowledgeGraphService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequireModule(ModuleKey.KNOWLEDGE_GRAPH)
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService,
                                    ProductAccessPort productAccessPort,
                                    AuthenticatedUserProvider authenticatedUserProvider) {
        this.knowledgeGraphService = knowledgeGraphService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/api/v1/products/{productId}/graph/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public GraphNodeDetail createNode(@PathVariable("productId") UUID productId,
                                      @Valid @RequestBody CreateGraphNodeRequest request,
                                      Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.createNode(productId, request);
    }

    @GetMapping("/api/v1/products/{productId}/graph/nodes")
    public List<GraphNodeSummary> searchNodes(@PathVariable("productId") UUID productId,
                                              @RequestParam(name = "q", required = false) String q,
                                              Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.searchNodes(productId, q);
    }

    @GetMapping("/api/v1/products/{productId}/graph/nodes/{nodeId}")
    public GraphNodeDetail findNode(@PathVariable("productId") UUID productId,
                                    @PathVariable("nodeId") UUID nodeId,
                                    Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.findNode(productId, nodeId);
    }

    @PatchMapping("/api/v1/products/{productId}/graph/nodes/{nodeId}/position")
    public GraphNodeDetail updateNodePosition(@PathVariable("productId") UUID productId,
                                              @PathVariable("nodeId") UUID nodeId,
                                              @Valid @RequestBody UpdateGraphNodePositionRequest request,
                                              Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.updatePosition(productId, nodeId, request);
    }

    @GetMapping("/api/v1/products/{productId}/graph/orphans")
    public List<GraphNodeSummary> findOrphans(@PathVariable("productId") UUID productId,
                                              Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.findOrphans(productId);
    }

    @PostMapping("/api/v1/products/{productId}/graph/orphans/{nodeId}/resolve")
    public GraphNodeDetail resolveOrphan(@PathVariable("productId") UUID productId,
                                         @PathVariable("nodeId") UUID nodeId,
                                         @Valid @RequestBody ResolveGraphOrphanRequest request,
                                         Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.resolveOrphan(productId, nodeId, request);
    }

    @PostMapping("/api/v1/products/{productId}/graph/orphans/resolve")
    public List<GraphNodeDetail> resolveOrphans(@PathVariable("productId") UUID productId,
                                                @Valid @RequestBody ResolveGraphOrphansRequest request,
                                                Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.resolveOrphans(productId, request);
    }

    @GetMapping("/api/v1/products/{productId}/graph/nodes/{nodeId}/preview")
    public GraphNodePreview previewNode(@PathVariable("productId") UUID productId,
                                        @PathVariable("nodeId") UUID nodeId,
                                        Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.previewNode(productId, nodeId);
    }

    @PostMapping("/api/v1/products/{productId}/graph/insights/review")
    public GraphInsightReviewSummary reviewInsight(@PathVariable("productId") UUID productId,
                                                   @Valid @RequestBody ReviewGraphInsightRequest request,
                                                   Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.reviewInsight(productId, request);
    }

    @PostMapping("/api/v1/products/{productId}/graph/edges")
    @ResponseStatus(HttpStatus.CREATED)
    public GraphEdgeDetail createEdge(@PathVariable("productId") UUID productId,
                                      @Valid @RequestBody CreateGraphEdgeRequest request,
                                      Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.createEdge(productId, request);
    }

    @GetMapping("/api/v1/products/{productId}/graph/nodes/{nodeId}/neighbors")
    public List<GraphNeighborSummary> findNeighbors(@PathVariable("productId") UUID productId,
                                                    @PathVariable("nodeId") UUID nodeId,
                                                    Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.findNeighbors(productId, nodeId);
    }

    @GetMapping("/api/v1/products/{productId}/graph/nodes/{nodeId}/related")
    public List<GraphRelatedSummary> findRelated(@PathVariable("productId") UUID productId,
                                                 @PathVariable("nodeId") UUID nodeId,
                                                 Authentication authentication) {
        assertProductAccess(authentication, productId);
        return knowledgeGraphService.findRelated(productId, nodeId);
    }

    private void assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
    }
}

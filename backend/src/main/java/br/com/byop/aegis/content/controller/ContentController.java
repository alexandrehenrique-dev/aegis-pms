package br.com.byop.aegis.content.controller;

import br.com.byop.aegis.content.contract.ContentTransitionRequest;
import br.com.byop.aegis.content.contract.CreateContentRequest;
import br.com.byop.aegis.content.contract.PublishContentRequest;
import br.com.byop.aegis.content.contract.UpdateContentRequest;
import br.com.byop.aegis.content.dto.ContentSummary;
import br.com.byop.aegis.content.dto.ContentVersionSummary;
import br.com.byop.aegis.content.dto.WorkflowItemSummary;
import br.com.byop.aegis.content.service.ContentService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequireModule(ModuleKey.CONTENT)
public class ContentController {

    private final ContentService contentService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ContentController(ContentService contentService, ProductAccessPort productAccessPort,
                             AuthenticatedUserProvider authenticatedUserProvider) {
        this.contentService = contentService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/api/v1/products/{productId}/content")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentSummary createContent(@PathVariable("productId") UUID productId,
                                        @Valid @RequestBody CreateContentRequest request,
                                        Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return contentService.createContent(productId, request, caller);
    }

    @GetMapping("/api/v1/products/{productId}/content")
    public List<ContentSummary> listContent(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return contentService.listContent(productId);
    }

    @GetMapping("/api/v1/products/{productId}/content/{contentId}")
    public ContentSummary getContent(@PathVariable("productId") UUID productId,
                                     @PathVariable("contentId") UUID contentId,
                                     Authentication authentication) {
        assertProductAccess(authentication, productId);
        return contentService.getContent(productId, contentId);
    }

    @PutMapping("/api/v1/products/{productId}/content/{contentId}")
    public ContentSummary updateContent(@PathVariable("productId") UUID productId,
                                        @PathVariable("contentId") UUID contentId,
                                        @Valid @RequestBody UpdateContentRequest request,
                                        Authentication authentication) {
        assertProductAccess(authentication, productId);
        return contentService.updateContent(productId, contentId, request);
    }

    @PostMapping("/api/v1/products/{productId}/content/{contentId}/transition")
    public ContentSummary transition(@PathVariable("productId") UUID productId,
                                     @PathVariable("contentId") UUID contentId,
                                     @Valid @RequestBody ContentTransitionRequest request,
                                     Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return contentService.transition(productId, contentId, request, caller);
    }

    @GetMapping("/api/v1/products/{productId}/content/{contentId}/versions")
    public List<ContentVersionSummary> listVersions(@PathVariable("productId") UUID productId,
                                                     @PathVariable("contentId") UUID contentId,
                                                     Authentication authentication) {
        assertProductAccess(authentication, productId);
        return contentService.listVersions(productId, contentId);
    }

    @PostMapping("/api/v1/products/{productId}/content/{contentId}/publish")
    public ContentSummary publish(@PathVariable("productId") UUID productId,
                                  @PathVariable("contentId") UUID contentId,
                                  @RequestBody(required = false) PublishContentRequest request,
                                  Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return contentService.publish(productId, contentId, request, caller);
    }

    @GetMapping("/api/v1/products/{productId}/content/edit-events")
    public List<String> listEditEvents(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return contentService.listEditEvents(productId);
    }

    @GetMapping("/api/v1/products/{productId}/content/workflow-items")
    public List<WorkflowItemSummary> listWorkflowItems(@PathVariable("productId") UUID productId,
                                                        Authentication authentication) {
        assertProductAccess(authentication, productId);
        return contentService.listWorkflowItems(productId);
    }

    private AuthenticatedUser assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
        return caller;
    }
}

package br.com.byop.aegis.pages.controller;

import br.com.byop.aegis.pages.contract.CreatePageRequest;
import br.com.byop.aegis.pages.contract.CreateSectionRequest;
import br.com.byop.aegis.pages.contract.ReorderSectionsRequest;
import br.com.byop.aegis.pages.contract.UpdatePageRequest;
import br.com.byop.aegis.pages.contract.UpdateSectionRequest;
import br.com.byop.aegis.pages.dto.PageDetail;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import br.com.byop.aegis.pages.dto.PageSummary;
import br.com.byop.aegis.pages.service.PageService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequireModule(ModuleKey.PAGES)
public class PageController {

    private final PageService pageService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public PageController(PageService pageService, ProductAccessPort productAccessPort,
                          AuthenticatedUserProvider authenticatedUserProvider) {
        this.pageService = pageService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/pages")
    public List<PageSummary> listPages(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return pageService.listPages(productId);
    }

    @PostMapping("/api/v1/products/{productId}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public PageSummary createPage(@PathVariable("productId") UUID productId,
                                  @Valid @RequestBody CreatePageRequest request,
                                  Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return pageService.createPage(productId, request, caller);
    }

    @GetMapping("/api/v1/products/{productId}/pages/{pageId}")
    public PageDetail getPage(@PathVariable("productId") UUID productId,
                              @PathVariable("pageId") UUID pageId,
                              Authentication authentication) {
        assertProductAccess(authentication, productId);
        return pageService.getPage(productId, pageId);
    }

    @PutMapping("/api/v1/products/{productId}/pages/{pageId}")
    public PageDetail updatePage(@PathVariable("productId") UUID productId,
                                 @PathVariable("pageId") UUID pageId,
                                 @Valid @RequestBody UpdatePageRequest request,
                                 Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return pageService.updatePage(productId, pageId, request, caller);
    }

    @DeleteMapping("/api/v1/products/{productId}/pages/{pageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(@PathVariable("productId") UUID productId,
                           @PathVariable("pageId") UUID pageId,
                           Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        pageService.deletePage(productId, pageId, caller);
    }

    @PostMapping("/api/v1/products/{productId}/pages/{pageId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public PageSectionResponse createSection(@PathVariable("productId") UUID productId,
                                             @PathVariable("pageId") UUID pageId,
                                             @Valid @RequestBody CreateSectionRequest request,
                                             Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return pageService.createSection(productId, pageId, request, caller);
    }

    @PutMapping("/api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}")
    public PageSectionResponse updateSection(@PathVariable("productId") UUID productId,
                                             @PathVariable("pageId") UUID pageId,
                                             @PathVariable("sectionId") UUID sectionId,
                                             @Valid @RequestBody UpdateSectionRequest request,
                                             Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return pageService.updateSection(productId, pageId, sectionId, request, caller);
    }

    @DeleteMapping("/api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable("productId") UUID productId,
                              @PathVariable("pageId") UUID pageId,
                              @PathVariable("sectionId") UUID sectionId,
                              Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        pageService.deleteSection(productId, pageId, sectionId, caller);
    }

    @PutMapping("/api/v1/products/{productId}/pages/{pageId}/sections/reorder")
    public List<PageSectionResponse> reorderSections(@PathVariable("productId") UUID productId,
                                                      @PathVariable("pageId") UUID pageId,
                                                      @Valid @RequestBody ReorderSectionsRequest request,
                                                      Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return pageService.reorderSections(productId, pageId, request, caller);
    }

    private AuthenticatedUser assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
        return caller;
    }
}

package br.com.byop.aegis.pages.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.pages.dto.PageDetail;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import br.com.byop.aegis.pages.dto.PageSeoResponse;
import br.com.byop.aegis.pages.dto.PageSummary;
import br.com.byop.aegis.pages.exception.DuplicatePageSlugException;
import br.com.byop.aegis.pages.exception.InvalidSectionContentException;
import br.com.byop.aegis.pages.exception.PageExceptionHandler;
import br.com.byop.aegis.pages.exception.PageNotFoundException;
import br.com.byop.aegis.pages.exception.UnknownBlockTypeException;
import br.com.byop.aegis.pages.service.PageService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = PageController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        PageExceptionHandler.class
})
class PageControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAGE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SECTION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PageService pageService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldListPages() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.listPages(PRODUCT_ID)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/products/{productId}/pages", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("home"));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldCreatePage() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.createPage(eq(PRODUCT_ID), any(), eq(caller))).thenReturn(summary());

        mockMvc.perform(post("/api/v1/products/{productId}/pages", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"slug":"home","title":"Home","locale":"pt-BR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("home"));
    }

    @Test
    void shouldRejectCreatePageWithBlankSlug() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/pages", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"slug":"","title":"Home","locale":"pt-BR"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreatePageWithDuplicateSlug() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.createPage(eq(PRODUCT_ID), any(), eq(caller))).thenThrow(new DuplicatePageSlugException("home"));

        mockMvc.perform(post("/api/v1/products/{productId}/pages", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"slug":"home","title":"Home","locale":"pt-BR"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PAGE_SLUG_ALREADY_EXISTS"));
    }

    @Test
    void shouldGetPageDetail() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.getPage(PRODUCT_ID, PAGE_ID)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/products/{productId}/pages/{pageId}", PRODUCT_ID, PAGE_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Home"));
    }

    @Test
    void shouldReturnNotFoundWhenPageDoesNotExistInProduct() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.getPage(PRODUCT_ID, PAGE_ID)).thenThrow(new PageNotFoundException(PAGE_ID));

        mockMvc.perform(get("/api/v1/products/{productId}/pages/{pageId}", PRODUCT_ID, PAGE_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PAGE_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenProductOutsideScope() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/pages", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldUpdatePage() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.updatePage(eq(PRODUCT_ID), eq(PAGE_ID), any(), eq(caller))).thenReturn(detail());

        mockMvc.perform(put("/api/v1/products/{productId}/pages/{pageId}", PRODUCT_ID, PAGE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"slug":"home","title":"Home","locale":"pt-BR","status":"draft"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Home"));
    }

    @Test
    void shouldDeletePage() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);

        mockMvc.perform(delete("/api/v1/products/{productId}/pages/{pageId}", PRODUCT_ID, PAGE_ID).with(jwt()))
                .andExpect(status().isNoContent());

        verify(pageService).deletePage(PRODUCT_ID, PAGE_ID, caller);
    }

    @Test
    void shouldCreateSection() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.createSection(eq(PRODUCT_ID), eq(PAGE_ID), any(), eq(caller))).thenReturn(sectionResponse());

        mockMvc.perform(post("/api/v1/products/{productId}/pages/{pageId}/sections", PRODUCT_ID, PAGE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"hero","order":0,"content":{"title":"Ola"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("hero"));
    }

    @Test
    void shouldRejectSectionWithUnknownType() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.createSection(eq(PRODUCT_ID), eq(PAGE_ID), any(), eq(caller)))
                .thenThrow(new UnknownBlockTypeException("carousel-3d"));

        mockMvc.perform(post("/api/v1/products/{productId}/pages/{pageId}/sections", PRODUCT_ID, PAGE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"carousel-3d","order":0,"content":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_BLOCK_TYPE"));
    }

    @Test
    void shouldRejectSectionWithInvalidContent() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.createSection(eq(PRODUCT_ID), eq(PAGE_ID), any(), eq(caller)))
                .thenThrow(new InvalidSectionContentException("HERO_TITLE_REQUIRED", "title is required"));

        mockMvc.perform(post("/api/v1/products/{productId}/pages/{pageId}/sections", PRODUCT_ID, PAGE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"hero","order":0,"content":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("HERO_TITLE_REQUIRED"));
    }

    @Test
    void shouldUpdateSection() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.updateSection(eq(PRODUCT_ID), eq(PAGE_ID), eq(SECTION_ID), any(), eq(caller))).thenReturn(sectionResponse());

        mockMvc.perform(put("/api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}", PRODUCT_ID, PAGE_ID, SECTION_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"hero","order":0,"content":{"title":"Ola"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("hero"));
    }

    @Test
    void shouldDeleteSection() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);

        mockMvc.perform(delete("/api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}", PRODUCT_ID, PAGE_ID, SECTION_ID).with(jwt()))
                .andExpect(status().isNoContent());

        verify(pageService).deleteSection(PRODUCT_ID, PAGE_ID, SECTION_ID, caller);
    }

    @Test
    void shouldReorderSections() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.reorderSections(eq(PRODUCT_ID), eq(PAGE_ID), any(), eq(caller))).thenReturn(List.of(sectionResponse()));

        mockMvc.perform(put("/api/v1/products/{productId}/pages/{pageId}/sections/reorder", PRODUCT_ID, PAGE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"sectionIds":["%s"]}
                                """.formatted(SECTION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("hero"));
    }

    @Test
    void shouldReturnNotFoundWhenSectionDoesNotExistInPage() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.updateSection(eq(PRODUCT_ID), eq(PAGE_ID), eq(SECTION_ID), any(), eq(caller)))
                .thenThrow(new br.com.byop.aegis.pages.exception.PageSectionNotFoundException(SECTION_ID));

        mockMvc.perform(put("/api/v1/products/{productId}/pages/{pageId}/sections/{sectionId}", PRODUCT_ID, PAGE_ID, SECTION_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"hero","order":0,"content":{"title":"Ola"}}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PAGE_SECTION_NOT_FOUND"));
    }

    @Test
    void shouldRejectReorderWithMismatchedSectionIds() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.reorderSections(eq(PRODUCT_ID), eq(PAGE_ID), any(), eq(caller)))
                .thenThrow(new br.com.byop.aegis.pages.exception.InvalidSectionReorderException());

        mockMvc.perform(put("/api/v1/products/{productId}/pages/{pageId}/sections/reorder", PRODUCT_ID, PAGE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"sectionIds":["%s"]}
                                """.formatted(SECTION_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SECTION_REORDER"));
    }

    @Test
    void shouldRejectUpdatePageWithInvalidStatus() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(pageService.updatePage(eq(PRODUCT_ID), eq(PAGE_ID), any(), eq(caller)))
                .thenThrow(new br.com.byop.aegis.pages.exception.InvalidPageStatusException("unknown-status"));

        mockMvc.perform(put("/api/v1/products/{productId}/pages/{pageId}", PRODUCT_ID, PAGE_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"slug":"home","title":"Home","locale":"pt-BR","status":"unknown-status"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAGE_STATUS"));
    }

    @Test
    void shouldRequirePagesModule() {
        RequireModule requireModule = PageController.class.getAnnotation(RequireModule.class);

        assertThat(requireModule).isNotNull();
        assertThat(requireModule.value()).isEqualTo(ModuleKey.PAGES);
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("user-1", "user@example.com", "user", "User", Set.of("ROLE_EDITOR"));
    }

    private PageSummary summary() {
        return new PageSummary(PAGE_ID, "home", "Home", "pt-BR", "draft", 1);
    }

    private PageDetail detail() {
        return new PageDetail(PAGE_ID, "home", "Home", "pt-BR", "draft", 1,
                new PageSeoResponse(null, null, null, null, false), List.of());
    }

    private PageSectionResponse sectionResponse() {
        return new PageSectionResponse(SECTION_ID, "hero", null, 0, Map.of("title", "Ola"), null);
    }
}

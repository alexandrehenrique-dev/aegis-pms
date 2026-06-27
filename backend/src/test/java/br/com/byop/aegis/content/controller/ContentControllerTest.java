package br.com.byop.aegis.content.controller;

import br.com.byop.aegis.content.contract.ContentTransitionRequest;
import br.com.byop.aegis.content.dto.ContentSummary;
import br.com.byop.aegis.content.dto.ContentVersionSummary;
import br.com.byop.aegis.content.dto.WorkflowItemSummary;
import br.com.byop.aegis.content.exception.ContentExceptionHandler;
import br.com.byop.aegis.content.exception.ContentNotFoundException;
import br.com.byop.aegis.content.exception.InsufficientContentRoleException;
import br.com.byop.aegis.content.exception.InvalidContentTransitionException;
import br.com.byop.aegis.content.service.ContentService;
import br.com.byop.aegis.core.CoreExceptionHandler;
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
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContentController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        ContentExceptionHandler.class
})
class ContentControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONTENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-06-26T10:00:00-03:00");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldCreateContent() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.createContent(eq(PRODUCT_ID), any(), eq(caller))).thenReturn(summary());

        mockMvc.perform(post("/api/v1/products/{productId}/content", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Artigo","type":"article","lang":"pt-BR","body":"corpo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONTENT_ID.toString()));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldRejectCreateWithBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/content", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"","type":"article","lang":"pt-BR"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListContent() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.listContent(PRODUCT_ID)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/products/{productId}/content", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONTENT_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("Draft"));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldGetContentDetail() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.getContent(PRODUCT_ID, CONTENT_ID)).thenReturn(summary());

        mockMvc.perform(get("/api/v1/products/{productId}/content/{contentId}", PRODUCT_ID, CONTENT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Artigo"));
    }

    @Test
    void shouldUpdateContent() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.updateContent(eq(PRODUCT_ID), eq(CONTENT_ID), any())).thenReturn(summary());

        mockMvc.perform(put("/api/v1/products/{productId}/content/{contentId}", PRODUCT_ID, CONTENT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Artigo","type":"article","lang":"pt-BR","body":"corpo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONTENT_ID.toString()));
    }

    @Test
    void shouldRejectUpdateWithBlankTitle() throws Exception {
        mockMvc.perform(put("/api/v1/products/{productId}/content/{contentId}", PRODUCT_ID, CONTENT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"","type":"article","lang":"pt-BR"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldTransitionContent() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        ContentTransitionRequest request = new ContentTransitionRequest("Draft", "In Review", null);
        when(contentService.transition(PRODUCT_ID, CONTENT_ID, request, caller)).thenReturn(summary());

        mockMvc.perform(post("/api/v1/products/{productId}/content/{contentId}/transition", PRODUCT_ID, CONTENT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(contentService).transition(PRODUCT_ID, CONTENT_ID, request, caller);
    }

    @Test
    void shouldRejectInvalidTransition() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.transition(eq(PRODUCT_ID), eq(CONTENT_ID), any(), eq(caller)))
                .thenThrow(new InvalidContentTransitionException(
                        br.com.byop.aegis.content.domain.ContentStatus.DRAFT,
                        br.com.byop.aegis.content.domain.ContentStatus.PUBLISHED));

        mockMvc.perform(post("/api/v1/products/{productId}/content/{contentId}/transition", PRODUCT_ID, CONTENT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"from":"Draft","to":"Published"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CONTENT_TRANSITION"));
    }

    @Test
    void shouldPublishWithAuthorizedRole() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_PRODUCT_MANAGER"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.publish(eq(PRODUCT_ID), eq(CONTENT_ID), any(), eq(caller))).thenReturn(summary());

        mockMvc.perform(post("/api/v1/products/{productId}/content/{contentId}/publish", PRODUCT_ID, CONTENT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectEditorPublishing() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.publish(eq(PRODUCT_ID), eq(CONTENT_ID), any(), eq(caller)))
                .thenThrow(new InsufficientContentRoleException());

        mockMvc.perform(post("/api/v1/products/{productId}/content/{contentId}/publish", PRODUCT_ID, CONTENT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CONTENT_PUBLISH_FORBIDDEN"));
    }

    @Test
    void shouldAcceptPublishWithoutBody() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_SUPER_ADMIN"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.publish(PRODUCT_ID, CONTENT_ID, null, caller)).thenReturn(summary());

        mockMvc.perform(post("/api/v1/products/{productId}/content/{contentId}/publish", PRODUCT_ID, CONTENT_ID)
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenProductOutsideScope() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/content", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenContentNotInProduct() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.getContent(PRODUCT_ID, CONTENT_ID)).thenThrow(new ContentNotFoundException(CONTENT_ID));

        mockMvc.perform(get("/api/v1/products/{productId}/content/{contentId}", PRODUCT_ID, CONTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CONTENT_NOT_FOUND"));
    }

    @Test
    void shouldListVersionsInChronologicalOrder() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        ContentVersionSummary first = new ContentVersionSummary(UUID.randomUUID(), "v1", "Alexandre Silva", UPDATED_AT, "{}");
        ContentVersionSummary second = new ContentVersionSummary(UUID.randomUUID(), "v2", "Alexandre Silva", UPDATED_AT.plusMinutes(5), "{}");
        when(contentService.listVersions(PRODUCT_ID, CONTENT_ID)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/products/{productId}/content/{contentId}/versions", PRODUCT_ID, CONTENT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionLabel").value("v1"))
                .andExpect(jsonPath("$[1].versionLabel").value("v2"));
    }

    @Test
    void shouldListWorkflowItems() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.listWorkflowItems(PRODUCT_ID)).thenReturn(List.of(
                new WorkflowItemSummary(CONTENT_ID, "Artigo", "article", "pt-BR", "Alexandre Silva", "Draft", "v1")
        ));

        mockMvc.perform(get("/api/v1/products/{productId}/content/workflow-items", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("Draft"));
    }

    @Test
    void shouldListEditEvents() throws Exception {
        AuthenticatedUser caller = user(Set.of("ROLE_EDITOR"));
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(contentService.listEditEvents(PRODUCT_ID)).thenReturn(List.of("Alexandre Silva moveu o conteudo para In Review (v2)."));

        mockMvc.perform(get("/api/v1/products/{productId}/content/edit-events", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Alexandre Silva moveu o conteudo para In Review (v2)."));
    }

    @Test
    void shouldRequireContentModule() {
        RequireModule requireModule = ContentController.class.getAnnotation(RequireModule.class);

        assertThat(requireModule).isNotNull();
        assertThat(requireModule.value()).isEqualTo(ModuleKey.CONTENT);
    }

    private AuthenticatedUser user(Set<String> authorities) {
        return new AuthenticatedUser("user-1", "user@example.com", "user", "User", authorities);
    }

    private ContentSummary summary() {
        return new ContentSummary(
                CONTENT_ID, "Artigo", "article", "pt-BR", "Alexandre Silva", "Draft",
                UPDATED_AT, "—", "v1", null, null, null, null, null, null
        );
    }
}

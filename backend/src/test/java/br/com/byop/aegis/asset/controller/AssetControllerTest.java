package br.com.byop.aegis.asset.controller;

import br.com.byop.aegis.asset.dto.AssetDetail;
import br.com.byop.aegis.asset.dto.AssetSummary;
import br.com.byop.aegis.asset.dto.AssetUsageSummary;
import br.com.byop.aegis.asset.dto.ResolvedAsset;
import br.com.byop.aegis.asset.exception.AssetExceptionHandler;
import br.com.byop.aegis.asset.exception.AssetInUseException;
import br.com.byop.aegis.asset.exception.AssetNotFoundException;
import br.com.byop.aegis.asset.exception.AssetSizeLimitExceededException;
import br.com.byop.aegis.asset.exception.AssetTagAlreadyExistsException;
import br.com.byop.aegis.asset.exception.InvalidAssetMimeTypeException;
import br.com.byop.aegis.asset.service.AssetService;
import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.exception.ProductContentAccessDeniedException;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssetController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        AssetExceptionHandler.class
})
class AssetControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ASSET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2026-06-27T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldListAssets() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(assetService.listAssets(PRODUCT_ID)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/products/{productId}/assets", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ASSET_ID.toString()));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldUploadAsset() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(assetService.uploadAsset(eq(PRODUCT_ID), any(), eq("Currículo"), eq(caller))).thenReturn(summary());

        MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/products/{productId}/assets", PRODUCT_ID)
                        .file(file)
                        .param("friendlyName", "Currículo")
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ASSET_ID.toString()));
    }

    @Test
    void shouldRejectUploadWithInvalidMimeTypeWith400() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.uploadAsset(eq(PRODUCT_ID), any(), any(), any()))
                .thenThrow(new InvalidAssetMimeTypeException("application/x-msdownload"));

        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", "x".getBytes());

        mockMvc.perform(multipart("/api/v1/products/{productId}/assets", PRODUCT_ID).file(file).with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ASSET_MIME_TYPE"));
    }

    @Test
    void shouldRejectUploadAboveLimitWith413() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.uploadAsset(eq(PRODUCT_ID), any(), any(), any()))
                .thenThrow(new AssetSizeLimitExceededException(br.com.byop.aegis.asset.domain.AssetCategory.IMAGE, 999L, 100L));

        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[999]);

        mockMvc.perform(multipart("/api/v1/products/{productId}/assets", PRODUCT_ID).file(file).with(jwt()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.error").value("ASSET_SIZE_LIMIT_EXCEEDED"));
    }

    @Test
    void shouldGetAssetDetail() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.getAsset(PRODUCT_ID, ASSET_ID)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/products/{productId}/assets/{assetId}", PRODUCT_ID, ASSET_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ASSET_ID.toString()));
    }

    @Test
    void shouldReturn404WhenAssetNotFoundInProduct() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.getAsset(PRODUCT_ID, ASSET_ID)).thenThrow(new AssetNotFoundException(ASSET_ID));

        mockMvc.perform(get("/api/v1/products/{productId}/assets/{assetId}", PRODUCT_ID, ASSET_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ASSET_NOT_FOUND"));
    }

    @Test
    void shouldReturn403WhenSuperAdminHasNoAssignment() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        doThrow(new ProductContentAccessDeniedException(PRODUCT_ID)).when(productAccessPort).assertAccessible(eq(PRODUCT_ID), any());

        mockMvc.perform(get("/api/v1/products/{productId}/assets", PRODUCT_ID).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PRODUCT_CONTENT_ACCESS_DENIED"));
    }

    @Test
    void shouldReturn404ForCrossProductAccess() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(eq(PRODUCT_ID), any());

        mockMvc.perform(get("/api/v1/products/{productId}/assets", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldUpdateMetadata() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.updateMetadata(eq(PRODUCT_ID), eq(ASSET_ID), any())).thenReturn(detail());

        mockMvc.perform(put("/api/v1/products/{productId}/assets/{assetId}/metadata", PRODUCT_ID, ASSET_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"friendlyName":"Novo nome","altText":"Alt","caption":"Legenda","credit":"Credito","tags":"blog"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendlyName").value("foto.png"));
    }

    @Test
    void shouldRejectUpdateMetadataWithBlankFriendlyName() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());

        mockMvc.perform(put("/api/v1/products/{productId}/assets/{assetId}/metadata", PRODUCT_ID, ASSET_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"friendlyName":"","tags":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_FAILED"));
    }

    @Test
    void shouldDeleteAssetWithoutForce() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());

        mockMvc.perform(delete("/api/v1/products/{productId}/assets/{assetId}", PRODUCT_ID, ASSET_ID).with(jwt()))
                .andExpect(status().isNoContent());

        verify(assetService).deleteAsset(any(AuthenticatedUser.class), eq(PRODUCT_ID), eq(ASSET_ID), eq(false));
    }

    @Test
    void shouldDeleteAssetWithForce() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());

        mockMvc.perform(delete("/api/v1/products/{productId}/assets/{assetId}", PRODUCT_ID, ASSET_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"force":true}
                                """))
                .andExpect(status().isNoContent());

        verify(assetService).deleteAsset(any(AuthenticatedUser.class), eq(PRODUCT_ID), eq(ASSET_ID), eq(true));
    }

    @Test
    void shouldDeleteAssetWithExplicitForceFalseBody() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());

        mockMvc.perform(delete("/api/v1/products/{productId}/assets/{assetId}", PRODUCT_ID, ASSET_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"force":false}
                                """))
                .andExpect(status().isNoContent());

        verify(assetService).deleteAsset(any(AuthenticatedUser.class), eq(PRODUCT_ID), eq(ASSET_ID), eq(false));
    }

    @Test
    void shouldRejectDeleteWhenAssetInUseWithout409() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        doThrow(new AssetInUseException(ASSET_ID)).when(assetService)
                .deleteAsset(any(AuthenticatedUser.class), eq(PRODUCT_ID), eq(ASSET_ID), anyBoolean());

        mockMvc.perform(delete("/api/v1/products/{productId}/assets/{assetId}", PRODUCT_ID, ASSET_ID).with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ASSET_IN_USE"));
    }

    @Test
    void shouldListUsage() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.listUsage(PRODUCT_ID, ASSET_ID)).thenReturn(List.of(new AssetUsageSummary("CONTENT", "content-1", "Artigo")));

        mockMvc.perform(get("/api/v1/products/{productId}/assets/{assetId}/usage", PRODUCT_ID, ASSET_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usedInLabel").value("Artigo"));
    }

    @Test
    void shouldListTags() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.listTags(PRODUCT_ID)).thenReturn(List.of("blog", "institucional"));

        mockMvc.perform(get("/api/v1/products/{productId}/asset-tags", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("blog"));
    }

    @Test
    void shouldCreateTag() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.createTag(PRODUCT_ID, "institucional")).thenReturn(List.of("institucional"));

        mockMvc.perform(post("/api/v1/products/{productId}/asset-tags", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("\"institucional\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("institucional"));
    }

    @Test
    void shouldRejectDuplicateTagWith409() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.createTag(PRODUCT_ID, "blog")).thenThrow(new AssetTagAlreadyExistsException(PRODUCT_ID, "blog"));

        mockMvc.perform(post("/api/v1/products/{productId}/asset-tags", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("\"blog\""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ASSET_TAG_ALREADY_EXISTS"));
    }

    @Test
    void shouldRejectMalformedJsonTagBodyWith400() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());

        mockMvc.perform(post("/api/v1/products/{productId}/asset-tags", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ASSET_TAG_NAME"));
    }

    @Test
    void shouldDeleteTag() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.deleteTag(PRODUCT_ID, "blog")).thenReturn(List.of());

        mockMvc.perform(delete("/api/v1/products/{productId}/asset-tags/{tag}", PRODUCT_ID, "blog").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldResolveAssetWithoutModuleGating() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(assetService.resolveAsset(ASSET_ID, caller)).thenReturn(new ResolvedAsset(ASSET_ID, "/api/v1/assets/" + ASSET_ID + "/file", null, "image/png"));

        mockMvc.perform(get("/api/v1/assets/{assetId}/resolve", ASSET_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("image/png"));

        verify(productAccessPort, org.mockito.Mockito.never()).assertAccessible(any(), any());
    }

    @Test
    void shouldReturn404WhenResolvingAssetFromAnotherTenant() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.resolveAsset(eq(ASSET_ID), any())).thenThrow(new AssetNotFoundException(ASSET_ID));

        mockMvc.perform(get("/api/v1/assets/{assetId}/resolve", ASSET_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ASSET_NOT_FOUND"));
    }

    @Test
    void shouldDownloadAssetFile() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(assetService.loadAssetFile(eq(ASSET_ID), any())).thenReturn(
                new br.com.byop.aegis.asset.dto.AssetFileContent("conteudo".getBytes(), "application/pdf", "curriculo.pdf"));

        mockMvc.perform(get("/api/v1/assets/{assetId}/file", ASSET_ID).with(jwt()))
                .andExpect(status().isOk());
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("subject-1", "user@aegis.app", "user", "User", Set.of("ROLE_EDITOR"));
    }

    private AssetSummary summary() {
        return new AssetSummary(ASSET_ID, "foto.png", "image", "1 KB", "ativo", "", "", "2026-06-27");
    }

    private AssetDetail detail() {
        return new AssetDetail(ASSET_ID, "foto.png", "foto.png", null, null, null, "image/png", "image", 1024L,
                "ativo", List.of(), List.of(), "subject-1", FIXED_TIMESTAMP, FIXED_TIMESTAMP);
    }
}

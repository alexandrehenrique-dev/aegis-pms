package br.com.byop.aegis.product.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductDetail;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.dto.ProductSummary;
import br.com.byop.aegis.product.exception.InvalidModuleKeyException;
import br.com.byop.aegis.product.exception.InvalidProductTypeException;
import br.com.byop.aegis.product.exception.ModuleDependencyMissingException;
import br.com.byop.aegis.product.exception.ProductAlreadyExistsException;
import br.com.byop.aegis.product.exception.ProductContentAccessDeniedException;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.service.ProductModuleService;
import br.com.byop.aegis.product.service.ProductService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class
})
class ProductControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MODULE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductModuleService productModuleService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldCreateProduct() throws Exception {
        AuthenticatedUser caller = user();
        ProductSummary summary = productSummary();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productService.createProduct(any(AuthenticatedUser.class), any())).thenReturn(summary);

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "22222222-2222-2222-2222-222222222222",
                                  "key": "maestro-beton",
                                  "name": "Maestro Beton",
                                  "type": "SITE_INSTITUCIONAL",
                                  "defaultLocale": "pt-BR",
                                  "assetStorageStrategy": "S3"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.key").value("maestro-beton"))
                .andExpect(jsonPath("$.type").value("SITE_INSTITUCIONAL"));
    }

    @Test
    void shouldReturnConflictWhenProductKeyAlreadyExists() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productService.createProduct(any(AuthenticatedUser.class), any()))
                .thenThrow(new ProductAlreadyExistsException(TENANT_ID, "maestro-beton"));

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(validCreateProductJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PRODUCT_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturnBadRequestWhenProductTypeIsInvalid() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productService.createProduct(any(AuthenticatedUser.class), any()))
                .thenThrow(new InvalidProductTypeException("UNKNOWN"));

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content(validCreateProductJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PRODUCT_TYPE"));
    }

    @Test
    void shouldListProducts() throws Exception {
        AuthenticatedUser caller = user();
        ProductSummary summary = productSummary();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productService.listProducts(caller)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/products")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$[0].key").value("maestro-beton"));
    }

    @Test
    void shouldGetProductDetail() throws Exception {
        AuthenticatedUser caller = user();
        ProductDetail detail = productDetail();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productService.getProductDetail(caller, PRODUCT_ID)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/products/{productId}", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.modules[0].id").value(MODULE_ID.toString()))
                .andExpect(jsonPath("$.modules[0].moduleKey").value("CONTENT"));
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExistOrIsOutsideScope() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productService.getProductDetail(caller, PRODUCT_ID)).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(get("/api/v1/products/{productId}", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldEnableModule() throws Exception {
        AuthenticatedUser caller = user();
        ProductModuleSummary summary = moduleSummary(true);
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productModuleService.enableModule(PRODUCT_ID, "CONTENT", caller)).thenReturn(summary);

        mockMvc.perform(post("/api/v1/products/{productId}/modules/{moduleKey}/enable", PRODUCT_ID, "CONTENT")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MODULE_ID.toString()))
                .andExpect(jsonPath("$.moduleKey").value("CONTENT"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void shouldDisableModule() throws Exception {
        AuthenticatedUser caller = user();
        ProductModuleSummary summary = moduleSummary(false);
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productModuleService.disableModule(PRODUCT_ID, "CONTENT", caller)).thenReturn(summary);

        mockMvc.perform(post("/api/v1/products/{productId}/modules/{moduleKey}/disable", PRODUCT_ID, "CONTENT")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MODULE_ID.toString()))
                .andExpect(jsonPath("$.moduleKey").value("CONTENT"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void shouldReturnBadRequestWhenModuleKeyIsInvalid() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productModuleService.enableModule(PRODUCT_ID, "UNKNOWN", caller))
                .thenThrow(new InvalidModuleKeyException("UNKNOWN"));

        mockMvc.perform(post("/api/v1/products/{productId}/modules/{moduleKey}/enable", PRODUCT_ID, "UNKNOWN")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_MODULE_KEY"));
    }

    @Test
    void shouldReturnBadRequestWhenModuleDependencyIsMissing() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productModuleService.enableModule(PRODUCT_ID, "KNOWLEDGE_GRAPH", caller))
                .thenThrow(new ModuleDependencyMissingException(ModuleKey.KNOWLEDGE_GRAPH, ModuleKey.CONTENT));

        mockMvc.perform(post("/api/v1/products/{productId}/modules/{moduleKey}/enable", PRODUCT_ID, "KNOWLEDGE_GRAPH")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MODULE_DEPENDENCY_MISSING"))
                .andExpect(jsonPath("$.moduleKey").value("KNOWLEDGE_GRAPH"))
                .andExpect(jsonPath("$.requires").value("CONTENT"));
    }

    @Test
    void shouldReturnForbiddenWhenProductContentAccessIsDenied() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productService.getProductDetail(caller, PRODUCT_ID))
                .thenThrow(new ProductContentAccessDeniedException(PRODUCT_ID));

        mockMvc.perform(get("/api/v1/products/{productId}", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PRODUCT_CONTENT_ACCESS_DENIED"));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": null,
                                  "key": "",
                                  "name": "",
                                  "type": "",
                                  "defaultLocale": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private ProductSummary productSummary() {
        return new ProductSummary(
                PRODUCT_ID,
                TENANT_ID,
                "maestro-beton",
                "Maestro Beton",
                ProductTypeKey.SITE_INSTITUCIONAL,
                ProductStatus.ACTIVE,
                "pt-BR",
                AssetStorageStrategy.S3,
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
    }

    private String validCreateProductJson() {
        return """
                {
                  "tenantId": "22222222-2222-2222-2222-222222222222",
                  "key": "maestro-beton",
                  "name": "Maestro Beton",
                  "type": "SITE_INSTITUCIONAL",
                  "defaultLocale": "pt-BR",
                  "assetStorageStrategy": "S3"
                }
                """;
    }

    private ProductDetail productDetail() {
        ProductSummary summary = productSummary();
        return new ProductDetail(
                summary.id(),
                summary.tenantId(),
                summary.key(),
                summary.name(),
                summary.type(),
                summary.status(),
                summary.defaultLocale(),
                summary.assetStorageStrategy(),
                summary.createdAt(),
                summary.updatedAt(),
                List.of(moduleSummary(true))
        );
    }

    private ProductModuleSummary moduleSummary(boolean enabled) {
        return new ProductModuleSummary(
                MODULE_ID,
                PRODUCT_ID,
                ModuleKey.CONTENT,
                enabled,
                "{}",
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(
                "subject-123",
                "subject@byop.dev",
                "subject",
                "Subject",
                Set.of("ROLE_SUPER_ADMIN")
        );
    }
}

package br.com.byop.aegis.product.module;

import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ModuleAccessAspectTest.TestModuleController.class,
        ModuleAccessAspectTest.ClassLevelModuleController.class
})
@Import({
        SecurityConfig.class,
        ProductExceptionHandler.class,
        ModuleAccessAspect.class,
        ModuleAccessAspectTest.TestModuleController.class,
        ModuleAccessAspectTest.ClassLevelModuleController.class
})
class ModuleAccessAspectTest {

    private static final UUID PRODUCT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductModuleRepository moduleRepository;

    @Test
    void shouldBlockWhenRequiredModuleIsDisabledOrMissing() throws Exception {
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.CONTENT))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/module-gating-test/products/{productId}/content", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.moduleKey").value("CONTENT"));
    }

    @Test
    void shouldAllowWhenRequiredModuleIsEnabled() throws Exception {
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.CONTENT))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/module-gating-test/products/{productId}/content", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void shouldBlockSuperAdminWhenRequiredModuleIsDisabled() throws Exception {
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.CONTENT))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/module-gating-test/products/{productId}/content", PRODUCT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.moduleKey").value("CONTENT"));
    }

    @Test
    void shouldResolveProductIdFromPathVariableNameAttribute() throws Exception {
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.CONTENT))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/module-gating-test/products/{productId}/content-by-name", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void shouldSkipNonPathVariableArgumentsWhenResolvingProductId() throws Exception {
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.CONTENT))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/module-gating-test/products/{productId}/content-with-query", PRODUCT_ID)
                        .param("marker", "scan")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("scan"));
    }

    @Test
    void shouldFailClearlyWhenAnnotatedMethodDoesNotExposeProductId() throws Exception {
        mockMvc.perform(get("/api/v1/module-gating-test/products/{id}/missing-product-id", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("MODULE_PRODUCT_ID_MISSING"));

        verify(moduleRepository, never()).existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.CONTENT);
    }

    @Test
    void shouldFailClearlyWhenProductIdPathVariableIsNotUuidArgument() throws Exception {
        mockMvc.perform(get("/api/v1/module-gating-test/string-products/not-a-uuid-product-id/content")
                        .with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("MODULE_PRODUCT_ID_MISSING"));
    }

    @Test
    void shouldApplyModuleGatingFromControllerClassAnnotation() throws Exception {
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.KNOWLEDGE_GRAPH))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/module-gating-test/products/{productId}/class-level", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.moduleKey").value("KNOWLEDGE_GRAPH"));
    }

    @Test
    void shouldAllowFromControllerClassAnnotationWhenRequiredModuleIsEnabled() throws Exception {
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.KNOWLEDGE_GRAPH))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/module-gating-test/products/{productId}/class-level", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @RestController
    public static class TestModuleController {

        @RequireModule(ModuleKey.CONTENT)
        @GetMapping("/api/v1/module-gating-test/products/{productId}/content")
        public String content(@PathVariable("productId") UUID productId) {
            return "ok";
        }

        @RequireModule(ModuleKey.CONTENT)
        @GetMapping("/api/v1/module-gating-test/products/{productId}/content-by-name")
        public String contentByName(@PathVariable(name = "productId") UUID productId) {
            return "ok";
        }

        @RequireModule(ModuleKey.CONTENT)
        @GetMapping("/api/v1/module-gating-test/products/{productId}/content-with-query")
        public String contentWithQuery(@RequestParam("marker") String marker, @PathVariable("productId") UUID productId) {
            return marker;
        }

        @RequireModule(ModuleKey.CONTENT)
        @GetMapping("/api/v1/module-gating-test/products/{id}/missing-product-id")
        public String missingProductId(@PathVariable("id") UUID id) {
            return "never";
        }

        @RequireModule(ModuleKey.CONTENT)
        @GetMapping("/api/v1/module-gating-test/string-products/{productId}/content")
        public String nonUuidProductId(@PathVariable("productId") String productId) {
            return productId;
        }
    }

    @RestController
    @RequireModule(ModuleKey.KNOWLEDGE_GRAPH)
    public static class ClassLevelModuleController {

        @GetMapping("/api/v1/module-gating-test/products/{productId}/class-level")
        public String classLevel(@PathVariable("productId") UUID productId) {
            return "ok";
        }
    }
}

package br.com.byop.aegis.product.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import br.com.byop.aegis.product.api.ModuleKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductExceptionHandlerTest {

    private final ProductExceptionHandler handler = new ProductExceptionHandler();

    @Test
    void shouldReturnProductErrorCodes() {
        assertThat(handler.handleProductAlreadyExists().error()).isEqualTo("PRODUCT_ALREADY_EXISTS");
        assertThat(handler.handleProductNotFound().error()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(handler.handleInvalidProductType().error()).isEqualTo("INVALID_PRODUCT_TYPE");
        assertThat(handler.handleInvalidModuleKey().error()).isEqualTo("INVALID_MODULE_KEY");
        assertThat(handler.handleProductContentAccessDenied().error()).isEqualTo("PRODUCT_CONTENT_ACCESS_DENIED");
        assertThat(handler.handleModuleProductIdMissing().error()).isEqualTo("MODULE_PRODUCT_ID_MISSING");
    }

    @Test
    void shouldReturnModuleDependencyMetadata() {
        ModuleDependencyMissingException exception = new ModuleDependencyMissingException(
                ModuleKey.KNOWLEDGE_GRAPH,
                ModuleKey.CONTENT
        );

        CoreErrorResponse response = handler.handleModuleDependencyMissing(exception);

        assertThat(response.error()).isEqualTo("MODULE_DEPENDENCY_MISSING");
        assertThat(response.moduleKey()).isEqualTo("KNOWLEDGE_GRAPH");
        assertThat(response.requires()).isEqualTo("CONTENT");
    }

    @Test
    void shouldReturnDisabledModuleMetadata() {
        CoreErrorResponse response = handler.handleModuleDisabled(new ModuleDisabledException(ModuleKey.CONTENT));

        assertThat(response.error()).isEqualTo("MODULE_DISABLED");
        assertThat(response.moduleKey()).isEqualTo("CONTENT");
        assertThat(response.requires()).isNull();
    }
}

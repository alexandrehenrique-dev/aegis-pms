package br.com.byop.aegis.product.export.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.export.dto.DeleteAcceptedResponse;
import br.com.byop.aegis.product.export.exception.InvalidProductDeleteConfirmationException;
import br.com.byop.aegis.product.export.exception.ProductExportExceptionHandler;
import br.com.byop.aegis.product.export.service.ProductDeleteService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductDeleteController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        ProductExportExceptionHandler.class
})
class ProductDeleteControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductDeleteService productDeleteService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldAcceptProductDelete() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productDeleteService.deleteProduct(any(AuthenticatedUser.class), any(UUID.class), any()))
                .thenReturn(new DeleteAcceptedResponse("Exportação iniciada."));

        mockMvc.perform(delete("/api/v1/products/{productId}", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationText": "Maestro Beton"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Exportação iniciada."));
    }

    @Test
    void shouldRejectInvalidProductDeleteConfirmation() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(productDeleteService.deleteProduct(any(AuthenticatedUser.class), any(UUID.class), any()))
                .thenThrow(new InvalidProductDeleteConfirmationException());

        mockMvc.perform(delete("/api/v1/products/{productId}", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationText": "Wrong"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PRODUCT_DELETE_CONFIRMATION"));
    }

    @Test
    void shouldRejectBlankConfirmation() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{productId}", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationText": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_FAILED"));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("subject", "subject@byop.dev", "Subject", "subject", Set.of("ROLE_SUPER_ADMIN"));
    }
}

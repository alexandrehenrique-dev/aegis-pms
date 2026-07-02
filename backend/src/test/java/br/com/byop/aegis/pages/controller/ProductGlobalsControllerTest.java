package br.com.byop.aegis.pages.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.pages.dto.FooterResponse;
import br.com.byop.aegis.pages.dto.NavbarResponse;
import br.com.byop.aegis.pages.dto.ProductGlobalsResponse;
import br.com.byop.aegis.pages.exception.PageExceptionHandler;
import br.com.byop.aegis.pages.service.ProductGlobalsService;
import br.com.byop.aegis.product.api.ProductAccessPort;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductGlobalsController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        PageExceptionHandler.class
})
class ProductGlobalsControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductGlobalsService globalsService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldGetGlobals() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(globalsService.getGlobals(PRODUCT_ID)).thenReturn(response());

        mockMvc.perform(get("/api/v1/products/{productId}/globals", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navbar.links").isEmpty());

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldUpdateGlobals() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(globalsService.updateGlobals(eq(PRODUCT_ID), any(), eq(caller))).thenReturn(response());

        mockMvc.perform(put("/api/v1/products/{productId}/globals", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"navbar":{"links":[{"label":"Home","href":"/"}]},"footer":{"links":[]},"socialLinks":[]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUpdateGlobalsWithoutNavbar() throws Exception {
        mockMvc.perform(put("/api/v1/products/{productId}/globals", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"footer":{"links":[]},"socialLinks":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenProductOutsideScope() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/globals", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldNotRequireAnyModule() {
        org.assertj.core.api.Assertions.assertThat(
                ProductGlobalsController.class.getAnnotation(br.com.byop.aegis.product.api.RequireModule.class)
        ).isNull();
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("user-1", "user@example.com", "user", "User", Set.of("ROLE_EDITOR"));
    }

    private ProductGlobalsResponse response() {
        return new ProductGlobalsResponse(
                new NavbarResponse(null, List.of()),
                new FooterResponse(null, List.of()),
                List.of(),
                null
        );
    }
}

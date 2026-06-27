package br.com.byop.aegis.form.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.form.dto.FormDetail;
import br.com.byop.aegis.form.dto.FormSummary;
import br.com.byop.aegis.form.exception.FormExceptionHandler;
import br.com.byop.aegis.form.exception.FormNotFoundException;
import br.com.byop.aegis.form.exception.InvalidFormDeliveryException;
import br.com.byop.aegis.form.exception.InvalidFormPublicationException;
import br.com.byop.aegis.form.service.FormPublicationPolicy;
import br.com.byop.aegis.form.service.FormService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.module.ModuleAccessAspect;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FormController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        FormExceptionHandler.class,
        ModuleAccessAspect.class
})
class FormControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FORM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FormService formService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @MockitoBean
    private ProductModuleRepository productModuleRepository;

    @BeforeEach
    void setUp() {
        when(productModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.FORMS))
                .thenReturn(true);
    }

    @Test
    void shouldListForms() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(formService.listForms(PRODUCT_ID)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/products/{productId}/forms", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(FORM_ID.toString()));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldCreateForm() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.createForm(eq(PRODUCT_ID), any())).thenReturn(detail());

        mockMvc.perform(post("/api/v1/products/{productId}/forms", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Contato","type":"lead","fields":[{"label":"Email","type":"Email","required":true}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(FORM_ID.toString()));
    }

    @Test
    void shouldRejectCreateWithBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/forms", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"","type":"lead","fields":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_FAILED"));
    }

    @Test
    void shouldGetForm() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.getForm(PRODUCT_ID, FORM_ID)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/products/{productId}/forms/{formId}", PRODUCT_ID, FORM_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Contato"));
    }

    @Test
    void shouldUpdateForm() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.updateForm(eq(PRODUCT_ID), eq(FORM_ID), any())).thenReturn(detail());

        mockMvc.perform(put("/api/v1/products/{productId}/forms/{formId}", PRODUCT_ID, FORM_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Contato","type":"lead","fields":[{"label":"Email","type":"Email","required":true}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FORM_ID.toString()));
    }

    @Test
    void shouldListFieldTypes() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.listFieldTypes(PRODUCT_ID)).thenReturn(List.of("Texto", "Email", "Upload"));

        mockMvc.perform(get("/api/v1/products/{productId}/forms/field-types", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2]").value("Upload"));
    }

    @Test
    void shouldPublishForm() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.publish(PRODUCT_ID, FORM_ID)).thenReturn(detail());

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/publish", PRODUCT_ID, FORM_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publication").value("2026-06-27/forms/contato"));
    }

    @Test
    void shouldRejectPublishWithoutRequiredField() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.publish(PRODUCT_ID, FORM_ID))
                .thenThrow(new InvalidFormPublicationException(FormPublicationPolicy.REQUIRED_FIELD_ERROR));

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/publish", PRODUCT_ID, FORM_ID).with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(FormPublicationPolicy.REQUIRED_FIELD_ERROR));
    }

    @Test
    void shouldUpdateDelivery() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.updateDelivery(eq(PRODUCT_ID), eq(FORM_ID), any())).thenReturn(detail());

        mockMvc.perform(put("/api/v1/products/{productId}/forms/{formId}/delivery", PRODUCT_ID, FORM_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"channels":[{"type":"webhook","enabled":true,"config":{"url":"https://example.com/hook"}}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FORM_ID.toString()));
    }

    @Test
    void shouldRejectInvalidDelivery() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.updateDelivery(eq(PRODUCT_ID), eq(FORM_ID), any()))
                .thenThrow(new InvalidFormDeliveryException("invalid webhook"));

        mockMvc.perform(put("/api/v1/products/{productId}/forms/{formId}/delivery", PRODUCT_ID, FORM_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"channels":[{"type":"webhook","enabled":true,"config":{"url":"not-a-url"}}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FORM_DELIVERY"));
    }

    @Test
    void shouldReturnNotFoundForCrossProductAccess() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/forms", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenFormDoesNotBelongToProduct() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(formService.getForm(PRODUCT_ID, FORM_ID)).thenThrow(new FormNotFoundException(FORM_ID));

        mockMvc.perform(get("/api/v1/products/{productId}/forms/{formId}", PRODUCT_ID, FORM_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FORM_NOT_FOUND"));
    }

    @Test
    void shouldReturnModuleDisabledWhenFormsModuleIsDisabled() throws Exception {
        when(productModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.FORMS))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/products/{productId}/forms", PRODUCT_ID).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.moduleKey").value("FORMS"));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("subject-1", "user@aegis.app", "user", "User", Set.of("ROLE_EDITOR"));
    }

    private FormSummary summary() {
        return new FormSummary(FORM_ID, "Contato", "lead", "Draft", "0", "0%", "—", "—");
    }

    private FormDetail detail() {
        return new FormDetail(FORM_ID, "Contato", "lead", "Draft",
                List.of(Map.of("label", "Email", "type", "Email", "required", true)),
                List.of(), "2026-06-27/forms/contato", OffsetDateTime.parse("2026-06-27T10:00:00Z"),
                OffsetDateTime.parse("2026-06-27T10:00:00Z"));
    }
}

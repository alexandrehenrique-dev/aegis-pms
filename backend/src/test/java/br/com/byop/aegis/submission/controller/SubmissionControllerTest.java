package br.com.byop.aegis.submission.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.module.ModuleAccessAspect;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import br.com.byop.aegis.submission.dto.SubmissionDetail;
import br.com.byop.aegis.submission.dto.SubmissionSummary;
import br.com.byop.aegis.submission.exception.SubmissionExceptionHandler;
import br.com.byop.aegis.submission.exception.SubmissionNotFoundException;
import br.com.byop.aegis.submission.service.SubmissionService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubmissionController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        SubmissionExceptionHandler.class,
        ModuleAccessAspect.class
})
class SubmissionControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FORM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SUBMISSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmissionService submissionService;

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
    void shouldListSubmissionsByForm() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(submissionService.listSubmissions(PRODUCT_ID, FORM_ID)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/products/{productId}/forms/{formId}/submissions", PRODUCT_ID, FORM_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SUBMISSION_ID.toString()));

        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldListProductSubmissions() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(submissionService.listProductSubmissions(PRODUCT_ID)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/products/{productId}/forms/submissions", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ana@example.com"));
    }

    @Test
    void shouldGetSubmissionDetailWithAnswersJson() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(submissionService.getSubmission(PRODUCT_ID, FORM_ID, SUBMISSION_ID)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/products/{productId}/forms/{formId}/submissions/{submissionId}",
                        PRODUCT_ID, FORM_ID, SUBMISSION_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answersJson.Email").value("ana@example.com"));
    }

    @Test
    void shouldReturnNotFoundForCrossProductAccess() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/forms/submissions", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenSubmissionDoesNotBelongToForm() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(user());
        when(submissionService.getSubmission(PRODUCT_ID, FORM_ID, SUBMISSION_ID))
                .thenThrow(new SubmissionNotFoundException(SUBMISSION_ID));

        mockMvc.perform(get("/api/v1/products/{productId}/forms/{formId}/submissions/{submissionId}",
                        PRODUCT_ID, FORM_ID, SUBMISSION_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SUBMISSION_NOT_FOUND"));
    }

    @Test
    void shouldReturnModuleDisabledWhenFormsModuleIsDisabled() throws Exception {
        when(productModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.FORMS))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/products/{productId}/forms/submissions", PRODUCT_ID).with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.moduleKey").value("FORMS"));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("subject-1", "user@aegis.app", "user", "User", Set.of("ROLE_EDITOR"));
    }

    private SubmissionSummary summary() {
        return new SubmissionSummary(SUBMISSION_ID, "2026-06-27T10:00:00Z", "Ana", "ana@example.com",
                "site", "new", "—", "90");
    }

    private SubmissionDetail detail() {
        return new SubmissionDetail(SUBMISSION_ID, FORM_ID, OffsetDateTime.parse("2026-06-27T10:00:00Z"),
                "Ana", "ana@example.com", "site", "new", "—", 90,
                Map.of("Email", "ana@example.com", "Curriculo", "44444444-4444-4444-4444-444444444444"),
                OffsetDateTime.parse("2026-06-27T10:01:00Z"));
    }
}

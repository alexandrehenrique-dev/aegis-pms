package br.com.byop.aegis.submission.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.module.ModuleAccessAspect;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.security.SecurityConfig;
import br.com.byop.aegis.submission.dto.SubmissionDetail;
import br.com.byop.aegis.submission.exception.InvalidSubmissionException;
import br.com.byop.aegis.submission.exception.SubmissionExceptionHandler;
import br.com.byop.aegis.submission.exception.TooManySubmissionsException;
import br.com.byop.aegis.submission.service.PublicSubmissionRateLimiter;
import br.com.byop.aegis.submission.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicSubmissionController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        SubmissionExceptionHandler.class,
        ModuleAccessAspect.class
})
class PublicSubmissionControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FORM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SUBMISSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmissionService submissionService;

    @MockitoBean
    private PublicSubmissionRateLimiter rateLimiter;

    @MockitoBean
    private ProductModuleRepository productModuleRepository;

    @BeforeEach
    void setUp() {
        when(productModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.FORMS))
                .thenReturn(true);
    }

    @Test
    void shouldSubmitWithoutAuthorizationHeader() throws Exception {
        when(submissionService.submit(eq(PRODUCT_ID), eq(FORM_ID), any())).thenReturn(detail());

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/submit", PRODUCT_ID, FORM_ID)
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ana",
                                  "email": "ana@example.com",
                                  "source": "site",
                                  "answers": { "Email": "ana@example.com" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SUBMISSION_ID.toString()));

        verify(rateLimiter).assertAllowed("203.0.113.10", FORM_ID);
    }

    @Test
    void shouldUseRemoteAddressWhenForwardedForHeaderIsAbsent() throws Exception {
        when(submissionService.submit(eq(PRODUCT_ID), eq(FORM_ID), any())).thenReturn(detail());

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/submit", PRODUCT_ID, FORM_ID)
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.20");
                            return request;
                        })
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ana",
                                  "email": "ana@example.com",
                                  "source": "site",
                                  "answers": { "Email": "ana@example.com" }
                                }
                                """))
                .andExpect(status().isCreated());

        verify(rateLimiter).assertAllowed("198.51.100.20", FORM_ID);
    }

    @Test
    void shouldUseRemoteAddressWhenForwardedForHeaderIsBlank() throws Exception {
        when(submissionService.submit(eq(PRODUCT_ID), eq(FORM_ID), any())).thenReturn(detail());

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/submit", PRODUCT_ID, FORM_ID)
                        .header("X-Forwarded-For", " ")
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.30");
                            return request;
                        })
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ana",
                                  "email": "ana@example.com",
                                  "source": "site",
                                  "answers": { "Email": "ana@example.com" }
                                }
                                """))
                .andExpect(status().isCreated());

        verify(rateLimiter).assertAllowed("198.51.100.30", FORM_ID);
    }

    @Test
    void shouldReturnUnprocessableContentWhenFormIsNotPublished() throws Exception {
        when(submissionService.submit(eq(PRODUCT_ID), eq(FORM_ID), any()))
                .thenThrow(new InvalidSubmissionException(SubmissionService.FORM_NOT_PUBLISHED_ERROR));

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/submit", PRODUCT_ID, FORM_ID)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ana",
                                  "email": "ana@example.com",
                                  "source": "site",
                                  "answers": { "Email": "ana@example.com" }
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value(SubmissionService.FORM_NOT_PUBLISHED_ERROR));
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
        doThrow(new TooManySubmissionsException()).when(rateLimiter).assertAllowed(any(), eq(FORM_ID));

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/submit", PRODUCT_ID, FORM_ID)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ana",
                                  "email": "ana@example.com",
                                  "source": "site",
                                  "answers": { "Email": "ana@example.com" }
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_SUBMISSIONS"));
    }

    @Test
    void shouldReturnModuleDisabledWhenFormsModuleIsDisabled() throws Exception {
        when(productModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(PRODUCT_ID, ModuleKey.FORMS))
                .thenReturn(false);

        mockMvc.perform(post("/api/v1/products/{productId}/forms/{formId}/submit", PRODUCT_ID, FORM_ID)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"));
    }

    private SubmissionDetail detail() {
        return new SubmissionDetail(SUBMISSION_ID, FORM_ID, OffsetDateTime.parse("2026-06-29T12:00:00Z"),
                "Ana", "ana@example.com", "site", "new", "—", null,
                Map.of("Email", "ana@example.com"), null);
    }
}

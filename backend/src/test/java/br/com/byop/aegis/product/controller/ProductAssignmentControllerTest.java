package br.com.byop.aegis.product.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.product.dto.ProductAssignmentSummary;
import br.com.byop.aegis.product.exception.InvalidProductAssignmentException;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.service.ProductAssignmentService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductAssignmentController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class
})
class ProductAssignmentControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASSIGNMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductAssignmentService productAssignmentService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldListProductUsers() throws Exception {
        ProductAssignmentSummary summary = assignmentSummary("user-1", "Editor User", "editor@byop.dev", "atribuido");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller());
        when(productAssignmentService.listAssignments(any(AuthenticatedUser.class), eq(PRODUCT_ID))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/products/{productId}/users", PRODUCT_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ASSIGNMENT_ID.toString()))
                .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$[0].productName").value("Aegis PMS"))
                .andExpect(jsonPath("$[0].userSubject").value("user-1"))
                .andExpect(jsonPath("$[0].userName").value("Editor User"))
                .andExpect(jsonPath("$[0].userEmail").value("editor@byop.dev"))
                .andExpect(jsonPath("$[0].role").value("EDITOR"))
                .andExpect(jsonPath("$[0].status").value("atribuido"));
    }

    @Test
    void shouldAssignExistingUser() throws Exception {
        ProductAssignmentSummary summary = assignmentSummary("user-1", "Editor User", "editor@byop.dev", "atribuido");
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller());
        when(productAssignmentService.assignUser(any(AuthenticatedUser.class), eq(PRODUCT_ID), any())).thenReturn(summary);

        mockMvc.perform(post("/api/v1/products/{productId}/users", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "22222222-2222-2222-2222-222222222222",
                                  "tenantId": "11111111-1111-1111-1111-111111111111",
                                  "userId": "user-1",
                                  "role": "EDITOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userSubject").value("user-1"))
                .andExpect(jsonPath("$.userEmail").value("editor@byop.dev"))
                .andExpect(jsonPath("$.status").value("atribuido"));
    }

    @Test
    void shouldInviteUserByEmail() throws Exception {
        ProductAssignmentSummary summary = assignmentSummary(
                "invite:guest@byop.dev",
                null,
                "guest@byop.dev",
                "convidado"
        );
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller());
        when(productAssignmentService.assignUser(any(AuthenticatedUser.class), eq(PRODUCT_ID), any())).thenReturn(summary);

        mockMvc.perform(post("/api/v1/products/{productId}/users", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "22222222-2222-2222-2222-222222222222",
                                  "tenantId": "11111111-1111-1111-1111-111111111111",
                                  "inviteEmail": "guest@byop.dev",
                                  "role": "VIEWER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userSubject").value("invite:guest@byop.dev"))
                .andExpect(jsonPath("$.userEmail").value("guest@byop.dev"))
                .andExpect(jsonPath("$.status").value("convidado"));
    }

    @Test
    void shouldRejectUserIdAndInviteEmailTogether() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller());
        when(productAssignmentService.assignUser(any(AuthenticatedUser.class), eq(PRODUCT_ID), any()))
                .thenThrow(new InvalidProductAssignmentException("Exactly one of userId or inviteEmail is required"));

        mockMvc.perform(post("/api/v1/products/{productId}/users", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "22222222-2222-2222-2222-222222222222",
                                  "tenantId": "11111111-1111-1111-1111-111111111111",
                                  "userId": "user-1",
                                  "inviteEmail": "guest@byop.dev",
                                  "role": "EDITOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PRODUCT_ASSIGNMENT"));
    }

    @Test
    void shouldRejectMissingUserIdAndInviteEmail() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller());
        when(productAssignmentService.assignUser(any(AuthenticatedUser.class), eq(PRODUCT_ID), any()))
                .thenThrow(new InvalidProductAssignmentException("Exactly one of userId or inviteEmail is required"));

        mockMvc.perform(post("/api/v1/products/{productId}/users", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "22222222-2222-2222-2222-222222222222",
                                  "tenantId": "11111111-1111-1111-1111-111111111111",
                                  "role": "EDITOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PRODUCT_ASSIGNMENT"));
    }

    @Test
    void shouldRemoveProductUser() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{productId}/users/{userId}", PRODUCT_ID, "user-1")
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/users", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": null,
                                  "tenantId": null,
                                  "role": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUEST_VALIDATION_FAILED"));
    }

    @Test
    void shouldReturnBadRequestWhenServiceRejectsDelete() throws Exception {
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller());
        doThrow(new InvalidProductAssignmentException("Invalid product assignment"))
                .when(productAssignmentService)
                .removeAssignment(any(AuthenticatedUser.class), eq(PRODUCT_ID), eq("user-1"));

        mockMvc.perform(delete("/api/v1/products/{productId}/users/{userId}", PRODUCT_ID, "user-1")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PRODUCT_ASSIGNMENT"));
    }

    private AuthenticatedUser caller() {
        return new AuthenticatedUser("admin-subject", "admin@byop.dev", "admin", "Admin", Set.of("ROLE_SUPER_ADMIN"));
    }

    private ProductAssignmentSummary assignmentSummary(String userSubject, String userName, String userEmail,
                                                       String status) {
        return new ProductAssignmentSummary(
                ASSIGNMENT_ID,
                TENANT_ID,
                PRODUCT_ID,
                "Aegis PMS",
                userSubject,
                userName,
                userEmail,
                "EDITOR",
                status,
                OffsetDateTime.parse("2026-06-26T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-26T10:10:00-03:00")
        );
    }
}

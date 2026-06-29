package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityDemoUserServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private IdentityDemoUserService service;

    @Test
    void shouldEnsureDemoUserWithCanonicalKeycloakRole() {
        when(keycloakAdminClient.ensureDemoUser(
                "admin@byop.io",
                "Ana Martins",
                "senha123",
                "AEGIS_TENANT_ADMIN"
        )).thenReturn(new UserResponse(
                "user-id",
                "admin@byop.io",
                "admin@byop.io",
                "Ana",
                "Martins",
                true
        ));

        IdentityUser user = service.ensureDemoUser("admin@byop.io", "Ana Martins", "senha123", "TENANT_ADMIN");

        assertThat(user.id()).isEqualTo("user-id");
        assertThat(user.email()).isEqualTo("admin@byop.io");
        verify(keycloakAdminClient).ensureDemoUser(
                "admin@byop.io",
                "Ana Martins",
                "senha123",
                "AEGIS_TENANT_ADMIN"
        );
    }
}

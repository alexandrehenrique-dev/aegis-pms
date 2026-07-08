package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityUserLifecycleServiceTest {

    private final KeycloakAdminClient keycloakAdminClient = mock(KeycloakAdminClient.class);
    private final IdentityUserLifecycleService service = new IdentityUserLifecycleService(keycloakAdminClient);

    @Test
    void shouldMapLifecycleOperations() {
        UserResponse response = new UserResponse("user-1", "guest", "guest@byop.dev", "Guest", "User", true);
        when(keycloakAdminClient.findUserById("user-1")).thenReturn(response);
        when(keycloakAdminClient.findUserByEmail("guest@byop.dev")).thenReturn(Optional.of(response));
        when(keycloakAdminClient.inviteUser("guest@byop.dev", "Guest User")).thenReturn(response);

        assertThat(service.getRequiredUser("user-1").displayName()).isEqualTo("Guest User");
        assertThat(service.findByEmail("guest@byop.dev")).isPresent();
        assertThat(service.invite("guest@byop.dev", "Guest User").id()).isEqualTo("user-1");

        service.executeActionsEmail("user-1", List.of("UPDATE_PASSWORD"));
        service.setUserEnabled("user-1", false);
        service.assignRealmRole("user-1", "AEGIS_PRODUCT_MANAGER");

        verify(keycloakAdminClient).executeActionsEmail("user-1", List.of("UPDATE_PASSWORD"));
        verify(keycloakAdminClient).setUserEnabled("user-1", false);
        verify(keycloakAdminClient).assignRealmRole("user-1", "AEGIS_PRODUCT_MANAGER");
    }
}

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
class IdentityInvitationServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private IdentityInvitationService service;

    @Test
    void shouldInviteByEmailAndReturnIdentityUser() {
        when(keycloakAdminClient.inviteUser("guest@byop.dev", "guest@byop.dev"))
                .thenReturn(new UserResponse("user-1", "guest", "guest@byop.dev", "Guest", "User", true));

        IdentityUser user = service.inviteByEmail("guest@byop.dev");

        assertThat(user.id()).isEqualTo("user-1");
        assertThat(user.username()).isEqualTo("guest");
        assertThat(user.email()).isEqualTo("guest@byop.dev");
        assertThat(user.displayName()).isEqualTo("Guest User");
    }

    @Test
    void shouldInviteByEmailAndName() {
        when(keycloakAdminClient.inviteUser("guest@byop.dev", "Guest User"))
                .thenReturn(new UserResponse("user-1", "guest", "guest@byop.dev", "Guest", "User", true));

        IdentityUser user = service.inviteByEmail("guest@byop.dev", "Guest User");

        assertThat(user.displayName()).isEqualTo("Guest User");
    }

    @Test
    void shouldInviteByEmailAndAssignRealmRole() {
        when(keycloakAdminClient.inviteUser("pm@byop.dev", "Product Manager"))
                .thenReturn(new UserResponse("user-1", "pm", "pm@byop.dev", "Product", "Manager", true));

        IdentityUser user = service.inviteByEmail("pm@byop.dev", "Product Manager", "AEGIS_PRODUCT_MANAGER");

        assertThat(user.id()).isEqualTo("user-1");
        verify(keycloakAdminClient).assignRealmRole("user-1", "AEGIS_PRODUCT_MANAGER");
    }
}

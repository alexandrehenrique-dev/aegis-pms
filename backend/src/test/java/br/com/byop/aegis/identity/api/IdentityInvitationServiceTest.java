package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityInvitationServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private IdentityInvitationService service;

    @Test
    void shouldInviteByEmailAndReturnIdentityUser() {
        when(keycloakAdminClient.inviteUserByEmail("guest@byop.dev"))
                .thenReturn(new UserResponse("user-1", "guest", "guest@byop.dev", "Guest", "User", true));

        IdentityUser user = service.inviteByEmail("guest@byop.dev");

        assertThat(user.id()).isEqualTo("user-1");
        assertThat(user.username()).isEqualTo("guest");
        assertThat(user.email()).isEqualTo("guest@byop.dev");
        assertThat(user.displayName()).isEqualTo("Guest User");
    }
}

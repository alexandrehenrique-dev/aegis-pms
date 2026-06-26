package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityInvitationService;
import br.com.byop.aegis.identity.api.IdentityUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakProductAssignmentInvitePortTest {

    @Mock
    private IdentityInvitationService invitationService;

    @InjectMocks
    private KeycloakProductAssignmentInvitePort port;

    @Test
    void shouldReturnInvitedIdentityUser() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        IdentityUser invited = new IdentityUser("user-1", "guest", "guest@byop.dev", null, null);
        when(invitationService.inviteByEmail("guest@byop.dev")).thenReturn(invited);

        IdentityUser user = port.invite(tenantId, productId, "guest@byop.dev");

        assertThat(user).isEqualTo(invited);
    }
}

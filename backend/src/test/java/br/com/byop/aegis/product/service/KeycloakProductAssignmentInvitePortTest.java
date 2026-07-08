package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityActionInviteCommand;
import br.com.byop.aegis.identity.api.IdentityActionTokenService;
import br.com.byop.aegis.identity.api.IdentityInvitationService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import br.com.byop.aegis.tenant.api.TenantReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakProductAssignmentInvitePortTest {

    @Mock
    private IdentityInvitationService invitationService;

    @Mock
    private IdentityActionTokenService actionTokenService;

    @Mock
    private TenantAccessService tenantAccessService;

    @Test
    void shouldReturnInvitedIdentityUser() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        IdentityUser invited = new IdentityUser("user-1", "guest", "guest@byop.dev", null, null);
        when(invitationService.inviteByEmail("guest@byop.dev")).thenReturn(invited);
        KeycloakProductAssignmentInvitePort port =
                new KeycloakProductAssignmentInvitePort(invitationService, actionTokenService, tenantAccessService);

        IdentityUser user = port.invite(tenantId, productId, "guest@byop.dev");

        assertThat(user).isEqualTo(invited);
    }

    @Test
    void shouldSendInviteActivationTokenWithProductContext() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        IdentityUser invited = new IdentityUser("user-1", "guest", "guest@byop.dev", "Guest", "User");
        when(invitationService.inviteByEmail("guest@byop.dev", "guest@byop.dev", "AEGIS_EDITOR")).thenReturn(invited);
        when(tenantAccessService.getRequiredReference(tenantId))
                .thenReturn(new TenantReference(tenantId, "BYOP"));
        KeycloakProductAssignmentInvitePort port =
                new KeycloakProductAssignmentInvitePort(invitationService, actionTokenService, tenantAccessService);

        IdentityUser user = port.invite(tenantId, productId, "aegis", "Aegis", "guest@byop.dev", "EDITOR", "Admin");

        ArgumentCaptor<IdentityActionInviteCommand> captor = ArgumentCaptor.forClass(IdentityActionInviteCommand.class);
        verify(actionTokenService).sendInviteActivation(captor.capture());
        assertThat(user).isEqualTo(invited);
        assertThat(captor.getValue().tenantName()).isEqualTo("BYOP");
        assertThat(captor.getValue().productNames()).containsExactly("Aegis");
        assertThat(captor.getValue().productSlug()).isEqualTo("aegis");
        assertThat(captor.getValue().role()).isEqualTo("EDITOR");
        assertThat(captor.getValue().inviterName()).isEqualTo("Admin");
        verify(invitationService).inviteByEmail("guest@byop.dev", "guest@byop.dev", "AEGIS_EDITOR");
    }
}

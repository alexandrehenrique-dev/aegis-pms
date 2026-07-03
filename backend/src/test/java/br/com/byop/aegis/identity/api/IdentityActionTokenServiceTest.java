package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.domain.AuthActionType;
import br.com.byop.aegis.identity.auth.service.AuthActionEmailService;
import br.com.byop.aegis.identity.auth.service.AuthActionTokenService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityActionTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");

    private final AuthActionTokenService tokenService = mock(AuthActionTokenService.class);
    private final AuthActionEmailService emailService = mock(AuthActionEmailService.class);
    private final IdentityActionTokenService service = new IdentityActionTokenService(tokenService, emailService);

    @Test
    void shouldCreateAndSendInviteActivationToken() {
        IdentityActionInviteCommand command = new IdentityActionInviteCommand(
                "user-id",
                "user@byop.dev",
                "User Name",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "BYOP",
                List.of("Aegis"),
                "EDITOR",
                "Admin"
        );
        AuthActionToken token = new AuthActionToken(
                "user-id",
                "user@byop.dev",
                "User Name",
                AuthActionType.INVITE,
                NOW,
                NOW.plusSeconds(3600)
        );
        when(tokenService.createInvite(command)).thenReturn(token);

        AuthActionToken result = service.sendInviteActivation(command);

        assertThat(result).isEqualTo(token);
        verify(emailService).sendInviteActivation(token);
    }
}

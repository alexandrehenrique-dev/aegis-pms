package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.domain.AuthActionType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthActionEmailServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AuthActionTokenService tokenService = mock(AuthActionTokenService.class);
    private final AuthActionEmailService service = new AuthActionEmailService(
            mailSender,
            tokenService,
            "../infra/keycloak/themes/aegis/email/html",
            "noreply@aegis.app",
            "http://localhost:5173/"
    );

    @Test
    void shouldRenderAndSendInviteTemplate() {
        MimeMessage message = mimeMessage();
        AuthActionToken token = inviteToken();
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(tokenService.productNames(token)).thenReturn(List.of("Aegis"));

        service.sendInviteActivation(token);

        verify(mailSender).send(message);
    }

    @Test
    void shouldRenderAndSendPasswordResetTemplate() {
        MimeMessage message = mimeMessage();
        AuthActionToken token = new AuthActionToken(
                "user-id",
                "user@byop.dev",
                "User",
                AuthActionType.PASSWORD_RESET,
                NOW,
                NOW.plusSeconds(3600)
        );
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendPasswordReset(token);

        verify(mailSender).send(message);
    }

    @Test
    void shouldRejectInvalidTemplateDirectory() {
        String missingDirectory = "/path/that/does/not/exist/aegis-mail";

        assertThatThrownBy(() -> new AuthActionEmailService(
                mailSender,
                tokenService,
                missingDirectory,
                "noreply@aegis.app",
                "http://localhost:5173"
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectMissingTemplate(@TempDir Path templateDirectory) {
        AuthActionEmailService brokenService = new AuthActionEmailService(
                mailSender,
                tokenService,
                templateDirectory.toString(),
                "noreply@aegis.app",
                "http://localhost:5173"
        );
        AuthActionToken token = new AuthActionToken(
                "user-id",
                "user@byop.dev",
                "User",
                AuthActionType.PASSWORD_RESET,
                NOW,
                NOW.plusSeconds(3600)
        );

        assertThatThrownBy(() -> brokenService.sendPasswordReset(token))
                .isInstanceOf(IllegalStateException.class);
    }

    private MimeMessage mimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private AuthActionToken inviteToken() {
        AuthActionToken token = new AuthActionToken(
                "user-id",
                "guest@byop.dev",
                "Guest",
                AuthActionType.INVITE,
                NOW,
                NOW.plusSeconds(3600)
        );
        token.addInviteContext(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "BYOP",
                "[\"Aegis\"]",
                null,
                "EDITOR",
                "Admin"
        );
        return token;
    }
}

package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.api.IdentityAuthActionAuditEvent;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserInviteActivatedEvent;
import br.com.byop.aegis.identity.api.IdentityUserLifecycleService;
import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.domain.AuthActionType;
import br.com.byop.aegis.identity.auth.dto.AuthInviteValidationResponse;
import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.auth.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthActivationServiceTest {

    private static final UUID TOKEN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");

    private final AuthActionTokenService tokenService = mock(AuthActionTokenService.class);
    private final AuthActionEmailService emailService = mock(AuthActionEmailService.class);
    private final PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);
    private final KeycloakAdminClient keycloakAdminClient = mock(KeycloakAdminClient.class);
    private final IdentityUserLifecycleService userLifecycleService = mock(IdentityUserLifecycleService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AuthActivationService service = new AuthActivationService(
            tokenService,
            emailService,
            passwordPolicy,
            keycloakAdminClient,
            userLifecycleService,
            eventPublisher
    );

    @Test
    void shouldValidateInviteRequiringPasswordSetupForNewUser() {
        AuthActionToken token = inviteToken();
        when(tokenService.validateInvite(TOKEN_ID)).thenReturn(token);
        when(tokenService.productNames(token)).thenReturn(List.of("Aegis"));
        when(keycloakAdminClient.hasRequiredAction("user-id", "UPDATE_PASSWORD")).thenReturn(true);

        AuthInviteValidationResponse response = service.validateInvite(TOKEN_ID);

        assertThat(response.userName()).isEqualTo("Guest");
        assertThat(response.userEmail()).isEqualTo("guest@byop.dev");
        assertThat(response.tenantName()).isEqualTo("BYOP");
        assertThat(response.productNames()).containsExactly("Aegis");
        assertThat(response.productSlug()).isNull();
        assertThat(response.role()).isEqualTo("EDITOR");
        assertThat(response.inviterName()).isEqualTo("Admin");
        assertThat(response.requiresPasswordSetup()).isTrue();
    }

    @Test
    void shouldValidateInviteNotRequiringPasswordSetupForExistingUser() {
        AuthActionToken token = inviteToken();
        when(tokenService.validateInvite(TOKEN_ID)).thenReturn(token);
        when(tokenService.productNames(token)).thenReturn(List.of("Aegis"));
        when(keycloakAdminClient.hasRequiredAction("user-id", "UPDATE_PASSWORD")).thenReturn(false);

        AuthInviteValidationResponse response = service.validateInvite(TOKEN_ID);

        assertThat(response.requiresPasswordSetup()).isFalse();
    }

    @Test
    void shouldAcceptExistingUserInvite() {
        AuthActionToken token = inviteToken();
        when(tokenService.consumeInvite(TOKEN_ID)).thenReturn(token);

        AuthMessageResponse response = service.acceptExistingUser(TOKEN_ID);

        assertThat(response.message()).isEqualTo("Convite aceito. Faça login para acessar o produto.");
        verify(keycloakAdminClient).assignRealmRole("user-id", "AEGIS_EDITOR");
        verify(keycloakAdminClient).clearRequiredActions("user-id");
        verify(eventPublisher).publishEvent(any(IdentityUserInviteActivatedEvent.class));
        assertPublishedAuditAction("USER_INVITE_ACCEPTED_EXISTING");
    }

    @Test
    void shouldActivateInvite() {
        AuthActionToken token = inviteToken();
        when(tokenService.consumeInvite(TOKEN_ID)).thenReturn(token);

        AuthMessageResponse response = service.activate(TOKEN_ID, "Senha123", "Alexandre", "Henrique");

        assertThat(response.message()).isEqualTo("Conta ativada. Faça login para continuar.");
        verify(passwordPolicy).assertStrong("Senha123");
        verify(keycloakAdminClient).resetPassword("user-id", "Senha123");
        verify(keycloakAdminClient).updateUserProfile("user-id", "Alexandre", "Henrique");
        verify(keycloakAdminClient).assignRealmRole("user-id", "AEGIS_EDITOR");
        verify(keycloakAdminClient).setUserEnabled("user-id", true);
        verify(keycloakAdminClient).clearRequiredActions("user-id");
        verify(eventPublisher).publishEvent(any(IdentityUserInviteActivatedEvent.class));
        assertPublishedAuditAction("USER_INVITE_ACTIVATED");
    }

    @Test
    void shouldAcceptExistingInviteWithoutAssigningRealmRoleWhenTokenHasNoRole() {
        AuthActionToken token = inviteTokenWithoutContext();
        when(tokenService.consumeInvite(TOKEN_ID)).thenReturn(token);

        AuthMessageResponse response = service.acceptExistingUser(TOKEN_ID);

        assertThat(response.message()).isEqualTo("Convite aceito. Faça login para acessar o produto.");
        verify(keycloakAdminClient, never()).assignRealmRole(anyString(), anyString());
        verify(keycloakAdminClient).clearRequiredActions("user-id");
        assertPublishedAuditAction("USER_INVITE_ACCEPTED_EXISTING");
    }

    @Test
    void shouldActivateInviteWithoutAssigningRealmRoleWhenTokenRoleIsBlank() {
        AuthActionToken token = inviteTokenWithRole(" ");
        when(tokenService.consumeInvite(TOKEN_ID)).thenReturn(token);

        service.activate(TOKEN_ID, "Senha123", "Alexandre", "Henrique");

        verify(keycloakAdminClient, never()).assignRealmRole(anyString(), anyString());
        verify(keycloakAdminClient).clearRequiredActions("user-id");
        assertPublishedAuditAction("USER_INVITE_ACTIVATED");
    }

    @Test
    void shouldRequestPasswordResetWithoutRevealingMissingEmail() {
        when(userLifecycleService.findByEmail("missing@byop.dev")).thenReturn(Optional.empty());

        AuthMessageResponse response = service.requestPasswordReset("missing@byop.dev");

        assertThat(response.message())
                .isEqualTo("Se este e-mail existe na plataforma, um link de recuperação será enviado.");
        verifyNoInteractions(emailService);
    }

    @Test
    void shouldRequestPasswordResetForExistingEmail() {
        IdentityUser user = new IdentityUser("user-id", "user", "user@byop.dev", "User", "Name");
        AuthActionToken token = resetToken();
        when(userLifecycleService.findByEmail("user@byop.dev")).thenReturn(Optional.of(user));
        when(tokenService.createPasswordReset(user)).thenReturn(token);

        service.requestPasswordReset("user@byop.dev");

        verify(emailService).sendPasswordReset(token);
        assertPublishedAuditAction("PASSWORD_RESET_REQUESTED");
    }

    @Test
    void shouldConfirmPasswordReset() {
        AuthActionToken token = resetToken();
        when(tokenService.consumePasswordReset(TOKEN_ID)).thenReturn(token);

        AuthMessageResponse response = service.confirmPasswordReset(TOKEN_ID, "NovaSenha456");

        assertThat(response.message()).isEqualTo("Senha redefinida. Faça login para continuar.");
        verify(passwordPolicy).assertStrong("NovaSenha456");
        verify(keycloakAdminClient).resetPassword("user-id", "NovaSenha456");
        assertPublishedAuditAction("PASSWORD_RESET_COMPLETED");
    }

    @Test
    void shouldPropagateWeakPasswordBeforeConsumingToken() {
        org.mockito.Mockito.doThrow(new WeakPasswordException(PasswordPolicy.WEAK_CREDENTIAL_MESSAGE))
                .when(passwordPolicy).assertStrong("fraca");

        assertThatThrownBy(() -> service.activate(TOKEN_ID, "fraca", "Alexandre", "Henrique"))
                .isInstanceOf(WeakPasswordException.class);

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void shouldMapKeycloakPasswordPolicyRejection() {
        AuthActionToken token = inviteToken();
        when(tokenService.consumeInvite(TOKEN_ID)).thenReturn(token);
        org.mockito.Mockito.doThrow(new KeycloakAuthenticationException("weak"))
                .when(keycloakAdminClient).resetPassword("user-id", "Senha123");

        assertThatThrownBy(() -> service.activate(TOKEN_ID, "Senha123", "Alexandre", "Henrique"))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessage(PasswordPolicy.WEAK_CREDENTIAL_MESSAGE);
    }

    private AuthActionToken inviteToken() {
        return inviteTokenWithRole("EDITOR");
    }

    private AuthActionToken inviteTokenWithRole(String role) {
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
                role,
                "Admin"
        );
        return token;
    }

    private AuthActionToken inviteTokenWithoutContext() {
        return new AuthActionToken(
                "user-id",
                "guest@byop.dev",
                "Guest",
                AuthActionType.INVITE,
                NOW,
                NOW.plusSeconds(3600)
        );
    }

    private AuthActionToken resetToken() {
        return new AuthActionToken(
                "user-id",
                "user@byop.dev",
                "User Name",
                AuthActionType.PASSWORD_RESET,
                NOW,
                NOW.plusSeconds(3600)
        );
    }

    private void assertPublishedAuditAction(String action) {
        // Usa atLeastOnce() para ser robusto quando activate() publica múltiplos
        // eventos (IdentityUserInviteActivatedEvent + IdentityAuthActionAuditEvent).
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(IdentityAuthActionAuditEvent.class::isInstance)
                .extracting(e -> ((IdentityAuthActionAuditEvent) e).action())
                .containsExactly(action);
    }
}

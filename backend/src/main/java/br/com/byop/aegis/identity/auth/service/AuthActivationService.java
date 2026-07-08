package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.api.IdentityAuthActionAuditEvent;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserInviteActivatedEvent;
import br.com.byop.aegis.identity.api.IdentityUserLifecycleService;
import br.com.byop.aegis.identity.auth.client.KeycloakAdminClient;
import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.dto.AuthInviteValidationResponse;
import br.com.byop.aegis.identity.auth.dto.AuthMessageResponse;
import br.com.byop.aegis.identity.auth.exception.KeycloakAuthenticationException;
import br.com.byop.aegis.identity.auth.exception.WeakPasswordException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AuthActivationService {

    private static final String ACTIVATE_MESSAGE = "Conta ativada. Faça login para continuar.";
    private static final String ACCEPT_EXISTING_MESSAGE = "Convite aceito. Faça login para acessar o produto.";
    private static final String RESET_REQUEST_MESSAGE =
            "Se este e-mail existe na plataforma, um link de recuperação será enviado.";
    private static final String RESET_CONFIRM_MESSAGE = "Senha redefinida. Faça login para continuar.";
    private static final String UPDATE_PASSWORD_ACTION = "UPDATE_PASSWORD";
    private static final String KEYCLOAK_ROLE_PREFIX = "AEGIS_";

    private final AuthActionTokenService tokenService;
    private final AuthActionEmailService emailService;
    private final PasswordPolicy passwordPolicy;
    private final KeycloakAdminClient keycloakAdminClient;
    private final IdentityUserLifecycleService userLifecycleService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthActivationService(AuthActionTokenService tokenService,
                                 AuthActionEmailService emailService,
                                 PasswordPolicy passwordPolicy,
                                 KeycloakAdminClient keycloakAdminClient,
                                 IdentityUserLifecycleService userLifecycleService,
                                 ApplicationEventPublisher eventPublisher) {
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordPolicy = passwordPolicy;
        this.keycloakAdminClient = keycloakAdminClient;
        this.userLifecycleService = userLifecycleService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public AuthInviteValidationResponse validateInvite(UUID tokenId) {
        log.debug("validateInvite: validando convite");
        AuthActionToken token = tokenService.validateInvite(tokenId);
        boolean requiresPasswordSetup = keycloakAdminClient.hasRequiredAction(
                token.getKeycloakId(), UPDATE_PASSWORD_ACTION);
        return new AuthInviteValidationResponse(
                token.getUserName(),
                token.getUserEmail(),
                token.getTenantName(),
                tokenService.productNames(token),
                token.getProductSlug(),
                token.getRole(),
                token.getInviterName(),
                token.getExpiresAt(),
                requiresPasswordSetup
        );
    }

    /**
     * O.1 (BUG-SPRINT-05) — {@code firstName}/{@code lastName} agora
     * obrigatorios e persistidos no Keycloak antes de habilitar a conta.
     * Sem isso, um convite criado com nome vazio ou de uma so palavra
     * deixava o perfil incompleto no Keycloak e o realm bloqueava o login
     * ("Account is not fully set up") mesmo com a conta {@code enabled}.
     */
    @Transactional
    public AuthMessageResponse activate(UUID tokenId, String password, String firstName, String lastName) {
        log.debug("activate: ativando conta via token de convite");
        passwordPolicy.assertStrong(password);
        AuthActionToken token = tokenService.consumeInvite(tokenId);
        updatePassword(token.getKeycloakId(), password);
        keycloakAdminClient.updateUserProfile(token.getKeycloakId(), firstName, lastName);
        assignRealmRole(token);
        keycloakAdminClient.setUserEnabled(token.getKeycloakId(), true);
        keycloakAdminClient.clearRequiredActions(token.getKeycloakId());
        eventPublisher.publishEvent(new IdentityUserInviteActivatedEvent(token.getKeycloakId()));
        audit(token, "USER_INVITE_ACTIVATED");
        log.info("activate: conta ativada para keycloakId='{}'", token.getKeycloakId());
        return new AuthMessageResponse(ACTIVATE_MESSAGE);
    }

    /**
     * Aceite de convite por usuário que já tem conta ativa no Keycloak (sem
     * {@code UPDATE_PASSWORD} pendente) — não passa por {@link #activate}
     * porque não há senha para definir. Consome o token e publica o mesmo
     * evento de ativação para transicionar assignments/memberships de
     * INVITED para ASSIGNED/ACTIVE.
     */
    @Transactional
    public AuthMessageResponse acceptExistingUser(UUID tokenId) {
        log.debug("acceptExistingUser: aceitando convite de usuário existente");
        AuthActionToken token = tokenService.consumeInvite(tokenId);
        assignRealmRole(token);
        keycloakAdminClient.clearRequiredActions(token.getKeycloakId());
        eventPublisher.publishEvent(new IdentityUserInviteActivatedEvent(token.getKeycloakId()));
        audit(token, "USER_INVITE_ACCEPTED_EXISTING");
        log.info("acceptExistingUser: convite aceito para keycloakId='{}'", token.getKeycloakId());
        return new AuthMessageResponse(ACCEPT_EXISTING_MESSAGE);
    }

    @Transactional
    public AuthMessageResponse requestPasswordReset(String email) {
        log.debug("requestPasswordReset: email='{}'", email);
        userLifecycleService.findByEmail(email)
                .ifPresent(this::sendPasswordReset);
        return new AuthMessageResponse(RESET_REQUEST_MESSAGE);
    }

    @Transactional
    public AuthMessageResponse confirmPasswordReset(UUID tokenId, String password) {
        log.debug("confirmPasswordReset: confirmando redefinicao de senha");
        passwordPolicy.assertStrong(password);
        AuthActionToken token = tokenService.consumePasswordReset(tokenId);
        updatePassword(token.getKeycloakId(), password);
        audit(token, "PASSWORD_RESET_COMPLETED");
        log.info("confirmPasswordReset: senha redefinida para keycloakId='{}'", token.getKeycloakId());
        return new AuthMessageResponse(RESET_CONFIRM_MESSAGE);
    }

    private void sendPasswordReset(IdentityUser user) {
        AuthActionToken token = tokenService.createPasswordReset(user);
        emailService.sendPasswordReset(token);
        audit(token, "PASSWORD_RESET_REQUESTED");
        log.info("requestPasswordReset: e-mail de redefinicao enviado para keycloakId='{}'", token.getKeycloakId());
    }

    private void updatePassword(String keycloakId, String password) {
        try {
            keycloakAdminClient.resetPassword(keycloakId, password);
        } catch (KeycloakAuthenticationException _) {
            log.warn("updatePassword: senha rejeitada pelo Keycloak para keycloakId='{}'", keycloakId);
            throw new WeakPasswordException(PasswordPolicy.WEAK_CREDENTIAL_MESSAGE);
        }
    }

    private void assignRealmRole(AuthActionToken token) {
        if (token.getRole() != null && !token.getRole().isBlank()) {
            keycloakAdminClient.assignRealmRole(token.getKeycloakId(), KEYCLOAK_ROLE_PREFIX + token.getRole());
        }
    }

    private void audit(AuthActionToken token, String action) {
        eventPublisher.publishEvent(new IdentityAuthActionAuditEvent(
                token.getTenantId(),
                token.getKeycloakId(),
                action,
                token.getKeycloakId(),
                token.getUserName(),
                Map.of("tokenId", token.getId().toString())
        ));
    }
}

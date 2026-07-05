package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.service.AuthActionEmailService;
import br.com.byop.aegis.identity.auth.service.AuthActionTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IdentityActionTokenService {

    private final AuthActionTokenService tokenService;
    private final AuthActionEmailService emailService;

    public IdentityActionTokenService(AuthActionTokenService tokenService, AuthActionEmailService emailService) {
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    /**
     * Cria o token de convite (dentro da transação corrente) e envia o e-mail.
     * O envio de e-mail é executado em bloco try-catch separado para garantir
     * que uma falha de SMTP/template NÃO faça rollback da TenantMembership
     * ou do AuthActionToken — o admin pode reenviar o convite manualmente.
     */
    public AuthActionToken sendInviteActivation(IdentityActionInviteCommand command) {
        log.debug("sendInviteActivation: keycloakId='{}', tenantId='{}'", command.keycloakId(), command.tenantId());
        AuthActionToken token = tokenService.createInvite(command);
        try {
            emailService.sendInviteActivation(token);
            log.info("sendInviteActivation: convite enviado para keycloakId='{}'", command.keycloakId());
        } catch (Exception e) {
            log.error("sendInviteActivation: falha ao enviar e-mail para keycloakId='{}' — token salvo, use 'Reenviar convite'",
                    command.keycloakId(), e);
        }
        return token;
    }
}

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

    public AuthActionToken sendInviteActivation(IdentityActionInviteCommand command) {
        log.debug("sendInviteActivation: keycloakId='{}', tenantId='{}'", command.keycloakId(), command.tenantId());
        AuthActionToken token = tokenService.createInvite(command);
        emailService.sendInviteActivation(token);
        log.info("sendInviteActivation: convite enviado para keycloakId='{}'", command.keycloakId());
        return token;
    }
}

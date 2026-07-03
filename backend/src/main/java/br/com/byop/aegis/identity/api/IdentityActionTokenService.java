package br.com.byop.aegis.identity.api;

import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import br.com.byop.aegis.identity.auth.service.AuthActionEmailService;
import br.com.byop.aegis.identity.auth.service.AuthActionTokenService;
import org.springframework.stereotype.Service;

@Service
public class IdentityActionTokenService {

    private final AuthActionTokenService tokenService;
    private final AuthActionEmailService emailService;

    public IdentityActionTokenService(AuthActionTokenService tokenService, AuthActionEmailService emailService) {
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    public AuthActionToken sendInviteActivation(IdentityActionInviteCommand command) {
        AuthActionToken token = tokenService.createInvite(command);
        emailService.sendInviteActivation(token);
        return token;
    }
}

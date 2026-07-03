package br.com.byop.aegis.tenant.controller;

import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint minimo de conclusao do tutorial interativo de onboarding
 * (Sprint 22, Secao E) — requer autenticacao. Fora do prefixo
 * {@code /api/v1/auth/**} de proposito: esse prefixo e {@code permitAll()}
 * em {@link br.com.byop.aegis.security.SecurityConfig} porque os demais
 * endpoints de auth (login/ativacao/reset) sao pre-autenticacao; este
 * endpoint exige um usuario ja logado, entao vive no prefixo
 * {@code /api/v1/**} autenticado por padrao.
 */
@RestController
public class TutorialController {

    private final TenantAccessService tenantAccessService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public TutorialController(TenantAccessService tenantAccessService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.tenantAccessService = tenantAccessService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Marca o tutorial de onboarding como concluido para o usuario
     * autenticado, em todas as suas memberships de tenant. Idempotente —
     * chamar mais de uma vez nao e um erro.
     *
     * @param authentication autenticacao atual resolvida pelo Spring Security
     */
    @PostMapping("/api/v1/tutorial/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeTutorial(Authentication authentication) {
        AuthenticatedUser user = authenticatedUserProvider.from(authentication);
        tenantAccessService.markTutorialCompleted(user.subject());
    }
}

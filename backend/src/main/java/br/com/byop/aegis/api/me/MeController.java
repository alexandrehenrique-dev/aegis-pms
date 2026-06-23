package br.com.byop.aegis.api.me;

import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint autenticado para consulta da identidade atual.
 */
@RestController
public class MeController {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final MeResponseMapper meResponseMapper;

    public MeController(
            AuthenticatedUserProvider authenticatedUserProvider,
            MeResponseMapper meResponseMapper
    ) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.meResponseMapper = meResponseMapper;
    }

    /**
     * Retorna os dados públicos do usuário autenticado.
     *
     * @param authentication autenticação atual resolvida pelo Spring Security
     * @return dados públicos do usuário autenticado
     */
    @GetMapping("/api/v1/me")
    public MeResponse me(Authentication authentication) {
        AuthenticatedUser user = authenticatedUserProvider.from(authentication);
        return meResponseMapper.toResponse(user);
    }
}
package br.com.byop.aegis.api.me;

import br.com.byop.aegis.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapeia o usuário autenticado interno para o contrato público do endpoint /me.
 */
@Component
public class MeResponseMapper {

    private static final List<RoleMapping> ROLE_PRIORITY = List.of(
            new RoleMapping("ROLE_SUPER_ADMIN", "super_admin"),
            new RoleMapping("ROLE_TENANT_ADMIN", "tenant_admin"),
            new RoleMapping("ROLE_PRODUCT_MANAGER", "product_manager"),
            new RoleMapping("ROLE_EDITOR", "editor"),
            new RoleMapping("ROLE_VIEWER", "viewer")
    );

    /**
     * Converte o usuário autenticado para a resposta pública.
     *
     * @param user usuário autenticado
     * @return resposta pública do /me
     */
    public MeResponse toResponse(AuthenticatedUser user) {
        return new MeResponse(
                user.subject(),
                user.email(),
                user.username(),
                user.name(),
                resolvePublicRole(user)
        );
    }

    private String resolvePublicRole(AuthenticatedUser user) {
        return ROLE_PRIORITY.stream()
                .filter(mapping -> user.authorities().contains(mapping.springAuthority()))
                .map(RoleMapping::publicRole)
                .findFirst()
                .orElse("viewer");
    }

    private record RoleMapping(String springAuthority, String publicRole) {
    }
}

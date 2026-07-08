package br.com.byop.aegis.api.me;

import br.com.byop.aegis.product.api.ProductUserAccessService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    private final TenantAccessService tenantAccessService;
    private final ProductUserAccessService productUserAccessService;

    public MeResponseMapper(TenantAccessService tenantAccessService, ProductUserAccessService productUserAccessService) {
        this.tenantAccessService = tenantAccessService;
        this.productUserAccessService = productUserAccessService;
    }

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
                resolvePublicRole(user),
                tenantAccessService.hasCompletedTutorial(user.subject())
        );
    }

    private String resolvePublicRole(AuthenticatedUser user) {
        List<String> authorities = new ArrayList<>(user.authorities());
        if (!tenantAccessService.findActiveTenantAdminTenantIds(user.subject()).isEmpty()) {
            authorities.add("ROLE_TENANT_ADMIN");
        }
        productUserAccessService.findHighestAssignedRole(user.subject())
                .map(role -> "ROLE_" + role)
                .ifPresent(authorities::add);
        return ROLE_PRIORITY.stream()
                .filter(mapping -> authorities.contains(mapping.springAuthority()))
                .map(RoleMapping::publicRole)
                .findFirst()
                .orElse("viewer");
    }

    private record RoleMapping(String springAuthority, String publicRole) {
    }
}

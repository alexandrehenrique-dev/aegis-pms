package br.com.byop.aegis.product.user.mapper;

import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.product.api.ProductUserAccess;
import br.com.byop.aegis.product.user.dto.TenantUserSummary;
import br.com.byop.aegis.tenant.api.TenantMembershipReference;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TenantUserMapper {

    default TenantUserSummary toSummary(TenantMembershipReference membership, IdentityUser user,
                                        List<ProductUserAccess> assignments) {
        return new TenantUserSummary(
                membership.userSubject(),
                user.displayName(),
                user.email(),
                membership.role() == null ? null : membership.role().toLowerCase(),
                products(assignments),
                membership.status(),
                "nunca",
                "convidado".equals(membership.status()) ? "pendente" : "ativo"
        );
    }

    default String products(List<ProductUserAccess> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return "";
        }
        return assignments.stream()
                .filter(assignment -> !"removido".equals(assignment.status()))
                .map(ProductUserAccess::productName)
                .distinct()
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}

package br.com.byop.aegis.product.user.mapper;

import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.product.api.ProductUserAccess;
import br.com.byop.aegis.product.user.dto.TenantUserSummary;
import br.com.byop.aegis.tenant.api.TenantMembershipReference;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantUserMapperTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final TenantUserMapper mapper = Mappers.getMapper(TenantUserMapper.class);

    @Test
    void shouldMapMembershipUserAndAssignmentsToSummary() {
        TenantMembershipReference membership = membership("convidado", "EDITOR");
        IdentityUser user = new IdentityUser("user-1", "guest", "guest@byop.dev", "Guest", "User");
        ProductUserAccess first = assignment("Maestro", "atribuido");
        ProductUserAccess second = assignment("Portal", "atribuido");
        ProductUserAccess removed = assignment("Old", "removido");

        TenantUserSummary summary = mapper.toSummary(membership, user, List.of(first, removed, second, first));

        assertThat(summary.userId()).isEqualTo("user-1");
        assertThat(summary.name()).isEqualTo("Guest User");
        assertThat(summary.email()).isEqualTo("guest@byop.dev");
        assertThat(summary.role()).isEqualTo("editor");
        assertThat(summary.products()).isEqualTo("Maestro, Portal");
        assertThat(summary.status()).isEqualTo("convidado");
        assertThat(summary.lastAccess()).isEqualTo("nunca");
        assertThat(summary.inviteStatus()).isEqualTo("pendente");
    }

    @Test
    void shouldHandleNullRoleAndEmptyAssignments() {
        TenantUserSummary summary = mapper.toSummary(
                membership("ativo", null),
                new IdentityUser("user-1", "guest", "guest@byop.dev", null, " "),
                null
        );

        assertThat(summary.name()).isEqualTo("guest");
        assertThat(summary.role()).isNull();
        assertThat(summary.products()).isEmpty();
        assertThat(summary.inviteStatus()).isEqualTo("ativo");
    }

    @Test
    void shouldHandleExplicitlyEmptyAssignments() {
        TenantUserSummary summary = mapper.toSummary(
                membership("ativo", "VIEWER"),
                new IdentityUser("user-1", "guest", "guest@byop.dev", null, null),
                List.of()
        );

        assertThat(summary.products()).isEmpty();
    }

    private TenantMembershipReference membership(String status, String role) {
        return new TenantMembershipReference(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                TENANT_ID,
                "BYOP",
                "user-1",
                role,
                status,
                OffsetDateTime.parse("2026-06-27T10:00:00Z"),
                OffsetDateTime.parse("2026-06-27T10:00:00Z")
        );
    }

    private ProductUserAccess assignment(String productName, String status) {
        return new ProductUserAccess(TENANT_ID, UUID.nameUUIDFromBytes(productName.getBytes()), productName, "user-1", "EDITOR", status);
    }
}

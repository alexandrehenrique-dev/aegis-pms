package br.com.byop.aegis.contract;

import br.com.byop.aegis.product.contract.AssignProductUserRequest;
import br.com.byop.aegis.tenant.contract.DeleteTenantRequest;
import br.com.byop.aegis.tenant.contract.UpdateTenantRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractRecordTest {

    @Test
    void shouldCreateAssignProductUserRequest() {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        AssignProductUserRequest request = new AssignProductUserRequest(
                productId,
                tenantId,
                "user-1",
                null,
                "EDITOR",
                "atribuido"
        );

        assertThat(request.productId()).isEqualTo(productId);
        assertThat(request.tenantId()).isEqualTo(tenantId);
        assertThat(request.userId()).isEqualTo("user-1");
        assertThat(request.inviteEmail()).isNull();
        assertThat(request.role()).isEqualTo("EDITOR");
        assertThat(request.status()).isEqualTo("atribuido");
    }

    @Test
    void shouldCreateUpdateTenantRequest() {
        UpdateTenantRequest request = new UpdateTenantRequest("BYOP", "PRO", "ativo");

        assertThat(request.name()).isEqualTo("BYOP");
        assertThat(request.plan()).isEqualTo("PRO");
        assertThat(request.status()).isEqualTo("ativo");
    }

    @Test
    void shouldCreateDeleteTenantRequest() {
        DeleteTenantRequest request = new DeleteTenantRequest("DELETE");

        assertThat(request.confirmationText()).isEqualTo("DELETE");
    }
}

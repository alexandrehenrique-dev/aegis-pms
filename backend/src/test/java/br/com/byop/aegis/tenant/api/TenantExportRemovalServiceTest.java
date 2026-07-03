package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantExportRemovalServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private TenantRepository tenantRepository;

    @Test
    void shouldDeleteTenantWhenItStillExists() {
        Tenant tenant = new Tenant("byop", "BYOP");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        service().deleteTenantAfterExports(TENANT_ID);

        verify(tenantRepository).delete(tenant);
    }

    @Test
    void shouldIgnoreMissingTenant() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        service().deleteTenantAfterExports(TENANT_ID);

        verify(tenantRepository, never()).delete(org.mockito.Mockito.any(Tenant.class));
    }

    private TenantExportRemovalService service() {
        return new TenantExportRemovalService(tenantRepository);
    }
}

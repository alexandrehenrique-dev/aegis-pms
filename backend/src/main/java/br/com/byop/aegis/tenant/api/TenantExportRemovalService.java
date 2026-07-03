package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class TenantExportRemovalService implements TenantExportRemovalPort {

    private final TenantRepository tenantRepository;

    TenantExportRemovalService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public void deleteTenantAfterExports(UUID tenantId) {
        tenantRepository.findById(tenantId).ifPresent(tenantRepository::delete);
    }
}

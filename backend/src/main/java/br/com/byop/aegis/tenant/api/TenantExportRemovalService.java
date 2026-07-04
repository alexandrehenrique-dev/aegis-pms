package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
class TenantExportRemovalService implements TenantExportRemovalPort {

    private final TenantRepository tenantRepository;

    TenantExportRemovalService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public void deleteTenantAfterExports(UUID tenantId) {
        log.debug("deleteTenantAfterExports: tenantId='{}'", tenantId);
        tenantRepository.findById(tenantId).ifPresent(tenantRepository::delete);
        log.info("deleteTenantAfterExports: tenant removido apos exportacoes tenantId='{}'", tenantId);
    }
}

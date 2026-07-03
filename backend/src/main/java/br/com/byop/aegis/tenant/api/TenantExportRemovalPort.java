package br.com.byop.aegis.tenant.api;

import java.util.UUID;

public interface TenantExportRemovalPort {

    void deleteTenantAfterExports(UUID tenantId);
}

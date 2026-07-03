package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.security.AuthenticatedUser;
import java.util.UUID;

public interface TenantProductExportPort {

    void startTenantProductExports(UUID tenantId, AuthenticatedUser caller);
}

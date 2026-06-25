package br.com.byop.aegis.core.tenant;

import br.com.byop.aegis.core.tenant.dto.TenantSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantSummary toSummary(Tenant tenant);
}

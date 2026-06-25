package br.com.byop.aegis.tenant.mapper;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantSummary toSummary(Tenant tenant);
}

package br.com.byop.aegis.tenant.mapper;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantStatus;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    @Mapping(target = "status", expression = "java(toContractStatus(tenant.getStatus()))")
    TenantSummary toSummary(Tenant tenant);

    default String toContractStatus(TenantStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "ativo";
            case SUSPENDED -> "suspenso";
            case ARCHIVED -> "arquivado";
        };
    }
}

package br.com.byop.aegis.audit.mapper;

import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.domain.AuditRisk;
import br.com.byop.aegis.audit.dto.AuditEventDetail;
import br.com.byop.aegis.audit.dto.AuditEventSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

/**
 * Conversor {@code AuditEvent -> AuditEventSummary/AuditEventDetail}. Os
 * valores de exibicao (ator, tenant, recurso) sao resolvidos pelo
 * {@code AuditEventQueryService} antes da chamada — o mapper apenas monta o
 * DTO final, sem acessar nenhum outro modulo.
 */
@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "actor", source = "actor")
    @Mapping(target = "action", source = "event.action")
    @Mapping(target = "target", source = "target")
    @Mapping(target = "tenant", source = "tenant")
    @Mapping(target = "module", source = "event.module")
    @Mapping(target = "time", expression = "java(event == null ? null : event.getCreatedAt().toString())")
    @Mapping(target = "risk", expression = "java(event == null ? null : toContractRisk(event.getRisk()))")
    AuditEventSummary toSummary(AuditEvent event, String actor, String tenant, String target);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "actor", source = "actor")
    @Mapping(target = "action", source = "event.action")
    @Mapping(target = "target", source = "target")
    @Mapping(target = "tenant", source = "tenant")
    @Mapping(target = "module", source = "event.module")
    @Mapping(target = "time", expression = "java(event == null ? null : event.getCreatedAt().toString())")
    @Mapping(target = "risk", expression = "java(event == null ? null : toContractRisk(event.getRisk()))")
    @Mapping(target = "diffJson", source = "diffJson")
    @Mapping(target = "traceId", source = "event.traceId")
    @Mapping(target = "ip", source = "event.ip")
    @Mapping(target = "userAgent", source = "event.userAgent")
    AuditEventDetail toDetail(AuditEvent event, String actor, String tenant, String target, Map<String, Object> diffJson);

    default String toContractRisk(AuditRisk risk) {
        return risk == null ? null : risk.contractValue();
    }
}

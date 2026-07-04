import { auditEvents } from "../mocks/audit.mocks";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AuditEvent, ListAuditEventsResponse } from "../contracts/responses";

const auditStore: AuditEvent[] = auditEvents.map(([actor, action, target, tenant, module, time, risk]) => ({
  actor, action, target, tenant, module, time, risk,
}));

type AuditEventDto = AuditEvent & {
  actorSubject?: string;
  targetType?: string;
  targetId?: string;
  tenantId?: string;
  timestamp?: string;
};

function mapAuditEvent(dto: AuditEventDto): AuditEvent {
  const fallbackTarget = [dto.targetType, dto.targetId].filter(Boolean).join(":") || "—";
  return {
    id: dto.id,
    actor: dto.actor ?? dto.actorSubject ?? "—",
    action: dto.action,
    target: dto.target ?? fallbackTarget,
    tenant: dto.tenant ?? dto.tenantId ?? "—",
    module: dto.module ?? "—",
    time: dto.time ?? (dto.timestamp ? new Date(dto.timestamp).toLocaleTimeString("pt-BR") : "—"),
    risk: dto.risk ?? "baixo",
  };
}

export const auditService = {
  /**
   * Backend é tenant-scoped (`GET /tenants/{tenantId}/audit-events`,
   * `AuditEventController`) — `AuditTimeline` hoje é uma visão cross-tenant de
   * Super Admin sem esse contexto ainda (Sprint de Integração 06 resolve).
   * `tenantId` obrigatório em modo api até lá.
   */
  async listEvents(tenantId?: string): Promise<ListAuditEventsResponse> {
    if (IS_API_MODE) {
      if (!tenantId) throw { status: 400, message: "Timeline de auditoria requer um tenant selecionado." };
      const events = await apiClient.get<AuditEventDto[]>(`/tenants/${tenantId}/audit-events`);
      return events.map(mapAuditEvent);
    }
    return auditStore;
  },
  // `recordEvent` removido (Sprint de Integração 02): auditoria é
  // exclusivamente server-side — o frontend nunca cria eventos de auditoria,
  // e não existe `POST /audit/events` no backend. Sem chamadores no app.
};

import { auditEvents } from "../mocks/audit.mocks";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AuditEvent, ListAuditEventsResponse } from "../contracts/responses";

const auditStore: AuditEvent[] = auditEvents.map(([actor, action, target, tenant, module, time, risk]) => ({
  actor, action, target, tenant, module, time, risk,
}));

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
      return apiClient.get<ListAuditEventsResponse>(`/tenants/${tenantId}/audit-events`);
    }
    return auditStore;
  },
  // `recordEvent` removido (Sprint de Integração 02): auditoria é
  // exclusivamente server-side — o frontend nunca cria eventos de auditoria,
  // e não existe `POST /audit/events` no backend. Sem chamadores no app.
};

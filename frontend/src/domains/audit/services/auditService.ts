import { auditEvents } from "../mocks/audit.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AuditEvent, ListAuditEventsResponse } from "../contracts/responses";

const auditStore: AuditEvent[] = auditEvents.map(([actor, action, target, tenant, module, time, risk]) => ({
  actor, action, target, tenant, module, time, risk,
}));

export const auditService = {
  async listEvents(): Promise<ListAuditEventsResponse> {
    if (IS_API_MODE) return apiClient.get<ListAuditEventsResponse>("/audit/events");
    return auditStore;
  },

  /** Registra um evento de auditoria (bug fix Sprint 20, Tarefa D.4) — útil para testar o log em operações que ainda não disparam auditoria no mock. */
  async recordEvent(event: Omit<AuditEvent, "time">): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/audit/events", event);
    logApiCall("POST", "/api/v1/audit/events", event);
    auditStore.unshift({ ...event, time: new Date().toLocaleTimeString("pt-BR") });
  },
};

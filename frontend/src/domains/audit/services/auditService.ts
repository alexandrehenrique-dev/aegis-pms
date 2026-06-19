import { auditEvents } from "../mocks/audit.mocks";
import type { AuditEvent, ListAuditEventsResponse } from "../contracts/responses";

const auditStore: AuditEvent[] = auditEvents.map(([actor, action, target, tenant, module, time, risk]) => ({
  actor, action, target, tenant, module, time, risk,
}));

export const auditService = {
  async listEvents(): Promise<ListAuditEventsResponse> {
    return auditStore;
  },
};

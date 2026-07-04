export type AuditEvent = {
  id?: string;
  actor: string; action: string; target: string;
  tenant: string; module: string; time: string; risk: string;
};

export type ListAuditEventsResponse = AuditEvent[];

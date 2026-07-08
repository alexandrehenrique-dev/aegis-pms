export type AuditEvent = {
  id?: string;
  actor: string; action: string; target: string;
  tenant: string; module: string; time: string; risk: string;
};

export type ListAuditEventsResponse = AuditEvent[];

export type AuditEventDetailDto = AuditEvent & {
  diffJson: { before?: Record<string, unknown>; after?: Record<string, unknown> } & Record<string, unknown>;
  traceId: string | null;
  ip: string | null;
  userAgent: string | null;
};

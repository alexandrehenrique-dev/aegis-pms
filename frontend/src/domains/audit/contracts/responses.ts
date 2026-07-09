export type AuditEvent = {
  id?: string;
  actor: string; action: string; target: string;
  tenant: string; module: string; time: string; risk: string;
};

export type ListAuditEventsResponse = AuditEvent[];

export type AuditEventFilters = {
  productId?: string;
  module?: string;
  productName?: string;
  risk?: string;
  query?: string;
};

export type AuditEventPage = {
  items: AuditEvent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AuditEventDetailDto = AuditEvent & {
  diffJson: { before?: Record<string, unknown>; after?: Record<string, unknown> } & Record<string, unknown>;
  traceId: string | null;
  ip: string | null;
  userAgent: string | null;
};

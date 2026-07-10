import { auditEvents } from "../mocks/audit.mocks";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AuditEvent, AuditEventDetailDto, AuditEventFilters, AuditEventPage, ListAuditEventsResponse } from "../contracts/responses";

const auditStore: AuditEvent[] = auditEvents.map(([actor, action, target, tenant, module, time, risk], i) => ({
  id: `mock-evt-${i}`, actor, action, target, tenant, module, time, risk,
}));

type AuditEventDto = AuditEvent & {
  actorSubject?: string;
  targetType?: string;
  targetId?: string;
  tenantId?: string;
  timestamp?: string;
};

type AuditEventDetailResponseDto = AuditEventDto & {
  diffJson?: AuditEventDetailDto["diffJson"] | null;
  traceId?: string | null;
  ip?: string | null;
  userAgent?: string | null;
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

function mapAuditEventDetail(dto: AuditEventDetailResponseDto): AuditEventDetailDto {
  return {
    ...mapAuditEvent(dto),
    diffJson: dto.diffJson ?? {},
    traceId: dto.traceId ?? null,
    ip: dto.ip ?? null,
    userAgent: dto.userAgent ?? null,
  };
}

export const auditService = {
  /**
   * Backend é tenant-scoped (`GET /tenants/{tenantId}/audit-events`,
   * `AuditEventController`) — `AuditTimeline` hoje é uma visão cross-tenant de
   * Super Admin sem esse contexto ainda (Sprint de Integração 06 resolve).
   * `tenantId` obrigatório em modo api até lá.
   *
   * `filters.module`/`filters.productId` (F.4, BUG-SPRINT consolidado) —
   * repassados como query params reais (`AuditEventController` já os aceita).
   * Em mock mode, `module` casa por prefixo (`"Forms".startsWith("FORM")`,
   * já que o backend usa a constante `MODULE_FORM = "FORM"` mas os fixtures
   * locais guardam o nome de exibição "Forms") e `productName` casa contra o
   * campo `tenant` do fixture — os mocks reaproveitam esse campo para nome
   * de produto em vez de tenant real (inconsistência pré-existente do
   * fixture, não deste bug fix).
   */
  async listEvents(tenantId?: string, filters?: AuditEventFilters): Promise<ListAuditEventsResponse> {
    if (IS_API_MODE) {
      if (!tenantId) throw { status: 400, message: "Timeline de auditoria requer um tenant selecionado." };
      const query = new URLSearchParams();
      if (filters?.productId) query.set("productId", filters.productId);
      if (filters?.module) query.set("module", filters.module);
      if (filters?.risk) query.set("risk", filters.risk);
      const qs = query.toString();
      const events = await apiClient.get<AuditEventDto[]>(`/tenants/${tenantId}/audit-events${qs ? `?${qs}` : ""}`);
      return events.map(mapAuditEvent);
    }
    let result = auditStore;
    if (filters?.module) {
      const wanted = filters.module.toLowerCase();
      result = result.filter((e) => e.module.toLowerCase().startsWith(wanted));
    }
    if (filters?.productName) result = result.filter((e) => e.tenant === filters.productName);
    if (filters?.risk) result = result.filter((e) => e.risk === filters.risk);
    if (filters?.query) {
      const query = filters.query.toLowerCase();
      result = result.filter((e) => [e.actor, e.action, e.target, e.tenant, e.module]
        .some((value) => value.toLowerCase().includes(query)));
    }
    return result;
  },
  async listEventsPage(tenantId?: string, filters: AuditEventFilters = {}, page = 0, size = 25): Promise<AuditEventPage> {
    if (IS_API_MODE) {
      if (!tenantId) throw { status: 400, message: "Timeline de auditoria requer um tenant selecionado." };
      const query = new URLSearchParams();
      if (filters.productId) query.set("productId", filters.productId);
      if (filters.module) query.set("module", filters.module);
      if (filters.risk) query.set("risk", filters.risk);
      if (filters.query?.trim()) query.set("q", filters.query.trim());
      query.set("page", String(page));
      query.set("size", String(size));
      const response = await apiClient.get<{
        items: AuditEventDto[];
        page: number;
        size: number;
        totalElements: number;
        totalPages: number;
      }>(`/tenants/${tenantId}/audit-events/page?${query.toString()}`);
      return {
        ...response,
        items: response.items.map(mapAuditEvent),
      };
    }
    const all = await this.listEvents(tenantId, filters);
    const start = Math.max(page, 0) * size;
    const items = all.slice(start, start + size);
    return {
      items,
      page: Math.max(page, 0),
      size,
      totalElements: all.length,
      totalPages: Math.ceil(all.length / size),
    };
  },
  /**
   * G.3 (BUG-SPRINT-05) — detalhe de um evento (`AuditEventDetail.tsx`), com
   * `diffJson`/`traceId`/`ip`/`userAgent` além dos campos de `AuditEvent`.
   * Backend: `GET /tenants/{tenantId}/audit-events/{eventId}`.
   */
  async getEvent(tenantId: string, eventId: string): Promise<AuditEventDetailDto> {
    if (IS_API_MODE) {
      const event = await apiClient.get<AuditEventDetailResponseDto>(`/tenants/${tenantId}/audit-events/${eventId}`);
      return mapAuditEventDetail(event);
    }
    const found = auditStore.find((e) => e.id === eventId);
    if (!found) throw { status: 404, message: `Evento ${eventId} não encontrado.` };
    return {
      ...found,
      diffJson: { before: {}, after: {} },
      traceId: `mock_tr_${eventId}`,
      ip: "127.0.0.1",
      userAgent: "mock",
    };
  },
  // `recordEvent` removido (Sprint de Integração 02): auditoria é
  // exclusivamente server-side — o frontend nunca cria eventos de auditoria,
  // e não existe `POST /audit/events` no backend. Sem chamadores no app.
};

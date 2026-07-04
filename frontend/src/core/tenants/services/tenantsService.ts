import { allTenants } from "../mocks/tenants.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { notificationsService } from "../../notifications/services/notificationsService";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import { mapTenantSummaries, mapTenantSummary, type TenantSummaryDto } from "../mappers/tenantMapper";
import type { TenantOption } from "../../../shared/types";
import type { CreateTenantRequest, DeleteTenantRequest, UpdateTenantRequest } from "../contracts/requests";
import type { ListTenantsResponse, TenantDetailResponse } from "../contracts/responses";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const tenantsStore: TenantOption[] = [...allTenants];

export const tenantsService = {
  async listTenants(): Promise<ListTenantsResponse> {
    if (IS_API_MODE) {
      const dtos = await apiClient.get<TenantSummaryDto[]>("/tenants");
      return mapTenantSummaries(dtos);
    }
    return tenantsStore;
  },

  async getTenant(id: string): Promise<TenantDetailResponse | undefined> {
    if (IS_API_MODE) {
      const tenants = await this.listTenants();
      return tenants.find((tenant) => tenant.id === id);
    }
    return tenantsStore.find((t) => t.id === id);
  },

  async create(req: CreateTenantRequest): Promise<TenantOption> {
    if (IS_API_MODE) {
      const dto = await apiClient.post<TenantSummaryDto>("/tenants", {
        key: req.slug,
        name: req.name,
        plan: req.plan,
        initialAdminEmail: req.initialAdminEmail,
      });
      return mapTenantSummary(dto);
    }
    logApiCall("POST", "/api/v1/admin/tenants", req);
    const created: TenantOption = {
      id: req.slug,
      name: req.name,
      plan: req.plan,
      productCount: 0,
      lastAccess: "—",
      status: "ativo",
    };
    tenantsStore.push(created);
    return created;
  },

  async update(id: string, req: UpdateTenantRequest): Promise<TenantOption> {
    if (IS_API_MODE) {
      const dto = await apiClient.put<TenantSummaryDto>(`/tenants/${id}`, {
        name: req.name,
        plan: req.plan,
        status: req.status === "ativo" ? "ACTIVE" : "SUSPENDED",
      });
      return mapTenantSummary(dto);
    }
    const tenant = tenantsStore.find((t) => t.id === id);
    if (!tenant) throw { status: 404, message: `Tenant ${id} não encontrado.` };
    const previousStatus = tenant.status;
    logApiCall("PUT", `/api/v1/admin/tenants/${id}`, req);
    tenant.name = req.name;
    tenant.plan = req.plan;
    tenant.status = req.status;

    if (previousStatus !== req.status) {
      const suspended = req.status === "suspenso";
      await notificationsService.create({
        type: suspended ? "WARNING" : "GENERAL",
        title: suspended ? "Tenant suspenso" : "Tenant reativado",
        bodyMarkdown: suspended
          ? "Este tenant foi suspenso pelo administrador da plataforma. Contate o suporte para mais informações."
          : "Este tenant foi reativado.",
        presentationMode: "BELL_ONLY",
        recipients: { mode: "tenant", tenantId: id },
      });
    }

    return tenant;
  },

  /** Destrutivo e irreversível no backend real: remove o tenant e cascateia para seus produtos/usuários. A UI deve sempre confirmar com um modal de severidade antes de chamar isto. */
  async remove(id: string, req: DeleteTenantRequest): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/tenants/${id}`);
    const index = tenantsStore.findIndex((t) => t.id === id);
    if (index < 0) return;
    logApiCall("DELETE", `/api/v1/admin/tenants/${id}`, req);
    tenantsStore.splice(index, 1);
  },
};

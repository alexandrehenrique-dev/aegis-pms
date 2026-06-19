import { allTenants } from "../mocks/tenants.mocks";
import type { TenantOption } from "../../../shared/types";
import type { CreateTenantRequest, DeleteTenantRequest, UpdateTenantRequest } from "../contracts/requests";
import type { ListTenantsResponse, TenantDetailResponse } from "../contracts/responses";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const tenantsStore: TenantOption[] = [...allTenants];

export const tenantsService = {
  async listTenants(): Promise<ListTenantsResponse> {
    return tenantsStore;
  },

  async getTenant(id: string): Promise<TenantDetailResponse | undefined> {
    return tenantsStore.find((t) => t.id === id);
  },

  async create(req: CreateTenantRequest): Promise<TenantOption> {
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
    const tenant = tenantsStore.find((t) => t.id === id);
    if (!tenant) throw { status: 404, message: `Tenant ${id} não encontrado.` };
    tenant.name = req.name;
    tenant.plan = req.plan;
    tenant.status = req.status;
    return tenant;
  },

  /** Destrutivo e irreversível no backend real: remove o tenant e cascateia para seus produtos/usuários. A UI deve sempre confirmar com um modal de severidade antes de chamar isto. */
  async remove(id: string, _req: DeleteTenantRequest): Promise<void> {
    const index = tenantsStore.findIndex((t) => t.id === id);
    if (index >= 0) tenantsStore.splice(index, 1);
  },
};

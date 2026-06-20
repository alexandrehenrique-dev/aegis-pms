import { products as productMocks } from "../mocks/products.mocks";
import { modules as moduleCatalogMocks } from "../../dashboard/mocks/dashboard.mocks";
import { slugify } from "../../../shared/utils/slugify";
import type { ComponentType } from "react";
import type { ModuleState } from "../../../shared/types";
import type { CreateProductRequest, DeleteProductRequest, UpdateProductRequest } from "../contracts/requests";
import type { ListModulesResponse, ListProductsResponse, ProductSummary } from "../contracts/responses";

// Store em memória só para a sessão do navegador — quando o backend existir
// (Sprint 07 troca a implementação, não a interface), isto desaparece e
// listProducts/create passam a falar com a API real via apiClient.
const productsStore: ProductSummary[] = [...productMocks];

const moduleCatalogStore: ListModulesResponse = moduleCatalogMocks.map(([Icon, name, desc, state, maturity, dependency, impact]) => ({
  Icon: Icon as ComponentType<{ size?: number; className?: string }>,
  name: name as string,
  desc: desc as string,
  state: state as ModuleState,
  maturity: maturity as string,
  dependency: dependency as string,
  impact: impact as string,
}));

export const productsService = {
  async listProducts(): Promise<ListProductsResponse> {
    return productsStore;
  },

  async listModuleCatalog(): Promise<ListModulesResponse> {
    return moduleCatalogStore;
  },

  async enableModule(moduleName: string): Promise<void> {
    const m = moduleCatalogStore.find((x) => x.name === moduleName);
    if (m) m.state = "habilitado";
  },

  async disableModule(moduleName: string): Promise<void> {
    const m = moduleCatalogStore.find((x) => x.name === moduleName);
    if (m) m.state = "desabilitado";
  },

  async archiveProduct(idOrName: string): Promise<void> {
    const p = productsStore.find((x) => x.id === idOrName || x.name === idOrName);
    if (p) p.status = "Arquivado";
  },

  async saveSettings(): Promise<void> {},

  /** Editar Produto (docs/implementation/004_aegis_pms_screen_inventory.md, 05.04) — nome/tipo/status; gating de role fica na UI (ver core/permissions/roles.ts). */
  async update(idOrName: string, req: UpdateProductRequest): Promise<ProductSummary> {
    const p = productsStore.find((x) => x.id === idOrName || x.name === idOrName);
    if (!p) throw { status: 404, message: `Produto ${idOrName} não encontrado.` };
    // Ponto de integração real (Sprint 07): PATCH /api/v1/admin/products/{productId} — docs/AEGIS_PMS_V1.md §8.4.
    console.log(`[mock→backend] PATCH /api/v1/admin/products/${p.id ?? p.name}`, req);
    p.name = req.name;
    p.type = req.type;
    p.status = req.status;
    return p;
  },

  /**
   * Exclusão lógica (soft delete) — `DELETE /api/v1/admin/products/{productId}`
   * em docs/AEGIS_PMS_V1.md §8.4: "produto nunca é apagado fisicamente sem
   * política de retenção" (§8.5, lei 5). No mock isto remove da listagem,
   * simulando o efeito visível de uma exclusão lógica para o usuário.
   */
  async remove(idOrName: string, req: DeleteProductRequest): Promise<void> {
    const index = productsStore.findIndex((x) => x.id === idOrName || x.name === idOrName);
    if (index < 0) return;
    // Ponto de integração real (Sprint 07): DELETE /api/v1/admin/products/{productId} — docs/AEGIS_PMS_V1.md §8.4.
    console.log(`[mock→backend] DELETE /api/v1/admin/products/${idOrName}`, req);
    productsStore.splice(index, 1);
  },

  async create(req: CreateProductRequest): Promise<ProductSummary> {
    const created: ProductSummary = {
      id: slugify(req.name) || slugify(req.slug),
      name: req.name,
      type: req.type,
      status: "Pendente",
      modules: req.initialModules.length,
      last: "Produto criado agora",
      score: "—",
      tenantId: req.tenantId,
    };
    productsStore.push(created);
    return created;
  },
};

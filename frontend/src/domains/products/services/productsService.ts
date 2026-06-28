import { products as productMocks } from "../mocks/products.mocks";
import { modules as moduleCatalogMocks } from "../../dashboard/mocks/dashboard.mocks";
import { slugify } from "../../../shared/utils/slugify";
import { logApiCall } from "../../../shared/services/devLog";
import { pagesService } from "../../pages/services/pagesService";
import { DEFAULT_BLOCK_CONTENT } from "../../pages/blockDefaults";
import { PRODUCT_PAGE_SKELETONS } from "../../../core/products/productTemplates";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { ProductTypeKey } from "../../../core/products/moduleDefaults";
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
    if (IS_API_MODE) return apiClient.get<ListProductsResponse>("/products");
    return productsStore;
  },

  async listModuleCatalog(): Promise<ListModulesResponse> {
    // Catálogo de módulos: em modo api, vem do backend por produto.
    // Path: /products/{productId}/modules — implementar quando houver contexto de produto.
    return moduleCatalogStore; // mock para ambos os modos por enquanto
  },

  /**
   * Habilita um módulo no catálogo global (comportamento original) e, quando
   * `productId` é informado, propaga para o `modulesList` do produto no
   * `productsStore` (bug fix Sprint 20, Tarefa C.2) — antes, habilitar um
   * módulo via `ModuleCatalog` nunca refletia na lista de módulos do produto.
   * `productId` é opcional para manter compatibilidade com chamadas
   * existentes que não tinham contexto de produto.
   */
  async enableModule(moduleName: string, productId?: string): Promise<void> {
    if (IS_API_MODE && productId) {
      return apiClient.post(`/products/${productId}/modules/${moduleName}/enable`);
    }
    const m = moduleCatalogStore.find((x) => x.name === moduleName);
    if (m) {
      logApiCall("POST", `/api/v1/products/${productId ?? "{productId}"}/modules/${moduleName}/enable`);
      m.state = "habilitado";
    }
    if (productId) {
      const p = productsStore.find((x) => x.id === productId || x.name === productId);
      if (p) {
        if (!p.modulesList) p.modulesList = [];
        if (!p.modulesList.includes(moduleName)) p.modulesList.push(moduleName);
        p.modules = p.modulesList.length;
      }
    }
  },

  /** Ver `enableModule` — mesma propagação para desabilitar. */
  async disableModule(moduleName: string, productId?: string): Promise<void> {
    if (IS_API_MODE && productId) {
      return apiClient.post(`/products/${productId}/modules/${moduleName}/disable`);
    }
    const m = moduleCatalogStore.find((x) => x.name === moduleName);
    if (m) {
      logApiCall("POST", `/api/v1/products/${productId ?? "{productId}"}/modules/${moduleName}/disable`);
      m.state = "desabilitado";
    }
    if (productId) {
      const p = productsStore.find((x) => x.id === productId || x.name === productId);
      if (p && p.modulesList) {
        p.modulesList = p.modulesList.filter((n) => n !== moduleName);
        p.modules = p.modulesList.length;
      }
    }
  },

  /** docs/AEGIS_PMS_V1.md §8.4: `POST /api/v1/admin/products/{productId}/archive`. */
  async archiveProduct(idOrName: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${idOrName}/archive`);
    const p = productsStore.find((x) => x.id === idOrName || x.name === idOrName);
    if (!p) return;
    logApiCall("POST", `/api/v1/admin/products/${p.id ?? p.name}/archive`);
    p.status = "Arquivado";
  },

  async saveSettings(): Promise<void> {
    if (IS_API_MODE) return apiClient.put("/products/{productId}/settings");
    logApiCall("PATCH", "/api/v1/admin/products/{productId} (settings)");
  },

  /** Editar Produto (docs/implementation/004_aegis_pms_screen_inventory.md, 05.04) — nome/tipo/status; gating de role fica na UI (ver core/permissions/roles.ts). */
  async update(idOrName: string, req: UpdateProductRequest): Promise<ProductSummary> {
    if (IS_API_MODE) return apiClient.put<ProductSummary>(`/products/${idOrName}`, req);
    const p = productsStore.find((x) => x.id === idOrName || x.name === idOrName);
    if (!p) throw { status: 404, message: `Produto ${idOrName} não encontrado.` };
    logApiCall("PATCH", `/api/v1/admin/products/${p.id ?? p.name}`, req);
    p.name = req.name;
    p.type = req.type;
    p.status = req.status;
    p.modulesList = req.modules;
    p.modules = req.modules.length;
    return p;
  },

  /**
   * Exclusão lógica (soft delete) — `DELETE /api/v1/admin/products/{productId}`
   * em docs/AEGIS_PMS_V1.md §8.4: "produto nunca é apagado fisicamente sem
   * política de retenção" (§8.5, lei 5). No mock isto remove da listagem,
   * simulando o efeito visível de uma exclusão lógica para o usuário.
   */
  async remove(idOrName: string, req: DeleteProductRequest): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${idOrName}`);
    const index = productsStore.findIndex((x) => x.id === idOrName || x.name === idOrName);
    if (index < 0) return;
    logApiCall("DELETE", `/api/v1/admin/products/${idOrName}`, req);
    productsStore.splice(index, 1);
  },

  /** docs/AEGIS_PMS_V1.md §8.4: `POST /api/v1/admin/products`. */
  async create(req: CreateProductRequest): Promise<ProductSummary> {
    if (IS_API_MODE) return apiClient.post<ProductSummary>("/products", req);
    logApiCall("POST", "/api/v1/admin/products", req);
    const created: ProductSummary = {
      id: slugify(req.name) || slugify(req.slug),
      name: req.name,
      type: req.type,
      status: "Pendente",
      modules: req.initialModules.length,
      modulesList: req.initialModules,
      last: "Produto criado agora",
      score: "—",
      tenantId: req.tenantId,
    };
    productsStore.push(created);

    const skeleton = PRODUCT_PAGE_SKELETONS[req.type as ProductTypeKey];
    if (skeleton) {
      for (const pageSkeleton of skeleton) {
        const page = await pagesService.createPage(created.id!, { slug: pageSkeleton.slug, title: pageSkeleton.title, locale: "pt-BR" });
        for (const section of pageSkeleton.sections) {
          await pagesService.createSection(created.id!, page.id, { type: section.type, label: section.label, content: DEFAULT_BLOCK_CONTENT[section.type] });
        }
      }
    }

    return created;
  },
};

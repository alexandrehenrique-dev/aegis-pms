import { products as productMocks } from "../mocks/products.mocks";
import { modules as moduleCatalogMocks } from "../../dashboard/mocks/dashboard.mocks";
import type { ComponentType } from "react";
import type { ModuleState } from "../../../shared/types";
import type { CreateProductRequest } from "../contracts/requests";
import type { ListModulesResponse, ListProductsResponse, ProductSummary } from "../contracts/responses";

// Store em memória só para a sessão do navegador — quando o backend existir
// (Sprint 07 troca a implementação, não a interface), isto desaparece e
// listProducts/create passam a falar com a API real via apiClient.
const productsStore: ProductSummary[] = [...productMocks];

function slugify(name: string): string {
  return name.toLowerCase().trim().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
}

export const productsService = {
  async listProducts(): Promise<ListProductsResponse> {
    return productsStore;
  },

  async listModuleCatalog(): Promise<ListModulesResponse> {
    return moduleCatalogMocks.map(([Icon, name, desc, state, maturity, dependency, impact]) => ({
      Icon: Icon as ComponentType<{ size?: number; className?: string }>,
      name: name as string,
      desc: desc as string,
      state: state as ModuleState,
      maturity: maturity as string,
      dependency: dependency as string,
      impact: impact as string,
    }));
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

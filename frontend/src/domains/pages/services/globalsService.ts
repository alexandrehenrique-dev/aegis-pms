import { globalsByProduct } from "../mocks/globals.mocks";
import { products as productMocks } from "../../products/mocks/products.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import { slugify } from "../../../shared/utils/slugify";
import type { ProductGlobals, UpdateGlobalsRequest } from "../contracts/globals";

function emptyGlobals(): ProductGlobals {
  return { navbar: { links: [] }, footer: { links: [] }, socialLinks: [] };
}

function mockProductSlug(productId: string): string {
  const product = productMocks.find((p) => p.id === productId);
  return product ? slugify(product.name) : productId;
}

/**
 * `ProductGlobals` por produto (ADR-0013, Sprint 13, Tarefa G) — navbar,
 * footer e redes sociais editados uma única vez por produto, não mais como
 * `BlockType` de página. Espelha `GET/PUT /products/{productId}/globals`
 * (`docs/sprints/sprint-02-fundacao-backend-gpt/21_dominio_pages_secoes_e_blocos.md`, Seção F).
 */
export const globalsService = {
  async getGlobals(productId: string): Promise<ProductGlobals> {
    if (IS_API_MODE) return apiClient.get<ProductGlobals>(`/products/${productId}/globals`);
    const productSlug = mockProductSlug(productId);
    if (!globalsByProduct[productSlug]) globalsByProduct[productSlug] = emptyGlobals();
    return globalsByProduct[productSlug];
  },

  async updateGlobals(productId: string, req: UpdateGlobalsRequest): Promise<ProductGlobals> {
    if (IS_API_MODE) return apiClient.put<ProductGlobals>(`/products/${productId}/globals`, req);
    const productSlug = mockProductSlug(productId);
    const current = globalsByProduct[productSlug] ?? emptyGlobals();
    logApiCall("PUT", `/api/v1/products/${productSlug}/globals`, req);
    const next: ProductGlobals = { ...current, ...req };
    globalsByProduct[productSlug] = next;
    return { ...next };
  },
};

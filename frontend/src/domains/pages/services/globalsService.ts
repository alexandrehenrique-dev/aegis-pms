import { globalsByProduct } from "../mocks/globals.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { ProductGlobals, UpdateGlobalsRequest } from "../contracts/globals";

function emptyGlobals(): ProductGlobals {
  return { navbar: { links: [] }, footer: { links: [] }, socialLinks: [] };
}

/**
 * `ProductGlobals` por produto (ADR-0013, Sprint 13, Tarefa G) — navbar,
 * footer e redes sociais editados uma única vez por produto, não mais como
 * `BlockType` de página. Espelha `GET/PUT /products/{productId}/globals`
 * (`docs/sprints/sprint-02-fundacao-backend-gpt/21_dominio_pages_secoes_e_blocos.md`, Seção F).
 */
export const globalsService = {
  async getGlobals(productSlug: string): Promise<ProductGlobals> {
    if (!globalsByProduct[productSlug]) globalsByProduct[productSlug] = emptyGlobals();
    return globalsByProduct[productSlug];
  },

  async updateGlobals(productSlug: string, req: UpdateGlobalsRequest): Promise<ProductGlobals> {
    const current = globalsByProduct[productSlug] ?? emptyGlobals();
    logApiCall("PUT", `/api/v1/products/${productSlug}/globals`, req);
    const next: ProductGlobals = { ...current, ...req };
    globalsByProduct[productSlug] = next;
    return { ...next };
  },
};

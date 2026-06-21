import { contentByProduct, contents, editEvents, wfInitialItems } from "../mocks/content.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { slugify } from "../../../shared/utils/slugify";
import { resolveEnabledModules } from "../../../core/products/moduleDefaults";
import { knowledgeService } from "../../knowledge/services/knowledgeService";
import type { ContentRow, ListContentResponse, ListEditEventsResponse, ListWorkflowItemsResponse } from "../contracts/responses";

/** Produto efetivo da sessão, no formato mínimo exigido por `resolveEnabledModules` — `null` quando chamado fora de um contexto de produto (ex.: seed inicial). */
type ContentProductContext = { name: string; type: string; modulesList?: string[] } | null;

const KG_REF_PATTERN = /\{\{kg-ref:([\w-]+):([^}]+)\}\}/g;

/**
 * Sprint 16, Tarefa B — fecha o ciclo que deixava `knowledgeService.createEdge`/
 * `ensureNodeForContent` como código morto: ao salvar um `Content` com pelo
 * menos uma referência `{{kg-ref:nodeId:Label}}` no corpo, garante o nó de
 * origem (o próprio conteúdo) e cria a edge para cada referência encontrada.
 *
 * Module-gating (ADR-0015): mock do equivalente a `@RequireModule(KNOWLEDGE_GRAPH)`
 * que o backend real (etapa 07) aplicará no endpoint — sem o módulo
 * habilitado no produto, ignora silenciosamente (o `{{kg-ref}}` fica como
 * texto bruto no corpo, sem node/edge criado).
 */
async function syncKnowledgeGraphRefs(content: ContentRow, product: ContentProductContext): Promise<void> {
  if (typeof content.body !== "string") return;
  if (!resolveEnabledModules(product).includes("Knowledge Graph")) return;
  const matches = [...content.body.matchAll(KG_REF_PATTERN)];
  if (matches.length === 0) return;
  const productSlug = product ? slugify(product.name) : "maestro-beton";
  await knowledgeService.ensureNodeForContent(content.id, content.title, productSlug, "Página");
  for (const [, nodeId] of matches) {
    await knowledgeService.createEdge(content.id, nodeId, productSlug, "RELATED_TO");
  }
}

const contentStore: ContentRow[] = contents.map(([title, type, lang, author, status, updatedAt, publication, version]) => ({
  id: slugify(title), title, type, lang, author, status, updatedAt, publication, version,
}));

// Todo o conteúdo por produto entra também no store genérico — `getContent`/
// `updateContent` resolvem por `id` independente de produto, então precisam
// de uma única lista combinada (Sprint 15, Tarefa A).
for (const rows of Object.values(contentByProduct)) contentStore.push(...rows);

export const contentService = {
  async listContent(): Promise<ListContentResponse> {
    return contentStore;
  },

  /** Conteúdo por produto (Sprint 11, Tarefa E.2) — produtos sem entrada aqui ainda usam o mock genérico de `contentStore`. */
  async listContentByProduct(productSlug: string): Promise<ListContentResponse> {
    return contentByProduct[productSlug] ?? [];
  },

  /** Busca um `Content` por id real (Sprint 15, Tarefa A) — usado pelo editor dedicado, nunca por slug de título improvisado. */
  async getContent(id: string): Promise<ContentRow | undefined> {
    return contentStore.find((c) => c.id === id);
  },

  /** Atualiza campos do `Content` (título, corpo, metadata por tipo) — mutação em memória, mesma estratégia de `productsService`/`pagesService`. `product` (produto efetivo da sessão) habilita o parsing de `kg-ref` no corpo (Sprint 16, Tarefa B) quando o patch alterar `body`. */
  async updateContent(id: string, patch: Partial<ContentRow>, product: ContentProductContext = null): Promise<ContentRow | undefined> {
    logApiCall("PATCH", `/api/v1/products/{productId}/content/${id}`, patch);
    const row = contentStore.find((c) => c.id === id);
    if (!row) return undefined;
    Object.assign(row, patch);
    if (typeof patch.body === "string") await syncKnowledgeGraphRefs(row, product);
    return row;
  },

  async listEditEvents(): Promise<ListEditEventsResponse> {
    return editEvents;
  },
  async listWorkflowItems(): Promise<ListWorkflowItemsResponse> {
    return wfInitialItems;
  },

  /** Cria um artigo do domínio `content` (Sprint 12, Tarefa B) — distinto de `pagesService.createPage`, que cria uma `Page` institucional. `product` segue o mesmo papel de `updateContent` (Sprint 16, Tarefa B). */
  async createContent(payload: { title: string; type: string; lang: string; author: string; body?: string; metadata?: Record<string, unknown> }, product: ContentProductContext = null): Promise<ContentRow> {
    logApiCall("POST", "/api/v1/products/{productId}/content", payload);
    const created: ContentRow = { id: `${slugify(payload.title)}-${Date.now().toString(36)}`, ...payload, status: "Draft", updatedAt: "agora", publication: "—", version: "v1" };
    contentStore.unshift(created);
    if (typeof created.body === "string") await syncKnowledgeGraphRefs(created, product);
    return created;
  },
  // Pontos de integração real (Sprint 07) — docs/trace/00_endpoints_esperados.md, Seção B.1.
  async restoreVersion(version: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/versions/{version}/restore", { version });
  },
  async submitForReview(id?: string): Promise<void> {
    logApiCall("POST", `/api/v1/products/{productId}/content/${id ?? "{contentId}"}/transition`, { from: "Draft", to: "In Review" });
    const row = id ? contentStore.find((c) => c.id === id) : undefined;
    if (row) row.status = "In Review";
  },
  async publish(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/transition", { from: "In Review", to: "Published" });
  },
  async archive(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/transition", { from: "Published", to: "Archived" });
  },
  async saveDraft(): Promise<void> {
    logApiCall("PUT", "/api/v1/products/{productId}/content/{contentId}");
  },
  async schedulePublish(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/content/{contentId}/transition", { from: "In Review", to: "Published", scheduled: true });
  },
};

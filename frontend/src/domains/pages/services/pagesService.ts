import { pagesByProduct } from "../mocks/pages.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { CreatePageRequest, CreateSectionRequest, ReorderSectionsRequest, UpdateSectionRequest } from "../contracts/requests";
import { BLOCK_TYPES, type BlockType, type ListPagesResponse, type Page, type Section } from "../contracts/responses";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const pagesStore: Page[] = Object.values(pagesByProduct).flat();

function nextSectionId(page: Page): string {
  return `${page.id}-s${page.sections.length + 1}`;
}

/** Busca o objeto real do store, para uso interno de mutação — nunca exposto ao chamador. */
function findStorePage(productSlug: string, pageId: string): Page | undefined {
  return pagesStore.find((p) => p.productSlug === productSlug && p.id === pageId);
}

/**
 * Clona página e seções antes de devolver ao chamador. Sem isto, o
 * componente guardava a MESMA referência de array do store em seu estado
 * local; um `createSection` subsequente (`page.sections.push(...)`) mutava
 * esse array por baixo dos panos, e o `setPage({ ...page, sections:
 * [...page.sections, created] })` do ContentEditor acabava duplicando o
 * item recém-criado (causa do warning de key duplicada `mb-home-s6`).
 */
function clonePage(page: Page): Page {
  return { ...page, sections: page.sections.map((s) => ({ ...s })) };
}

export const pagesService = {
  /**
   * Catálogo de `BlockType` (Sprint 13, Tarefa H.1) — hoje devolve a
   * constante `BLOCK_TYPES` (mock), mas todo código novo consulta este
   * método, nunca a constante direto. Prepara a troca para um catálogo
   * vindo do backend (que pode variar por tipo de produto/plano) sem exigir
   * outro refactor.
   */
  async listBlockTypes(): Promise<readonly BlockType[]> {
    return BLOCK_TYPES;
  },

  async listPages(productSlug: string): Promise<ListPagesResponse> {
    return pagesStore.filter((p) => p.productSlug === productSlug).map(clonePage);
  },

  async getPage(productSlug: string, pageId: string): Promise<Page | undefined> {
    const page = findStorePage(productSlug, pageId);
    return page ? clonePage(page) : undefined;
  },

  async getPageBySlug(productSlug: string, slug: string): Promise<Page | undefined> {
    const page = pagesStore.find((p) => p.productSlug === productSlug && p.slug === slug);
    return page ? clonePage(page) : undefined;
  },

  // Pontos de integração real (Sprint 11, Tarefa B.2 / docs/trace/00_endpoints_esperados.md, Seção D.1).

  async createPage(productSlug: string, req: CreatePageRequest): Promise<Page> {
    logApiCall("POST", `/api/v1/products/${productSlug}/pages`, req);
    const created: Page = {
      id: `${productSlug}-${req.slug}`,
      productSlug,
      slug: req.slug,
      title: req.title,
      locale: req.locale,
      status: "draft",
      version: 1,
      seo: {},
      sections: [],
    };
    pagesStore.push(created);
    return clonePage(created);
  },

  async deletePage(productSlug: string, pageId: string): Promise<void> {
    const index = pagesStore.findIndex((p) => p.productSlug === productSlug && p.id === pageId);
    if (index < 0) return;
    logApiCall("DELETE", `/api/v1/products/${productSlug}/pages/${pageId}`);
    pagesStore.splice(index, 1);
  },

  async updatePage(productSlug: string, pageId: string, patch: Partial<Pick<Page, "title" | "status" | "seo">>): Promise<Page> {
    const page = findStorePage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    logApiCall("PUT", `/api/v1/products/${productSlug}/pages/${pageId}`, patch);
    Object.assign(page, patch);
    page.version += 1;
    return clonePage(page);
  },

  async createSection(productSlug: string, pageId: string, req: CreateSectionRequest): Promise<Section> {
    const page = findStorePage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    const section: Section = { id: nextSectionId(page), order: page.sections.length, ...req };
    logApiCall("POST", `/api/v1/products/${productSlug}/pages/${pageId}/sections`, req);
    page.sections.push(section);
    page.version += 1;
    return { ...section };
  },

  async updateSection(productSlug: string, pageId: string, sectionId: string, req: UpdateSectionRequest): Promise<Section> {
    const page = findStorePage(productSlug, pageId);
    const section = page?.sections.find((s) => s.id === sectionId);
    if (!page || !section) throw { status: 404, message: `Seção ${sectionId} não encontrada.` };
    logApiCall("PUT", `/api/v1/products/${productSlug}/pages/${pageId}/sections/${sectionId}`, req);
    Object.assign(section, req);
    page.version += 1;
    return { ...section };
  },

  async deleteSection(productSlug: string, pageId: string, sectionId: string): Promise<void> {
    const page = findStorePage(productSlug, pageId);
    if (!page) return;
    logApiCall("DELETE", `/api/v1/products/${productSlug}/pages/${pageId}/sections/${sectionId}`);
    page.sections = page.sections.filter((s) => s.id !== sectionId);
    page.version += 1;
  },

  async reorderSections(productSlug: string, pageId: string, req: ReorderSectionsRequest): Promise<Page> {
    const page = findStorePage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    const byId = new Map(page.sections.map((s) => [s.id, s]));
    logApiCall("PUT", `/api/v1/products/${productSlug}/pages/${pageId}/sections/reorder`, req);
    page.sections = req.sectionIds.map((id, order) => {
      const section = byId.get(id);
      if (!section) throw { status: 400, message: `Seção ${id} não pertence a esta página.` };
      return { ...section, order };
    });
    page.version += 1;
    return clonePage(page);
  },
};

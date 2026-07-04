import { pagesByProduct } from "../mocks/pages.mocks";
import { products as productMocks } from "../../products/mocks/products.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { CreatePageRequest, CreateSectionRequest, ReorderSectionsRequest, UpdateSectionRequest } from "../contracts/requests";
import { BLOCK_TYPES, type BlockType, type ListPagesResponse, type Page, type Section } from "../contracts/responses";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const pagesStore: Page[] = Object.values(pagesByProduct).flat();

function nextSectionId(page: Page): string {
  return `${page.id}-s${page.sections.length + 1}`;
}

function mockProductSlug(productId: string): string {
  const product = productMocks.find((p) => p.id === productId);
  return product ? product.name.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") : productId;
}

/** Busca o objeto real do store, para uso interno de mutação — nunca exposto ao chamador. */
function findStorePage(productSlug: string, pageId: string): Page | undefined {
  return pagesStore.find((p) => p.productSlug === productSlug && p.id === pageId);
}

type PageDto = {
  id: string;
  slug: string;
  title: string;
  locale: string;
  status: string;
  version: number;
  seo?: Page["seo"] | null;
  sections?: SectionDto[];
};

type SectionDto = {
  id: string;
  type: BlockType;
  variant?: string | null;
  order: number;
  content?: Record<string, unknown> | null;
  settings?: Record<string, unknown> | null;
};

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

function mapSection(dto: SectionDto): Section {
  return {
    id: dto.id,
    type: dto.type,
    label: dto.variant ?? dto.type,
    order: dto.order,
    content: dto.content ?? {},
    settings: dto.settings ?? undefined,
  };
}

function mapPage(dto: PageDto, productId: string): Page {
  return {
    id: dto.id,
    productSlug: productId,
    slug: dto.slug,
    title: dto.title,
    locale: dto.locale,
    status: dto.status.toLowerCase() as Page["status"],
    version: dto.version,
    seo: dto.seo ?? {},
    sections: (dto.sections ?? []).map(mapSection),
  };
}

function toCreateSectionDto(req: CreateSectionRequest, order: number) {
  return { type: req.type, variant: req.label, order, content: req.content, settings: req.settings };
}

function toUpdateSectionDto(current: Section, req: UpdateSectionRequest) {
  const next = { ...current, ...req };
  return { type: next.type, variant: next.label, order: next.order, content: next.content, settings: next.settings };
}

function toUpdatePageDto(current: Page, patch: Partial<Pick<Page, "title" | "status" | "seo">>) {
  return {
    slug: current.slug,
    title: patch.title ?? current.title,
    locale: current.locale,
    status: patch.status ?? current.status,
    seo: patch.seo ?? current.seo,
  };
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
    // Catálogo estático por enquanto em ambos os modos.
    return BLOCK_TYPES;
  },

  async listPages(productId: string): Promise<ListPagesResponse> {
    if (IS_API_MODE) {
      const dtos = await apiClient.get<PageDto[]>(`/products/${productId}/pages`);
      return dtos.map((dto) => mapPage(dto, productId));
    }
    const productSlug = mockProductSlug(productId);
    return pagesStore.filter((p) => p.productSlug === productSlug).map(clonePage);
  },

  async getPage(productId: string, pageId: string): Promise<Page | undefined> {
    if (IS_API_MODE) {
      const dto = await apiClient.get<PageDto>(`/products/${productId}/pages/${pageId}`);
      return mapPage(dto, productId);
    }
    const productSlug = mockProductSlug(productId);
    const page = findStorePage(productSlug, pageId);
    return page ? clonePage(page) : undefined;
  },

  async getPageBySlug(productId: string, slug: string): Promise<Page | undefined> {
    if (IS_API_MODE) {
      const pages = await this.listPages(productId);
      const page = pages.find((p) => p.slug === slug);
      return page ? this.getPage(productId, page.id) : undefined;
    }
    const productSlug = mockProductSlug(productId);
    const page = pagesStore.find((p) => p.productSlug === productSlug && p.slug === slug);
    return page ? clonePage(page) : undefined;
  },

  // Pontos de integração real (Sprint 11, Tarefa B.2 / docs/trace/00_endpoints_esperados.md, Seção D.1).

  async createPage(productId: string, req: CreatePageRequest): Promise<Page> {
    if (IS_API_MODE) {
      const dto = await apiClient.post<PageDto>(`/products/${productId}/pages`, req);
      return mapPage(dto, productId);
    }
    const productSlug = mockProductSlug(productId);
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

  async deletePage(productId: string, pageId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productId}/pages/${pageId}`);
    const productSlug = mockProductSlug(productId);
    const index = pagesStore.findIndex((p) => p.productSlug === productSlug && p.id === pageId);
    if (index < 0) return;
    logApiCall("DELETE", `/api/v1/products/${productSlug}/pages/${pageId}`);
    pagesStore.splice(index, 1);
  },

  async updatePage(productId: string, pageId: string, patch: Partial<Pick<Page, "title" | "status" | "seo">>): Promise<Page> {
    if (IS_API_MODE) {
      const current = await this.getPage(productId, pageId);
      if (!current) throw { status: 404, message: `Página ${pageId} não encontrada.` };
      const dto = await apiClient.put<PageDto>(`/products/${productId}/pages/${pageId}`, toUpdatePageDto(current, patch));
      return mapPage(dto, productId);
    }
    const productSlug = mockProductSlug(productId);
    const page = findStorePage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    logApiCall("PUT", `/api/v1/products/${productSlug}/pages/${pageId}`, patch);
    Object.assign(page, patch);
    page.version += 1;
    return clonePage(page);
  },

  async createSection(productId: string, pageId: string, req: CreateSectionRequest): Promise<Section> {
    if (IS_API_MODE) {
      const current = await this.getPage(productId, pageId);
      const order = current?.sections.length ?? 0;
      const dto = await apiClient.post<SectionDto>(`/products/${productId}/pages/${pageId}/sections`, toCreateSectionDto(req, order));
      return mapSection(dto);
    }
    const productSlug = mockProductSlug(productId);
    const page = findStorePage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    const section: Section = { id: nextSectionId(page), order: page.sections.length, ...req };
    logApiCall("POST", `/api/v1/products/${productSlug}/pages/${pageId}/sections`, req);
    page.sections.push(section);
    page.version += 1;
    return { ...section };
  },

  async updateSection(productId: string, pageId: string, sectionId: string, req: UpdateSectionRequest): Promise<Section> {
    if (IS_API_MODE) {
      const current = await this.getPage(productId, pageId);
      const section = current?.sections.find((s) => s.id === sectionId);
      if (!section) throw { status: 404, message: `Seção ${sectionId} não encontrada.` };
      const dto = await apiClient.put<SectionDto>(`/products/${productId}/pages/${pageId}/sections/${sectionId}`, toUpdateSectionDto(section, req));
      return mapSection(dto);
    }
    const productSlug = mockProductSlug(productId);
    const page = findStorePage(productSlug, pageId);
    const section = page?.sections.find((s) => s.id === sectionId);
    if (!page || !section) throw { status: 404, message: `Seção ${sectionId} não encontrada.` };
    logApiCall("PUT", `/api/v1/products/${productSlug}/pages/${pageId}/sections/${sectionId}`, req);
    Object.assign(section, req);
    page.version += 1;
    return { ...section };
  },

  async deleteSection(productId: string, pageId: string, sectionId: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productId}/pages/${pageId}/sections/${sectionId}`);
    const productSlug = mockProductSlug(productId);
    const page = findStorePage(productSlug, pageId);
    if (!page) return;
    logApiCall("DELETE", `/api/v1/products/${productSlug}/pages/${pageId}/sections/${sectionId}`);
    page.sections = page.sections.filter((s) => s.id !== sectionId);
    page.version += 1;
  },

  async reorderSections(productId: string, pageId: string, req: ReorderSectionsRequest): Promise<Page> {
    if (IS_API_MODE) {
      await apiClient.put<SectionDto[]>(`/products/${productId}/pages/${pageId}/sections/reorder`, req);
      const page = await this.getPage(productId, pageId);
      if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
      return page;
    }
    const productSlug = mockProductSlug(productId);
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

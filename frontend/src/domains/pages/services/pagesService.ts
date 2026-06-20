import { pagesByProduct } from "../mocks/pages.mocks";
import type { CreateSectionRequest, ReorderSectionsRequest, UpdateSectionRequest } from "../contracts/requests";
import type { ListPagesResponse, Page, Section } from "../contracts/responses";

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts.
const pagesStore: Page[] = Object.values(pagesByProduct).flat();

function nextSectionId(page: Page): string {
  return `${page.id}-s${page.sections.length + 1}`;
}

export const pagesService = {
  async listPages(productSlug: string): Promise<ListPagesResponse> {
    return pagesStore.filter((p) => p.productSlug === productSlug);
  },

  async getPage(productSlug: string, pageId: string): Promise<Page | undefined> {
    return pagesStore.find((p) => p.productSlug === productSlug && p.id === pageId);
  },

  async updatePage(productSlug: string, pageId: string, patch: Partial<Pick<Page, "title" | "status" | "seo">>): Promise<Page> {
    const page = await this.getPage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    Object.assign(page, patch);
    page.version += 1;
    return page;
  },

  async createSection(productSlug: string, pageId: string, req: CreateSectionRequest): Promise<Section> {
    const page = await this.getPage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    const section: Section = { id: nextSectionId(page), order: page.sections.length, ...req };
    page.sections.push(section);
    page.version += 1;
    return section;
  },

  async updateSection(productSlug: string, pageId: string, sectionId: string, req: UpdateSectionRequest): Promise<Section> {
    const page = await this.getPage(productSlug, pageId);
    const section = page?.sections.find((s) => s.id === sectionId);
    if (!page || !section) throw { status: 404, message: `Seção ${sectionId} não encontrada.` };
    Object.assign(section, req);
    page.version += 1;
    return section;
  },

  async deleteSection(productSlug: string, pageId: string, sectionId: string): Promise<void> {
    const page = await this.getPage(productSlug, pageId);
    if (!page) return;
    page.sections = page.sections.filter((s) => s.id !== sectionId);
    page.version += 1;
  },

  async reorderSections(productSlug: string, pageId: string, req: ReorderSectionsRequest): Promise<Page> {
    const page = await this.getPage(productSlug, pageId);
    if (!page) throw { status: 404, message: `Página ${pageId} não encontrada.` };
    const byId = new Map(page.sections.map((s) => [s.id, s]));
    page.sections = req.sectionIds.map((id, order) => {
      const section = byId.get(id);
      if (!section) throw { status: 400, message: `Seção ${id} não pertence a esta página.` };
      return { ...section, order };
    });
    page.version += 1;
    return page;
  },
};

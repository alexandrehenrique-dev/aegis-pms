import type { BlockType } from "../../domains/pages/contracts/responses";
import type { ProductTypeKey } from "./moduleDefaults";

/**
 * Esqueleto de páginas por tipo de produto (ADR-0017, Sprint 17, Tarefa A) —
 * mesmas páginas/seções/labels que `domains/pages/mocks/pages.mocks.ts` já
 * usa para Maestro Beton/CMSS/Conecta Talentos/Alexandre Dev. Tipos sem
 * entrada aqui ("Knowledge Base", "Library/Books/Music", "Produto SaaS",
 * "Custom") não geram nenhuma página na criação do produto.
 */
export type PageSkeletonSection = { type: BlockType; label: string };
export type PageSkeleton = { slug: string; title: string; sections: PageSkeletonSection[] };

export const PRODUCT_PAGE_SKELETONS: Partial<Record<ProductTypeKey, PageSkeleton[]>> = {
  "Site Institucional": [
    { slug: "home", title: "Home", sections: [{ type: "hero", label: "Hero" }, { type: "feature-grid", label: "Pilares" }, { type: "event-list", label: "Próximos eventos" }, { type: "cta-section", label: "CTA" }] },
    { slug: "quem-somos", title: "Quem Somos", sections: [{ type: "image-text", label: "Apresentação" }, { type: "two-column", label: "Missão e Valores" }] },
    { slug: "historia", title: "História", sections: [{ type: "timeline", label: "Linha do tempo" }] },
    { slug: "agenda", title: "Agenda", sections: [{ type: "event-list", label: "Agenda completa" }] },
    { slug: "galeria", title: "Galeria", sections: [{ type: "gallery", label: "Galeria de fotos" }] },
    { slug: "apoie", title: "Apoie", sections: [{ type: "rich-text", label: "Como apoiar" }, { type: "faq", label: "Perguntas frequentes" }] },
    { slug: "contato", title: "Contato", sections: [{ type: "contact", label: "Fale com a gente" }] },
  ],
  Portal: [
    { slug: "home", title: "Home", sections: [{ type: "hero", label: "Hero" }, { type: "text", label: "Quem Somos" }, { type: "card-list", label: "Vagas" }, { type: "card-list", label: "Blog" }] },
  ],
  Portfolio: [
    { slug: "home", title: "Home", sections: [{ type: "hero", label: "Hero" }, { type: "card-list", label: "Projetos" }, { type: "feature-grid", label: "Skills" }, { type: "timeline", label: "Experiência" }, { type: "download", label: "Downloads" }] },
  ],
  // "Knowledge Base", "Library/Books/Music", "Produto SaaS", "Custom": sem entrada — sem esqueleto de páginas.
};

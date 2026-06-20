import type { ProductGlobals } from "../contracts/globals";

function emptyGlobals(): ProductGlobals {
  return {
    navbar: { links: [{ label: "Home", href: "/" }] },
    footer: { links: [] },
    socialLinks: [],
  };
}

/**
 * Seed de `ProductGlobals` (Sprint 13, Tarefa G) — `cmss` migra os dados que
 * antes viviam na seção `footer` de `cmss-contato` (`pages.mocks.ts`); os
 * demais produtos nascem com um navbar mínimo (link para Home) e o resto
 * vazio, editável em `/products/:slug/globals`.
 */
export const globalsByProduct: Record<string, ProductGlobals> = {
  "maestro-beton": emptyGlobals(),
  "conecta-talentos": emptyGlobals(),
  "alexandre-dev": emptyGlobals(),
  cmss: {
    navbar: {
      links: [
        { label: "Home", href: "/" },
        { label: "Quem Somos", href: "/quem-somos" },
        { label: "História", href: "/historia" },
        { label: "Agenda", href: "/agenda" },
        { label: "Apoie", href: "/apoie" },
        { label: "Contato", href: "/contato" },
      ],
    },
    footer: {
      addressText: "Praça Central, s/n — São Sebastião",
      links: [{ label: "Home", href: "/" }, { label: "Contato", href: "/contato" }],
    },
    socialLinks: [{ platform: "instagram", href: "" }, { platform: "facebook", href: "" }],
  },
};

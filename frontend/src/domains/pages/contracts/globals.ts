/**
 * `ProductGlobals` (ADR-0013, Sprint 13, Tarefa G) — navbar, footer e redes
 * sociais não são mais `BlockType` de página; são uma configuração única por
 * produto, editada em `/products/:slug/globals` e renderizada
 * automaticamente em toda página (ver `GlobalNavbar`/`GlobalFooter`).
 */
export type NavLink = { label: string; href: string };
export type SocialLink = { platform: string; href: string };

export type ProductGlobals = {
  navbar: { logoAssetId?: string; links: NavLink[] };
  footer: { addressText?: string; links: NavLink[] };
  socialLinks: SocialLink[];
  floatingWhatsapp?: { enabled: boolean; number: string; message: string };
};

export type UpdateGlobalsRequest = Partial<ProductGlobals>;

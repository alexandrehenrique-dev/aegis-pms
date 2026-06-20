import type { ProductGlobals } from "../contracts/globals";

/**
 * Navbar e footer globais por produto (ADR-0013, Sprint 13, Tarefa G.4) —
 * substituem os antigos `BlockType: "navbar"/"footer"`. Renderizados pelo
 * preview/produção automaticamente em volta das seções de qualquer página;
 * a página em si nunca mais inclui navbar/footer nas próprias seções.
 */
export function GlobalNavbar({ globals }: { globals: ProductGlobals }) {
  return (
    <div className="flex gap-4 border-b border-border p-4 text-sm">
      {globals.navbar.links.map((link, i) => <span key={i}>{link.label}</span>)}
    </div>
  );
}

export function GlobalFooter({ globals }: { globals: ProductGlobals }) {
  return (
    <div className="border-t border-border p-6 text-sm text-muted-foreground">
      {globals.footer.addressText ? <p>{globals.footer.addressText}</p> : null}
      <div className="mt-2 flex flex-wrap gap-3">
        {globals.footer.links.map((link, i) => <span key={i}>{link.label}</span>)}
        {globals.socialLinks.map((s, i) => <span key={i} className="rounded-full border border-border px-2 py-0.5">{s.platform}</span>)}
      </div>
    </div>
  );
}

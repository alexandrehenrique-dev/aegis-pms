import { useEffect, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { motion } from "motion/react";
import { X } from "lucide-react";

export type ContextMenuTarget = { x: number; y: number };
export type ActionMenuItem = { label: string; icon: ReactNode; destructive?: boolean; onClick: () => void };

/**
 * Menu de ações (Editar/Excluir etc.) com duas apresentações pela mesma
 * marcação — só CSS decide qual aparece, sem JS de breakpoint:
 * - desktop (`lg:` e acima): painel flutuante na posição do clique direito,
 *   igual ao que TenantContextMenu/ProductContextMenu já faziam;
 * - mobile (abaixo de `lg`): painel de tela cheia ancorado na base, mesma
 *   linguagem visual do overlay mobile da navegação principal
 *   (`AppShell.tsx`, state `mobile`) — botão direito/long-press não é uma
 *   interação confiável em touch, então quem abre isto em mobile é um botão
 *   "⋮" visível no card, não o gesto de contexto.
 *
 * Ambas as variantes ficam dentro do MESMO elemento com `ref` para que o
 * listener de "clique fora" trate cliques em qualquer uma delas como
 * "dentro" — sem isso, fechar a variante mobile via clique num item
 * disparada o listener global como se fosse um clique externo antes da
 * ação rodar.
 *
 * Renderiza via `createPortal` direto em `document.body`: TenantSelectScreen/
 * ProductSelectScreen embrulham a tela inteira num `motion.div` animado
 * (`fade`), e qualquer ancestral com `transform` (framer-motion aplica isso
 * via inline style) vira o "containing block" de elementos `fixed` — sem o
 * portal, o painel de tela cheia em mobile ficava confinado à caixa daquele
 * ancestral em vez do viewport real, e não aparecia como esperado.
 */
export function ContextActionMenu({ position, items, onClose }: { position: ContextMenuTarget; items: ActionMenuItem[]; onClose: () => void }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) onClose(); };
    const handleEsc = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("click", handleClick);
    window.addEventListener("keydown", handleEsc);
    return () => { window.removeEventListener("click", handleClick); window.removeEventListener("keydown", handleEsc); };
  }, [onClose]);

  return createPortal(
    <div ref={ref}>
      <div style={{ position: "fixed", left: position.x, top: position.y, zIndex: 60 }} className="hidden w-44 rounded-xl border border-border bg-card p-1.5 shadow-[0_8px_32px_rgba(0,0,0,0.16)] lg:block">
        {items.map((item) => (
          <button key={item.label} onClick={item.onClick} className={`flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm transition hover:bg-muted ${item.destructive ? "text-destructive hover:bg-destructive/10" : ""}`}>
            {item.icon}{item.label}
          </button>
        ))}
      </div>

      <motion.div
        className="fixed inset-0 z-[60] bg-black/20 lg:hidden"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        onClick={onClose}
      >
        <motion.div
          className="absolute inset-x-0 bottom-0 rounded-t-2xl border-t border-border bg-card p-2 pb-[max(0.5rem,env(safe-area-inset-bottom))]"
          initial={{ y: 280 }} animate={{ y: 0 }} exit={{ y: 280 }}
        >
          <div className="flex items-center justify-between px-2 py-2">
            <p className="text-sm font-medium text-muted-foreground">Ações</p>
            <button onClick={onClose} className="rounded-full p-1.5 hover:bg-muted"><X size={16} /></button>
          </div>
          {items.map((item) => (
            <button key={item.label} onClick={item.onClick} className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm transition hover:bg-muted ${item.destructive ? "text-destructive" : ""}`}>
              {item.icon}{item.label}
            </button>
          ))}
        </motion.div>
      </motion.div>
    </div>,
    document.body,
  );
}

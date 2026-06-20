import { useState, type ReactNode } from "react";
import { AnimatePresence, motion } from "motion/react";
import { Menu, X } from "lucide-react";

/**
 * Padrão hamburguer + drawer usado pelo `AppShell` (Sprint 12, Tarefa I) —
 * extraído para componente reutilizável para que qualquer tela fora do
 * workspace logado (ex.: `TenantSelectScreen`, `ProductSelectScreen`) tenha
 * o mesmo padrão mobile para ações de nível de tela, em vez de cada tela
 * reinventar seu próprio menu apertado.
 */
export function MobileDrawerMenu({ children, label = "Menu", title = "Ações" }: { children: ReactNode; label?: string; title?: string }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button onClick={() => setOpen(true)} aria-label={label} className="rounded-xl border border-border bg-card p-2 transition hover:bg-muted lg:hidden">
        <Menu size={17} />
      </button>
      <AnimatePresence>
        {open && (
          <motion.div className="fixed inset-0 z-50 bg-black/20" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setOpen(false)}>
            <motion.div
              className="absolute right-0 top-0 flex h-full w-[280px] flex-col bg-card p-4"
              initial={{ x: 280 }}
              animate={{ x: 0 }}
              exit={{ x: 280 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="mb-3 flex items-center justify-between">
                <p className="font-semibold">{title}</p>
                <button onClick={() => setOpen(false)} aria-label="Fechar menu" className="rounded-full bg-muted p-2"><X size={16} /></button>
              </div>
              <div className="space-y-2">{children}</div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}

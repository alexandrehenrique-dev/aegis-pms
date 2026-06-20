import { useState } from "react";
import { useNavigate } from "react-router";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "motion/react";
import { Bell, X } from "lucide-react";
import { fade } from "./Primitives";
import { timeline, timelineRoutes } from "../../mocks/timeline";

/**
 * Lista de notificações compartilhada pelas duas apresentações abaixo —
 * extraída para não duplicar o `.map` entre a variante desktop e mobile.
 */
function NotificationItems({ read, onSelect }: { read: boolean; onSelect: (route: string) => void }) {
  return (
    <>
      {timeline.map((t, i) => (
        <button key={t} onClick={() => onSelect(timelineRoutes[i] ?? "/dashboard")} className="flex w-full items-start gap-3 rounded-xl p-3 text-left transition hover:bg-muted">
          <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${read ? "bg-muted" : "bg-primary"}`} />
          <div>
            <p className="text-sm font-medium leading-snug">{t}</p>
            <p className="mt-0.5 text-xs text-muted-foreground">{i % 2 ? "Produto" : "Sistema"} · há {i + 1} min</p>
          </div>
        </button>
      ))}
    </>
  );
}

/**
 * Sprint 13, Tarefa P — em telas estreitas, o dropdown ancorado (`absolute
 * right-0`) media sua largura a partir do botão do sino, não da borda da
 * viewport; como o sino não fica colado na borda direita real da tela em
 * mobile (há outros ícones depois dele), o painel abria estourando para a
 * ESQUERDA da viewport (texto cortado, sem scroll). Mesmo padrão de duas
 * apresentações por CSS já usado em `ContextActionMenu`: painel ancorado em
 * `lg:` e acima, bottom sheet de tela cheia abaixo de `lg`. Portal para
 * `document.body` pela mesma razão documentada lá — qualquer ancestral com
 * `transform` (motion) vira containing block de um `fixed` interno.
 */
export function Notifications() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [read, setRead] = useState(false);

  const handleSelect = (route: string) => {
    navigate(route);
    setOpen(false);
  };

  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className="relative rounded-xl border border-border bg-card p-2 transition hover:bg-muted">
        <Bell size={16} />
        {!read && <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-primary" />}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div {...fade} className="absolute right-0 top-11 z-40 hidden w-[360px] rounded-2xl border border-border bg-card shadow-[0_8px_32px_rgba(0,0,0,0.12)] dark:shadow-[0_8px_32px_rgba(0,0,0,0.4)] lg:block" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between border-b border-border px-4 py-3">
              <h3 className="text-sm font-semibold">Notificações</h3>
              <button onClick={() => { setRead(true); setOpen(false); }} className="text-xs text-muted-foreground transition hover:text-foreground">Marcar lidas</button>
            </div>
            <div className="p-2">
              <NotificationItems read={read} onSelect={handleSelect} />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {open &&
        createPortal(
          <motion.div
            className="fixed inset-0 z-40 bg-black/20 lg:hidden"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={() => setOpen(false)}
          >
            <motion.div
              className="absolute inset-x-0 bottom-0 max-h-[80vh] overflow-auto rounded-t-2xl border-t border-border bg-card pb-[max(0.5rem,env(safe-area-inset-bottom))]"
              initial={{ y: 320 }} animate={{ y: 0 }} exit={{ y: 320 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-border px-4 py-3">
                <h3 className="text-sm font-semibold">Notificações</h3>
                <div className="flex items-center gap-2">
                  <button onClick={() => { setRead(true); setOpen(false); }} className="text-xs text-muted-foreground transition hover:text-foreground">Marcar lidas</button>
                  <button onClick={() => setOpen(false)} aria-label="Fechar" className="rounded-full p-1.5 hover:bg-muted"><X size={16} /></button>
                </div>
              </div>
              <div className="p-2">
                <NotificationItems read={read} onSelect={handleSelect} />
              </div>
            </motion.div>
          </motion.div>,
          document.body,
        )}
    </div>
  );
}

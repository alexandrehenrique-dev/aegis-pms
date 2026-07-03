import { useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { CheckCircle2, ChevronDown, Search } from "lucide-react";
import { EmptyState } from "./Primitives";
import { fade } from "./motion";

export type SwitcherItem = { id: string; name: string; meta?: string };

export function Switcher({ label, active, items, onSelect }: { label: string; active?: string; items: SwitcherItem[]; onSelect?: (item: SwitcherItem) => void }) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const filtered = items.filter((i) => i.name.toLowerCase().includes(q.toLowerCase()));
  const current = active ?? items[0]?.name ?? label;
  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-sm transition hover:bg-muted">
        <span className="hidden text-[11px] text-muted-foreground sm:inline">{label}</span>
        <b className="text-foreground">{current}</b>
        <ChevronDown size={13} className={`transition-transform duration-200 ${open ? "rotate-180" : ""}`} />
      </button>
      <AnimatePresence>
        {open && (
          <motion.div {...fade} className="absolute left-0 top-11 z-40 w-[min(300px,calc(100vw-24px))] rounded-xl border border-border bg-card p-2 shadow-[0_8px_32px_rgba(0,0,0,0.12)] dark:shadow-[0_8px_32px_rgba(0,0,0,0.4)]">
            <div className="mb-2 flex items-center gap-2 rounded-lg border border-border bg-muted/40 px-2 py-1.5">
              <Search size={14} className="text-muted-foreground" />
              <input autoFocus value={q} onChange={(e) => setQ(e.target.value)} placeholder={`Buscar ${label.toLowerCase()}`} className="w-full bg-transparent text-sm outline-none" />
            </div>
            {filtered.length ? filtered.map((item) => (
              <button key={item.id} onClick={() => { onSelect?.(item); setOpen(false); setQ(""); }} className="flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm transition hover:bg-muted">
                <div className="min-w-0">
                  <span className={item.name === current ? "font-medium" : ""}>{item.name}</span>
                  {item.meta && <span className="ml-2 text-xs text-muted-foreground">{item.meta}</span>}
                </div>
                {item.name === current && <CheckCircle2 size={14} className="shrink-0 text-primary" />}
              </button>
            )) : <EmptyState compact title="Sem resultados" description="Nenhum item encontrado." />}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

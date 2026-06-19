import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { Bell } from "lucide-react";
import { fade } from "./Primitives";
import { timeline, timelineRoutes } from "../../mocks/timeline";

export function Notifications() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [read, setRead] = useState(false);
  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className="relative rounded-xl border border-border bg-card p-2 transition hover:bg-muted">
        <Bell size={16} />
        {!read && <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-primary" />}
      </button>
      <AnimatePresence>
        {open && (
          <motion.div {...fade} className="absolute right-0 top-11 z-40 w-[min(360px,calc(100vw-24px))] rounded-2xl border border-border bg-card shadow-[0_8px_32px_rgba(0,0,0,0.12)] dark:shadow-[0_8px_32px_rgba(0,0,0,0.4)]" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between border-b border-border px-4 py-3">
              <h3 className="text-sm font-semibold">Notificações</h3>
              <button onClick={() => { setRead(true); setOpen(false); }} className="text-xs text-muted-foreground transition hover:text-foreground">Marcar lidas</button>
            </div>
            <div className="p-2">
              {timeline.map((t, i) => (
                <button key={t} onClick={() => { navigate(timelineRoutes[i] ?? "/dashboard"); setOpen(false); }} className="flex w-full items-start gap-3 rounded-xl p-3 text-left transition hover:bg-muted">
                  <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${read ? "bg-muted" : "bg-primary"}`} />
                  <div>
                    <p className="text-sm font-medium leading-snug">{t}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">{i % 2 ? "Produto" : "Sistema"} · há {i + 1} min</p>
                  </div>
                </button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

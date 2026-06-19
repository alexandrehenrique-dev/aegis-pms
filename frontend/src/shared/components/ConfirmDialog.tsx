import type { ReactNode } from "react";
import { motion } from "motion/react";
import { Loader2 } from "lucide-react";
import { Button, fade } from "./Primitives";

export function ConfirmDialog({ title, desc, onConfirm, onCancel, danger = false, loading = false, children, confirmDisabled = false }: { title: string; desc: string; onConfirm: () => void; onCancel: () => void; danger?: boolean; loading?: boolean; children?: ReactNode; confirmDisabled?: boolean }) {
  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onCancel}>
      <motion.div {...fade} className="w-full max-w-sm rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-semibold">{title}</h3>
        <p className="mt-2 text-sm text-muted-foreground">{desc}</p>
        {children && <div className="mt-4">{children}</div>}
        <div className="mt-5 flex justify-end gap-2">
          <Button onClick={onCancel}>Cancelar</Button>
          <button onClick={onConfirm} disabled={loading || confirmDisabled} className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm transition active:scale-[0.97] disabled:opacity-50 ${danger ? "border-destructive bg-destructive text-destructive-foreground hover:bg-destructive/90" : "border-primary bg-primary text-primary-foreground hover:bg-primary/90 shadow-[0_4px_14px_rgba(124,58,237,.25)]"}`}>
            {loading ? <><Loader2 size={15} className="animate-spin" />Processando...</> : "Confirmar"}
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}

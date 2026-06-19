import type { ReactNode } from "react";
import { motion } from "motion/react";
import { CheckCircle2, Circle, Loader2, AlertTriangle } from "lucide-react";
import { fade } from "./Primitives";

export type SaveStatus = "idle" | "dirty" | "saving" | "saved" | "error";

export function FloatingSaveStatus({ status, onRetry }: { status: SaveStatus; onRetry?: () => void }) {
  if (status === "idle") return null;
  const cfg: Record<Exclude<SaveStatus, "idle">, { cls: string; icon: ReactNode; text: string }> = {
    dirty: { cls: "border-[#fef3c7] bg-[#fef3c7]/95 text-[#b45309]", icon: <Circle size={8} className="fill-current" />, text: "Alterações não salvas" },
    saving: { cls: "border-border bg-card/95 text-muted-foreground", icon: <Loader2 size={12} className="animate-spin" />, text: "Salvando..." },
    saved: { cls: "border-[#dcfce7] bg-[#dcfce7]/95 text-[#15803d]", icon: <CheckCircle2 size={12} />, text: "Salvo automaticamente" },
    error: { cls: "border-[#fee2e2] bg-[#fee2e2]/95 text-[#dc2626]", icon: <AlertTriangle size={12} />, text: "Erro ao salvar" },
  };
  const c = cfg[status];
  return (
    <motion.div {...fade} className={`fixed bottom-6 right-6 z-40 flex items-center gap-2 rounded-xl border px-3 py-2 text-xs shadow-[0_4px_16px_rgba(0,0,0,0.12)] backdrop-blur-sm ${c.cls}`}>
      {c.icon}{c.text}
      {status === "error" && onRetry && <button onClick={onRetry} className="ml-1 underline transition hover:opacity-80">Tentar novamente</button>}
    </motion.div>
  );
}

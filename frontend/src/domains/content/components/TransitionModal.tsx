import { useState } from "react";
import { motion } from "motion/react";
import { AlertTriangle, ChevronRight, Lock } from "lucide-react";
import { Badge, Button, fade } from "../../../shared/components/Primitives";
import type { UserRole } from "../../../shared/types";
import { wfBadgeTone, wfImpact, type PendingDrop, type WFStatus } from "../mocks/content.mocks";
import { wfCanTransition } from "./wfRules";

export function TransitionModal({ drop, onConfirm, onCancel, viewAsRole }: { drop: PendingDrop; onConfirm: (c: string) => void; onCancel: () => void; viewAsRole: UserRole }) {
  const [comment, setComment] = useState("");
  const { ok, reason } = wfCanTransition(drop.from, drop.to, viewAsRole);
  const required = drop.to === "Archived";
  const canConfirm = ok && (!required || comment.trim().length > 0);
  const toneOf = (s: WFStatus) => wfBadgeTone[s];
  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onCancel}>
      <motion.div {...fade} className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-semibold tracking-[-.02em]">Confirmar mudança de status?</h2>
        <div className="mt-4 space-y-3">
          <div className="rounded-xl bg-muted p-3">
            <p className="text-xs text-muted-foreground">Conteúdo afetado</p>
            <p className="mt-0.5 font-medium">{drop.item.title}</p>
            <p className="text-xs text-muted-foreground">{drop.item.type} · {drop.item.lang} · {drop.item.author} · {drop.item.version}</p>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex-1 rounded-xl border border-border bg-muted/40 px-3 py-2.5 text-center"><p className="text-[10px] text-muted-foreground">Status anterior</p><Badge tone={toneOf(drop.from)}>{drop.from}</Badge></div>
            <ChevronRight size={16} className="shrink-0 text-muted-foreground" />
            <div className="flex-1 rounded-xl border border-border bg-muted/40 px-3 py-2.5 text-center"><p className="text-[10px] text-muted-foreground">Novo status</p><Badge tone={toneOf(drop.to)}>{drop.to}</Badge></div>
          </div>
          {wfImpact[drop.to] && <div className="flex items-start gap-2 rounded-xl border border-[#fef3c7] bg-[#fef3c7]/60 p-3 text-xs text-[#b45309]"><AlertTriangle size={13} className="mt-0.5 shrink-0" />{wfImpact[drop.to]}</div>}
          {!ok && reason && <div className="flex items-start gap-2 rounded-xl border border-[#fee2e2] bg-[#fee2e2]/60 p-3 text-xs text-[#dc2626]"><Lock size={13} className="mt-0.5 shrink-0" />{reason}</div>}
          <div>
            <label className="mb-1 block text-sm font-medium">Comentário {required ? <span className="text-destructive">*</span> : <span className="text-xs font-normal text-muted-foreground">(opcional)</span>}</label>
            <textarea value={comment} onChange={(e) => setComment(e.target.value)} placeholder={required ? "Obrigatório para arquivamento..." : "Adicione contexto para a equipe..."} className="min-h-[68px] w-full resize-none rounded-xl border border-border bg-card p-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" />
          </div>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <Button onClick={onCancel}>Cancelar</Button>
          <Button primary onClick={() => canConfirm && onConfirm(comment)} disabled={!canConfirm}>{ok ? "Confirmar" : "Sem permissão"}</Button>
        </div>
      </motion.div>
    </motion.div>
  );
}

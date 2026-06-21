import type { ReactNode } from "react";
import { Loader2 } from "lucide-react";
import { Button } from "./Primitives";
import { ModalShell } from "./ModalShell";

export function ConfirmDialog({ title, desc, onConfirm, onCancel, danger = false, loading = false, children, confirmDisabled = false, confirmLabel = "Confirmar" }: { title: string; desc: string; onConfirm: () => void; onCancel: () => void; danger?: boolean; loading?: boolean; children?: ReactNode; confirmDisabled?: boolean; confirmLabel?: string }) {
  return (
    <ModalShell onClose={onCancel} maxWidthClassName="max-w-sm">
      <h3 className="font-semibold">{title}</h3>
      <p className="mt-2 text-sm text-muted-foreground">{desc}</p>
      {children && <div className="mt-4">{children}</div>}
      <div className="mt-5 flex justify-end gap-2">
        <Button onClick={onCancel}>Cancelar</Button>
        <button onClick={onConfirm} disabled={loading || confirmDisabled} className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm transition active:scale-[0.97] disabled:opacity-50 ${danger ? "border-destructive bg-destructive text-destructive-foreground hover:bg-destructive/90" : "border-primary bg-primary text-primary-foreground hover:bg-primary/90 shadow-[0_4px_14px_rgba(124,58,237,.25)]"}`}>
          {loading ? <><Loader2 size={15} className="animate-spin" />Processando...</> : confirmLabel}
        </button>
      </div>
    </ModalShell>
  );
}

import { useNavigate } from "react-router";
import { ArrowLeft } from "lucide-react";

/** Link fixo de "voltar" usado no wizard de Super Admin (Sprint 09, Tarefa C.4) — garante que o usuário nunca fique perdido no meio de um fluxo de múltiplas etapas. */
export function BackLink({ to, label }: { to: string; label: string }) {
  const navigate = useNavigate();
  return (
    <button onClick={() => navigate(to)} className="mb-4 flex items-center gap-1.5 text-sm text-muted-foreground transition hover:text-foreground">
      <ArrowLeft size={14} />{label}
    </button>
  );
}

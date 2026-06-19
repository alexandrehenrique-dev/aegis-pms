import { useNavigate } from "react-router";
import { CheckCircle2 } from "lucide-react";
import { Card } from "../../../shared/components/Primitives";
import { roleActions, roleLabels } from "../../../core/permissions/roles";
import type { UserRole } from "../../../shared/types";

export function QuickActions({ viewAsRole = "product_manager" }: { viewAsRole?: UserRole }) {
  const navigate = useNavigate();
  const actions = roleActions[viewAsRole] ?? roleActions.product_manager;
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Próximas ações</h2>
      <p className="mb-3 text-xs text-muted-foreground">{roleLabels[viewAsRole]} — ações recomendadas para o contexto atual</p>
      <div className="grid gap-2 md:grid-cols-2">
        {actions.map((a) => (
          <button key={a.label} onClick={() => navigate(a.path)} className="group rounded-xl border border-border p-3.5 text-left text-sm transition hover:border-primary/30 hover:bg-muted">
            <CheckCircle2 size={15} className="mb-1.5 text-primary transition group-hover:scale-110" />
            <p className="font-medium leading-snug">{a.label}</p>
            <p className="mt-0.5 text-[11px] text-muted-foreground">{a.desc}</p>
          </button>
        ))}
      </div>
    </Card>
  );
}

import { useAuth } from "../../../core/auth/useAuth";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { auditService } from "../../audit/services/auditService";

/**
 * F.4 (BUG-SPRINT consolidado) — antes, array literal hardcoded sempre
 * exibido independente de haver atividade real. Carrega eventos reais de
 * auditoria filtrados por `module=FORM`; estado vazio mostra mensagem, nunca
 * dados inventados (regra de ouro Z.2.5).
 */
export function FormsTimeline() {
  const { effectiveTenant, effectiveProduct } = useAuth();
  const { data: events } = useAsyncData(
    () => (effectiveTenant?.id
      ? auditService.listEvents(effectiveTenant.id, { productId: effectiveProduct?.id, module: "FORM", productName: effectiveProduct?.name })
      : Promise.resolve([])),
    [effectiveTenant?.id, effectiveProduct?.id],
  );

  if (!events || events.length === 0) {
    return <p className="p-3 text-sm text-muted-foreground">Nenhuma atividade recente.</p>;
  }

  return (
    <div className="space-y-1">
      {events.map((ev, i) => (
        <div key={ev.id ?? i} className="flex gap-3 rounded-xl p-3 hover:bg-muted">
          <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
          <div><p className="text-sm font-medium">{ev.action} — {ev.target}</p><p className="text-xs text-muted-foreground">{ev.time} · {effectiveProduct?.name ?? "Produto"} → Forms</p></div>
        </div>
      ))}
    </div>
  );
}

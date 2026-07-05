import { useAuth } from "../../../core/auth/useAuth";

export function FormsTimeline() {
  const { effectiveProduct } = useAuth();
  return (
    <div className="space-y-1">
      {["Novo orçamento recebido", "Novo contato enviado", "RSVP confirmado", "Formulário publicado"].map((t, i) => (
        <div key={t} className="flex gap-3 rounded-xl p-3 hover:bg-muted">
          <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
          <div><p className="text-sm font-medium">{t}</p><p className="text-xs text-muted-foreground">há {i + 1} h · {effectiveProduct?.name ?? "Produto"} → Forms</p></div>
        </div>
      ))}
    </div>
  );
}

import { useNavigate } from "react-router";
import { useAuth } from "../../core/auth/useAuth";
import { timeline, timelineRoutes } from "../../mocks/timeline";

export function OperationalTimeline() {
  const navigate = useNavigate();
  const { effectiveProduct } = useAuth();
  const entries = effectiveProduct
    ? timeline
      .map((label, index) => ({ label, path: timelineRoutes[index] ?? "/dashboard" }))
      .filter((entry) => entry.label.includes(effectiveProduct.name))
    : timeline.map((label, index) => ({ label, path: timelineRoutes[index] ?? "/dashboard" }));

  return (
    <div data-tour="dashboard-timeline" className="space-y-1">
      {entries.length === 0 && (
        <div className="rounded-xl border border-dashed border-border p-3 text-sm text-muted-foreground">
          Nenhuma atividade recente deste produto.
        </div>
      )}
      {entries.map((entry, i) => (
        <button key={entry.label} onClick={() => navigate(entry.path)} className="flex w-full gap-3 rounded-xl p-3 text-left transition hover:bg-muted">
          <div className="mt-1 flex flex-col items-center">
            <span className="h-2.5 w-2.5 rounded-full bg-primary" />
            {i < entries.length - 1 && <span className="mt-1 h-9 w-px bg-border" />}
          </div>
          <div>
            <p className="text-sm font-medium">{entry.label}</p>
            <p className="text-xs text-muted-foreground">há {i + 2} min · BYOP → Produto</p>
          </div>
        </button>
      ))}
    </div>
  );
}

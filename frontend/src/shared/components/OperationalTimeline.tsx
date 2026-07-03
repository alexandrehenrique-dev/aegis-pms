import { useNavigate } from "react-router";
import { timeline, timelineRoutes } from "../../mocks/timeline";

export function OperationalTimeline() {
  const navigate = useNavigate();
  return (
    <div data-tour="dashboard-timeline" className="space-y-1">
      {timeline.map((t, i) => (
        <button key={t} onClick={() => navigate(timelineRoutes[i] ?? "/dashboard")} className="flex w-full gap-3 rounded-xl p-3 text-left transition hover:bg-muted">
          <div className="mt-1 flex flex-col items-center">
            <span className="h-2.5 w-2.5 rounded-full bg-primary" />
            {i < timeline.length - 1 && <span className="mt-1 h-9 w-px bg-border" />}
          </div>
          <div>
            <p className="text-sm font-medium">{t}</p>
            <p className="text-xs text-muted-foreground">há {i + 2} min · BYOP → Produto</p>
          </div>
        </button>
      ))}
    </div>
  );
}

import { ArrowLeft, Eye } from "lucide-react";
import type { UserRole } from "../../../shared/types";
import { roleDescriptions, roleLabels } from "../roles";

export function SimulationBanner({ viewAs, actual, onRestore }: { viewAs: UserRole; actual: UserRole; onRestore: () => void }) {
  if (viewAs === actual) return null;
  return (
    <div className="flex shrink-0 items-center justify-between gap-3 border-b border-[var(--byop-violet-soft)] bg-[var(--byop-violet-soft)] px-4 py-2 text-xs text-[var(--byop-violet-dark)]">
      <span className="flex items-center gap-2"><Eye size={13} />Simulando como: <b>{roleLabels[viewAs]}</b> — {roleDescriptions[viewAs]}</span>
      <button onClick={onRestore} className="flex items-center gap-1 rounded-lg border border-[var(--byop-violet-dark)]/20 px-2 py-1 transition hover:bg-[var(--byop-violet-dark)]/10"><ArrowLeft size={11} />Restaurar: {roleLabels[actual]}</button>
    </div>
  );
}

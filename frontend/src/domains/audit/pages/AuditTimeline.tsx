import { useMemo, useState } from "react";
import { AlertTriangle, BookOpen, Calendar, Download, LayoutGrid, ShieldAlert, User, X, Zap } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { AuditEventCard } from "../components/AuditEventCard";
import { auditService } from "../services/auditService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";
import { useAuth } from "../../../core/auth/useAuth";
import type { AuditEvent } from "../contracts/responses";

// ─── Tipos ────────────────────────────────────────────────────────────────────

type FilterKey = "actor" | "module" | "action" | "tenant" | "risk";

const FILTER_CONFIG: Array<{ key: FilterKey; label: string; icon: React.ReactNode; field: keyof AuditEvent }> = [
  { key: "actor",  label: "Usuário",   icon: <User size={13} />,        field: "actor" },
  { key: "tenant", label: "Produto",   icon: <BookOpen size={13} />,    field: "tenant" },
  { key: "module", label: "Módulo",    icon: <LayoutGrid size={13} />,  field: "module" },
  { key: "action", label: "Evento",    icon: <Zap size={13} />,         field: "action" },
  { key: "risk",   label: "Severidade",icon: <ShieldAlert size={13} />, field: "risk" },
];

const RISK_COLOR: Record<string, string> = {
  alta:  "text-red-500",
  média: "text-amber-500",
  baixa: "text-green-500",
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

function exportTimelineCsv(events: AuditEvent[]) {
  const header = ["Ator", "Ação", "Recurso", "Tenant", "Módulo", "Hora", "Risco"];
  const csv = [header, ...events.map((e) => [e.actor, e.action, e.target, e.tenant, e.module, e.time, e.risk])]
    .map((r) => r.map((c) => `"${String(c)}"`).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "audit-timeline.csv";
  a.click();
  URL.revokeObjectURL(url);
}

// ─── Painel lateral de filtros ────────────────────────────────────────────────

function FilterPanel({
  events,
  filters,
  onChange,
  onClearAll,
}: {
  events: AuditEvent[];
  filters: Partial<Record<FilterKey, string | null>>;
  onChange: (key: FilterKey, value: string | null) => void;
  onClearAll: () => void;
}) {
  const hasActive = Object.values(filters).some(Boolean);

  return (
    <Card className="sticky top-4 h-fit">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold">Filtros</h2>
        {hasActive && (
          <button
            onClick={onClearAll}
            className="flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <X size={10} /> Limpar
          </button>
        )}
      </div>

      {FILTER_CONFIG.map(({ key, label, icon, field }) => {
        const opts = Array.from(new Set(events.map((e) => String(e[field])))).filter(Boolean).sort();
        const active = filters[key] ?? null;
        if (opts.length === 0) return null;
        return (
          <div key={key} className="mb-4">
            <p className="mb-1.5 flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
              {icon} {label}
            </p>
            <div className="flex flex-wrap gap-1">
              <Chip active={!active} onClick={() => onChange(key, null)}>Todos</Chip>
              {opts.map((o) => (
                <Chip
                  key={o}
                  active={active === o}
                  onClick={() => onChange(key, active === o ? null : o)}
                >
                  {key === "risk" && (
                    <AlertTriangle size={10} className={RISK_COLOR[o] ?? "text-muted-foreground"} />
                  )}
                  {o}
                </Chip>
              ))}
            </div>
          </div>
        );
      })}

      {/* Período — visual até API retornar timestamps completos */}
      <div className="mb-1">
        <p className="mb-1.5 flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
          <Calendar size={13} /> Período
        </p>
        <div className="flex flex-wrap gap-1">
          {["Hoje", "7 dias", "30 dias"].map((p) => (
            <Chip key={p} active={false} onClick={() => toast.info("Disponível após integração com timestamps completos")} >
              {p}
            </Chip>
          ))}
          <Chip active={true} onClick={() => {}}>Tudo</Chip>
        </div>
      </div>
    </Card>
  );
}

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-medium transition-colors ${
        active
          ? "bg-primary text-primary-foreground shadow-sm"
          : "bg-muted text-muted-foreground hover:bg-muted/70"
      }`}
    >
      {children}
    </button>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

export function AuditTimeline({ compact = false }: { compact?: boolean }) {
  const { effectiveTenant } = useAuth();
  const { data: auditEvents, loading, error } = useAsyncData(
    () => auditService.listEvents(effectiveTenant?.id),
    [effectiveTenant?.id],
  );

  const [filters, setFilters] = useState<Partial<Record<FilterKey, string | null>>>({});

  const handleFilter = (key: FilterKey, value: string | null) =>
    setFilters((prev) => ({ ...prev, [key]: value }));

  const handleClearAll = () => setFilters({});

  const allEvents = useMemo(() => auditEvents ?? [], [auditEvents]);

  const filtered = useMemo(
    () =>
      allEvents.filter((e) =>
        FILTER_CONFIG.every(({ key, field }) => {
          const active = filters[key];
          return !active || String(e[field]) === active;
        }),
      ),
    [allEvents, filters],
  );

  if (loading) return <SkeletonLines />;
  if (error || !auditEvents) return <PartialErrorWidget />;

  const body = (
    <div>
      {filtered.map((e) => (
        <AuditEventCard key={`${e.actor}-${e.action}-${e.time}`} e={e} />
      ))}
    </div>
  );

  if (compact) return body;

  const activeCount = Object.values(filters).filter(Boolean).length;

  return (
    <>
      <PageHeader
        title="Audit Timeline"
        module="Auditoria"
        desc="Timeline operacional de eventos relevantes e rastreáveis."
        badge="Audit"
      >
        {activeCount > 0 && (
          <span className="rounded-full bg-primary/15 px-2.5 py-0.5 text-xs font-medium text-primary">
            {activeCount} filtro{activeCount > 1 ? "s" : ""} ativo{activeCount > 1 ? "s" : ""}
          </span>
        )}
        <Button
          primary
          onClick={() => {
            exportTimelineCsv(filtered);
            toast.success("Timeline exportada!", { description: `${filtered.length} evento(s) exportados.` });
          }}
        >
          <Download size={14} /> Exportar
        </Button>
      </PageHeader>

      <div className="grid gap-4 xl:grid-cols-[240px_1fr]">
        <FilterPanel
          events={allEvents}
          filters={filters}
          onChange={handleFilter}
          onClearAll={handleClearAll}
        />
        <div>
          {filtered.length === 0 ? (
            <EmptyState
              compact
              title="Nenhum evento encontrado"
              description="Tente remover alguns filtros para ver mais resultados."
              primaryAction={{ label: "Limpar filtros", onClick: handleClearAll }}
            />
          ) : (
            body
          )}
        </div>
      </div>
    </>
  );
}

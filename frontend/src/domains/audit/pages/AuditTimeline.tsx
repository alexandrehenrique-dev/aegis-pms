import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { ChevronLeft, ChevronRight, Download, Search } from "lucide-react";
import { Button, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { RiskBadge } from "../../../shared/components/RiskBadge";
import { useAuth } from "../../../core/auth/useAuth";
import { toast } from "../../../core/notifications/toast";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AuditEventCard } from "../components/AuditEventCard";
import type { AuditEvent, AuditEventFilters } from "../contracts/responses";
import { auditService } from "../services/auditService";

const PAGE_SIZE = 25;
const RISK_OPTIONS = ["baixo", "medio", "alto"];
const MODULE_OPTIONS = ["CONTENT", "PAGES", "ASSETS", "FORM", "ANALYTICS", "KNOWLEDGE_GRAPH", "USERS", "SETTINGS", "PRODUCT"];

function exportTimelineCsv(events: AuditEvent[]) {
  const header = ["Ator", "Ação", "Recurso", "Tenant", "Módulo", "Hora", "Risco"];
  const csv = [header, ...events.map((e) => [e.actor, e.action, e.target, e.tenant, e.module, e.time, e.risk])]
    .map((row) => row.map((cell) => `"${String(cell).replaceAll("\"", "\"\"")}"`).join(","))
    .join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "audit-events.csv";
  link.click();
  URL.revokeObjectURL(url);
}

function SelectFilter({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
}) {
  return (
    <label className="min-w-0 text-xs font-medium text-muted-foreground">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 h-10 w-full rounded-lg border border-border bg-card px-3 text-sm text-foreground outline-primary"
      >
        <option value="">Todos</option>
        {options.map((option) => (
          <option key={option} value={option}>{option}</option>
        ))}
      </select>
    </label>
  );
}

function AuditTable({ events }: { events: AuditEvent[] }) {
  const navigate = useNavigate();
  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="overflow-x-auto">
        <table className="min-w-[920px] w-full text-left text-sm">
          <thead className="bg-muted/70 text-xs uppercase tracking-wide text-muted-foreground">
            <tr>
              <th className="px-4 py-3 font-semibold">Quando</th>
              <th className="px-4 py-3 font-semibold">Ator</th>
              <th className="px-4 py-3 font-semibold">Evento</th>
              <th className="px-4 py-3 font-semibold">Alvo</th>
              <th className="px-4 py-3 font-semibold">Módulo</th>
              <th className="px-4 py-3 font-semibold">Risco</th>
              <th className="px-4 py-3 text-right font-semibold">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {events.map((event) => (
              <tr key={event.id ?? `${event.actor}-${event.action}-${event.time}`} className="transition hover:bg-muted/35">
                <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{event.time}</td>
                <td className="max-w-[220px] px-4 py-3">
                  <p className="truncate font-medium">{event.actor}</p>
                  <p className="truncate text-xs text-muted-foreground">{event.tenant}</p>
                </td>
                <td className="max-w-[260px] px-4 py-3">
                  <p className="truncate font-medium">{event.action}</p>
                </td>
                <td className="max-w-[280px] px-4 py-3 text-muted-foreground">
                  <p className="truncate">{event.target}</p>
                </td>
                <td className="px-4 py-3">
                  <span className="rounded-full bg-muted px-2 py-1 text-xs text-muted-foreground">{event.module}</span>
                </td>
                <td className="px-4 py-3"><RiskBadge risk={event.risk} /></td>
                <td className="px-4 py-3 text-right">
                  <Button onClick={() => navigate(`/audit/${event.id}`)} disabled={!event.id}>Abrir</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}) {
  const lastPage = Math.max(totalPages - 1, 0);
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-border bg-card p-3 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
      <span>
        {totalElements === 0
          ? "Nenhum evento"
          : `${totalElements} evento${totalElements === 1 ? "" : "s"} · página ${page + 1} de ${Math.max(totalPages, 1)}`}
      </span>
      <div className="flex gap-2">
        <Button onClick={() => onPageChange(page - 1)} disabled={page <= 0}>
          <ChevronLeft size={14} /> Anterior
        </Button>
        <Button onClick={() => onPageChange(page + 1)} disabled={page >= lastPage}>
          Próxima <ChevronRight size={14} />
        </Button>
      </div>
    </div>
  );
}

export function AuditTimeline({ compact = false }: { compact?: boolean }) {
  const { effectiveTenant } = useAuth();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<AuditEventFilters>({});

  const { data, loading, error } = useAsyncData(
    () => auditService.listEventsPage(effectiveTenant?.id, filters, page, PAGE_SIZE),
    [effectiveTenant?.id, filters.query, filters.module, filters.risk, filters.productId, page],
  );

  const events = useMemo(() => data?.items ?? [], [data?.items]);
  const compactEvents = useMemo(() => events.slice(0, 3), [events]);
  const activeFilterCount = [filters.query, filters.module, filters.risk].filter(Boolean).length;

  const updateFilter = (patch: Partial<AuditEventFilters>) => {
    setPage(0);
    setFilters((current) => ({ ...current, ...patch }));
  };

  const clearFilters = () => {
    setPage(0);
    setFilters({});
  };

  if (loading) return <SkeletonLines />;
  if (error || !data) return <PartialErrorWidget />;

  if (compact) {
    return (
      <div>
        {compactEvents.map((event) => <AuditEventCard key={event.id ?? `${event.actor}-${event.time}`} e={event} />)}
        {compactEvents.length === 0 && <EmptyState compact title="Sem atividade recente" description="Ainda não há eventos para este contexto." />}
      </div>
    );
  }

  const visibleEvents = events;

  return (
    <>
      <PageHeader
        title="Auditoria"
        module="Auditoria"
        desc="Eventos rastreáveis do tenant em uma tabela paginada, com filtros operacionais."
        badge="Audit"
      >
        {activeFilterCount > 0 && (
          <span className="rounded-full bg-primary/15 px-2.5 py-1 text-xs font-medium text-primary">
            {activeFilterCount} filtro{activeFilterCount === 1 ? "" : "s"}
          </span>
        )}
        <Button
          primary
          onClick={() => {
            exportTimelineCsv(visibleEvents);
            toast.success("Auditoria exportada.", { description: `${visibleEvents.length} evento(s) da página exportados.` });
          }}
        >
          <Download size={14} /> Exportar página
        </Button>
      </PageHeader>

      <div className="mb-4 rounded-xl border border-border bg-card p-3">
        <div className="grid gap-3 lg:grid-cols-[minmax(220px,1fr)_180px_160px_auto] lg:items-end">
          <label className="text-xs font-medium text-muted-foreground">
            Buscar
            <div className="mt-1 flex h-10 items-center gap-2 rounded-lg border border-border bg-background px-3">
              <Search size={15} />
              <input
                value={filters.query ?? ""}
                onChange={(event) => updateFilter({ query: event.target.value })}
                placeholder="Ator, evento, alvo..."
                className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none"
              />
            </div>
          </label>
          <SelectFilter label="Módulo" value={filters.module ?? ""} options={MODULE_OPTIONS} onChange={(module) => updateFilter({ module: module || undefined })} />
          <SelectFilter label="Risco" value={filters.risk ?? ""} options={RISK_OPTIONS} onChange={(risk) => updateFilter({ risk: risk || undefined })} />
          <Button onClick={clearFilters} disabled={activeFilterCount === 0}>Limpar filtros</Button>
        </div>
      </div>

      {visibleEvents.length === 0 ? (
        <EmptyState
          title="Nenhum evento encontrado"
          description="Ajuste filtros ou avance para outra página da trilha de auditoria."
          primaryAction={{ label: "Limpar filtros", onClick: clearFilters }}
        />
      ) : (
        <div className="space-y-3">
          <AuditTable events={visibleEvents} />
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={(nextPage) => setPage(Math.max(nextPage, 0))}
          />
        </div>
      )}
    </>
  );
}

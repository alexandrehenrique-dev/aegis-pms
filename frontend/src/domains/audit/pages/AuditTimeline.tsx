import { useMemo, useState } from "react";
import { Filter } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { AuditEventCard } from "../components/AuditEventCard";
import { auditService } from "../services/auditService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { toast } from "../../../core/notifications/toast";
import type { AuditEvent } from "../contracts/responses";

function FilterGroup({ label, options, value, onChange }: { label: string; options: string[]; value: string | null; onChange: (v: string | null) => void }) {
  return (
    <div className="mb-3">
      <p className="mb-1 text-sm font-medium">{label}</p>
      <div className="flex flex-wrap gap-1">
        <Button onClick={() => onChange(null)} primary={!value}>Todos</Button>
        {options.map((o) => <Button key={o} onClick={() => onChange(o)} primary={value === o}>{o}</Button>)}
      </div>
    </div>
  );
}

function exportTimelineCsv(events: AuditEvent[]) {
  const header = ["Ator", "Ação", "Recurso", "Tenant", "Módulo", "Hora", "Risco"];
  const csv = [header, ...events.map((e) => [e.actor, e.action, e.target, e.tenant, e.module, e.time, e.risk])]
    .map((r) => r.map((c) => `"${c}"`).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "audit-timeline.csv";
  a.click();
  URL.revokeObjectURL(url);
}

export function AuditTimeline({ compact = false }: { compact?: boolean }) {
  const { data: auditEvents, loading, error } = useAsyncData(() => auditService.listEvents(), []);
  const [actor, setActor] = useState<string | null>(null);
  const [module, setModule] = useState<string | null>(null);
  const [risk, setRisk] = useState<string | null>(null);

  const options = useMemo(() => ({
    actors: Array.from(new Set((auditEvents ?? []).map((e) => e.actor))),
    modules: Array.from(new Set((auditEvents ?? []).map((e) => e.module))),
    risks: Array.from(new Set((auditEvents ?? []).map((e) => e.risk))),
  }), [auditEvents]);

  if (loading) return <SkeletonLines />;
  if (error || !auditEvents) return <PartialErrorWidget />;

  const filtered = auditEvents.filter((e) => (!actor || e.actor === actor) && (!module || e.module === module) && (!risk || e.risk === risk));
  const body = <div>{filtered.map((e) => <AuditEventCard key={`${e.actor}-${e.action}-${e.time}`} e={e} />)}</div>;
  if (compact) return body;
  return (
    <>
      <PageHeader title="Audit Timeline" module="Auditoria" desc="Timeline operacional de eventos relevantes e rastreáveis." badge="Audit">
        <Popover>
          <PopoverTrigger asChild><Button><Filter size={15} />Usuário / produto / módulo / severidade</Button></PopoverTrigger>
          <PopoverContent className="w-80">
            <FilterGroup label="Usuário" options={options.actors} value={actor} onChange={setActor} />
            <FilterGroup label="Módulo" options={options.modules} value={module} onChange={setModule} />
            <FilterGroup label="Severidade" options={options.risks} value={risk} onChange={setRisk} />
          </PopoverContent>
        </Popover>
        <Button primary onClick={() => { exportTimelineCsv(filtered); toast.success("Timeline exportada!"); }}>Exportar timeline</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Filtros</h2>
          {["usuário", "produto", "módulo", "evento", "período", "severidade"].map((f) => <div key={f} className="mb-2 rounded-lg border border-border p-3 text-sm">{f}</div>)}
        </Card>
        <div>
          {filtered.length === 0 ? <EmptyState compact title="Sem eventos" description="Estado previsto para filtros sem resultado." /> : body}
        </div>
      </div>
    </>
  );
}

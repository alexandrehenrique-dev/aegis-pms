import { Filter } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { AuditEventCard } from "../components/AuditEventCard";
import { auditService } from "../services/auditService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

export function AuditTimeline({ compact = false }: { compact?: boolean }) {
  const { data: auditEvents, loading, error } = useAsyncData(() => auditService.listEvents(), []);

  if (loading) return <SkeletonLines />;
  if (error || !auditEvents) return <PartialErrorWidget />;

  const body = <div>{auditEvents.map((e) => <AuditEventCard key={`${e.actor}-${e.action}-${e.time}`} e={e} />)}</div>;
  if (compact) return body;
  return (
    <>
      <PageHeader title="Audit Timeline" module="Auditoria" desc="Timeline operacional de eventos relevantes e rastreáveis." badge="Audit">
        <Button><Filter size={15} />Usuário / produto / módulo / severidade</Button>
        <Button primary>Exportar timeline</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[280px_1fr]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Filtros</h2>
          {["usuário", "produto", "módulo", "evento", "período", "severidade"].map((f) => <div key={f} className="mb-2 rounded-lg border border-border p-3 text-sm">{f}</div>)}
        </Card>
        <div>
          {body}
          <EmptyState compact title="Sem eventos" description="Estado previsto para filtros sem resultado." />
        </div>
      </div>
    </>
  );
}

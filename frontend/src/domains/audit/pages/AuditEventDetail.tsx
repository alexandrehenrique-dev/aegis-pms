import { useNavigate, useParams } from "react-router";
import { Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { useAuth } from "../../../core/auth/useAuth";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { auditService } from "../services/auditService";

const MODULE_ROUTE: Record<string, string> = {
  Permissions: "/settings/roles",
  Users: "/users",
  Forms: "/forms",
  Assets: "/assets",
  Conteúdo: "/content",
  Content: "/content",
  Modules: "/products",
};

function resourceRoute(module: string): string {
  return MODULE_ROUTE[module] ?? "/audit";
}

export function AuditEventDetail() {
  const navigate = useNavigate();
  const { id: eventId } = useParams<{ id: string }>();
  const { effectiveTenant } = useAuth();
  const tenantId = effectiveTenant?.id ?? "";

  const { data: event, loading, error } = useAsyncData(
    () => (tenantId && eventId ? auditService.getEvent(tenantId, eventId) : Promise.resolve(undefined)),
    [tenantId, eventId],
  );

  if (loading) return <SkeletonLines />;
  if (error || !event) return <PartialErrorWidget />;

  const before = JSON.stringify(event.diffJson?.before ?? {}, null, 2);
  const after = JSON.stringify(event.diffJson?.after ?? {}, null, 2);

  const handleCopyId = async () => {
    await navigator.clipboard.writeText(event.id ?? "");
    toast.success("Copiado");
  };

  const exportEventJson = () => {
    const blob = new Blob([JSON.stringify(event, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${event.id}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <>
      <PageHeader title={`Evento ${event.id ?? ""}`} module="Auditoria" desc="Detalhe do evento com antes/depois, payload e identificadores técnicos." badge={event.risk === "alta" ? "Evento crítico" : undefined}>
        <Button onClick={handleCopyId}>Copiar ID</Button>
        <Button onClick={exportEventJson}>Exportar evento</Button>
        <Button primary onClick={() => navigate(resourceRoute(event.module))}>Abrir recurso</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Antes / Depois</h2>
          <div className="grid gap-3 md:grid-cols-2">
            <pre className="overflow-auto rounded-xl bg-[#FDEBE8] p-4 text-xs">{before}</pre>
            <pre className="overflow-auto rounded-xl bg-[#ede9fe] p-4 text-xs">{after}</pre>
          </div>
          <h2 className="mb-3 mt-4 text-lg font-semibold">Payload resumido</h2>
          <pre className="overflow-auto rounded-xl bg-muted p-4 text-xs">{`trace_id: ${event.traceId ?? "—"}\nip: ${event.ip ?? "—"}\nuser_agent: ${event.userAgent ?? "—"}`}</pre>
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Metadados</h2>
          {[["Ator", event.actor], ["Tenant", event.tenant], ["Módulo", event.module], ["Recurso", event.target], ["Ação", event.action], ["Severidade", event.risk], ["Data/hora", event.time]].map((x) => (
            <div key={x[0]} className="mb-2 flex justify-between rounded-lg bg-muted p-2 text-sm"><span>{x[0]}</span><b>{x[1]}</b></div>
          ))}
        </Card>
      </div>
    </>
  );
}

import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, SkeletonLines } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";

function PartialAnalyticsError() {
  return <div className="rounded-xl border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">Alguns widgets carregaram, mas canais pagos estão temporariamente indisponíveis.</div>;
}

function AnalyticsEmptyState() {
  return <EmptyState title="Este produto ainda não possui dados suficientes." description="Assim que o produto receber visitas, formulários ou publicações, os indicadores aparecerão aqui." />;
}

export function AnalyticsStates() {
  const [retrying, setRetrying] = useState(false);
  const [retryingError, setRetryingError] = useState(false);

  const handleRetry = async (setter: (v: boolean) => void) => {
    setter(true);
    await new Promise((r) => setTimeout(r, 800));
    setter(false);
    toast.success("Dados atualizados!");
  };

  return (
    <>
      <PageHeader title="Estados Analytics" module="Analytics" desc="Empty, loading, erro e erro parcial do módulo." badge="Estados">
        <Button onClick={() => handleRetry(setRetrying)} disabled={retrying}>{retrying && <Loader2 size={15} className="animate-spin" />}{retrying ? "Tentando..." : "Tentar novamente"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-2">
        <AnalyticsEmptyState />
        <Card><h2 className="mb-3 text-lg font-semibold">Loading</h2><SkeletonLines /><div className="mt-3"><SkeletonLines /></div></Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Error</h2>
          <div className="rounded-xl border border-destructive/20 bg-[#FDEBE8] p-4 text-sm text-destructive">
            Não foi possível carregar os dados de analytics.
            <div className="mt-3"><Button onClick={() => handleRetry(setRetryingError)} disabled={retryingError}>{retryingError && <Loader2 size={15} className="animate-spin" />}{retryingError ? "Tentando..." : "Tentar novamente"}</Button></div>
          </div>
        </Card>
        <Card><h2 className="mb-3 text-lg font-semibold">Partial Error</h2><PartialAnalyticsError /></Card>
      </div>
    </>
  );
}

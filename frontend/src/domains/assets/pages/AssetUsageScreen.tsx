import { useNavigate } from "react-router";
import { Button, EmptyState, PageHeader } from "../../../shared/components/Primitives";
import { AssetUsagePanel } from "../components/AssetUsagePanel";
import { toast } from "../../../core/notifications/toast";

export function AssetUsageScreen() {
  const navigate = useNavigate();

  const handleCopyReport = async () => {
    await navigator.clipboard.writeText("Relatório de uso · hero-maestro-beton.jpg · Home → Hero");
    toast.success("Copiado");
  };

  return (
    <>
      <PageHeader title="Uso do Asset" module="Assets" desc="Entenda onde o asset é usado antes de arquivar ou substituir." badge="Impacto">
        <Button onClick={handleCopyReport}>Copiar relatório</Button>
        <Button primary onClick={() => navigate("/assets/hero-maestro-beton")}>Abrir asset</Button>
      </PageHeader>
      <AssetUsagePanel />
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <EmptyState compact title="Asset não utilizado" description="Quando não há vínculos, arquivar tem baixo impacto." />
        <div className="rounded-xl border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">Uso quebrado: referência em página arquivada precisa revisão.</div>
      </div>
    </>
  );
}

import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { ComparisonBadge } from "../components/AnalyticsBits";
import { toast } from "../../../core/notifications/toast";
import { analyticsService } from "../services/analyticsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { AnalyticsReport } from "../contracts/responses";

function downloadReport(name: string) {
  const blob = new Blob([`Relatório: ${name}\nGerado em: ${new Date().toISOString()}`], { type: "text/plain" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${name.toLowerCase().replace(/\s+/g, "-")}.txt`;
  a.click();
  URL.revokeObjectURL(url);
}

function ReportCard({ r, productId }: { r: AnalyticsReport; productId: string }) {
  const [generating, setGenerating] = useState(false);

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      await analyticsService.generateReport(productId, r.name);
      toast.success("Relatório gerado!", { description: r.name });
    } finally {
      setGenerating(false);
    }
  };

  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{r.name}</h3><ComparisonBadge value={r.status} /></div>
      <p className="mt-2 text-sm text-muted-foreground">{r.description}</p>
      <div className="mt-4 grid grid-cols-2 gap-2 text-xs"><span>Período: {r.period}</span><span>Formato: {r.format}</span><span>Última geração: {r.lastGenerated}</span></div>
      <div className="mt-4 flex gap-2">
        <Button onClick={handleGenerate} disabled={generating}>{generating && <Loader2 size={15} className="animate-spin" />}{generating ? "Gerando..." : "Gerar"}</Button>
        <Button primary onClick={() => { downloadReport(r.name); toast.success("Download iniciado!"); }}>Baixar</Button>
      </div>
    </Card>
  );
}

export function ReportGrid() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [generatingAll, setGeneratingAll] = useState(false);
  const { data: reports, loading, error } = useAsyncData(() => analyticsService.listReports(productId), [productId]);

  const handleGenerateAll = async () => {
    setGeneratingAll(true);
    try {
      await Promise.all((reports ?? []).map((r) => analyticsService.generateReport(productId, r.name)));
      toast.success("Todos os relatórios foram gerados!");
    } finally {
      setGeneratingAll(false);
    }
  };

  return (
    <>
      <PageHeader title="Reports" module="Analytics" desc="Central de relatórios implementáveis e rastreáveis." badge="Relatórios">
        <Button primary onClick={handleGenerateAll} disabled={generatingAll}>{generatingAll && <Loader2 size={15} className="animate-spin" />}{generatingAll ? "Gerando..." : "Gerar relatório"}</Button>
      </PageHeader>
      {loading ? <SkeletonLines /> : error ? <PartialErrorWidget /> : !reports || reports.length === 0 ? (
        <EmptyState title="Sem relatórios disponíveis" description="Quando houver dados do produto, os relatórios aparecem aqui." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{reports.map((r) => <ReportCard key={r.name} r={r} productId={productId} />)}</div>
      )}
    </>
  );
}

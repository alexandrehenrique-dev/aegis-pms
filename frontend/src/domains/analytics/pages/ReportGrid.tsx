import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ComparisonBadge } from "../components/AnalyticsBits";
import { toast } from "../../../core/notifications/toast";
import { analyticsService } from "../services/analyticsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

function downloadReport(name: string) {
  const blob = new Blob([`Relatório: ${name}\nGerado em: ${new Date().toISOString()}`], { type: "text/plain" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${name.toLowerCase().replace(/\s+/g, "-")}.txt`;
  a.click();
  URL.revokeObjectURL(url);
}

function ReportCard({ r, productId }: { r: string[]; productId: string }) {
  const [generating, setGenerating] = useState(false);

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      await analyticsService.generateReport(productId, r[0]);
      toast.success("Relatório gerado!", { description: r[0] });
    } finally {
      setGenerating(false);
    }
  };

  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{r[0]}</h3><ComparisonBadge value={r[4]} /></div>
      <p className="mt-2 text-sm text-muted-foreground">{r[1]}</p>
      <div className="mt-4 grid grid-cols-2 gap-2 text-xs"><span>Período: {r[2]}</span><span>Formato: {r[3]}</span><span>Última geração: {r[5]}</span></div>
      <div className="mt-4 flex gap-2">
        <Button onClick={handleGenerate} disabled={generating}>{generating && <Loader2 size={15} className="animate-spin" />}{generating ? "Gerando..." : "Gerar"}</Button>
        <Button primary onClick={() => { downloadReport(r[0]); toast.success("Download iniciado!"); }}>Baixar</Button>
      </div>
    </Card>
  );
}

export function ReportGrid() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [generatingAll, setGeneratingAll] = useState(false);
  const reports = [
    ["Relatório mensal do produto", "Resumo executivo-operacional do produto.", "Junho", "PDF", "pronto", "hoje"],
    ["Performance de conteúdo", "Páginas, SEO e publicações.", "30 dias", "CSV", "gerando", "agora"],
    ["Conversões de formulários", "Leads, origem e resposta.", "7 dias", "XLSX", "pronto", "ontem"],
    ["Saúde do produto", "Pendências por módulo.", "atual", "PDF", "sem dados suficientes", "—"],
    ["Auditoria editorial", "Fluxo de revisão e publicação.", "mês", "PDF", "erro", "2 dias"],
    ["SEO e acessibilidade", "Alt text, metas e oportunidades.", "30 dias", "PDF", "pronto", "hoje"],
  ];

  const handleGenerateAll = async () => {
    setGeneratingAll(true);
    try {
      await Promise.all(reports.map((r) => analyticsService.generateReport(productId, r[0])));
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
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{reports.map((r) => <ReportCard key={r[0]} r={r} productId={productId} />)}</div>
    </>
  );
}

import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { ComparisonBadge } from "../components/AnalyticsBits";
import { toast } from "../../../core/notifications/toast";
import { analyticsService } from "../services/analyticsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { AnalyticsReport } from "../contracts/responses";

function fileSlug(value: string): string {
  return value.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
}

function escapeCsv(value: string): string {
  return `"${value.replace(/"/g, "\"\"")}"`;
}

function reportRows(report: AnalyticsReport, productName: string, generatedAt: string): string[][] {
  return [
    ["Produto", productName],
    ["Relatorio", report.name],
    ["Descricao", report.description],
    ["Periodo", report.period],
    ["Status", report.status],
    ["Formato", report.format],
    ["Gerado em", generatedAt],
    ["Rascunhos", "0"],
    ["Em revisao", "0"],
    ["Publicados", report.name.includes("conteudo") ? "2" : "N/A"],
    ["Formularios recebidos", report.name.includes("form") ? "0" : "N/A"],
  ];
}

function buildCsv(report: AnalyticsReport, productName: string, generatedAt: string): Blob {
  const csv = reportRows(report, productName, generatedAt)
    .map((row) => row.map(escapeCsv).join(","))
    .join("\n");
  return new Blob([csv], { type: "text/csv;charset=utf-8" });
}

function buildSpreadsheet(report: AnalyticsReport, productName: string, generatedAt: string): Blob {
  const rows = reportRows(report, productName, generatedAt)
    .map((row) => `<tr>${row.map((cell) => `<td>${cell}</td>`).join("")}</tr>`)
    .join("");
  const html = `<html><head><meta charset="utf-8" /></head><body><table>${rows}</table></body></html>`;
  return new Blob([html], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8" });
}

function pdfEscape(value: string): string {
  return value.replace(/[()\\]/g, "\\$&").replace(/[^\x20-\x7E]/g, "?");
}

function buildPdf(report: AnalyticsReport, productName: string, generatedAt: string): Blob {
  const lines = reportRows(report, productName, generatedAt)
    .map(([label, value]) => `${label}: ${value}`);
  const textCommands = lines.map((line, index) => `BT /F1 11 Tf 56 ${760 - index * 22} Td (${pdfEscape(line)}) Tj ET`).join("\n");
  const objects = [
    "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj",
    "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj",
    "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj",
    "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj",
    `5 0 obj << /Length ${textCommands.length} >> stream\n${textCommands}\nendstream endobj`,
  ];
  let pdf = "%PDF-1.4\n";
  const offsets = [0];
  for (const object of objects) {
    offsets.push(pdf.length);
    pdf += `${object}\n`;
  }
  const xrefStart = pdf.length;
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  for (const offset of offsets.slice(1)) {
    pdf += `${String(offset).padStart(10, "0")} 00000 n \n`;
  }
  pdf += `trailer << /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefStart}\n%%EOF`;
  return new Blob([pdf], { type: "application/pdf" });
}

function reportBlob(report: AnalyticsReport, productName: string, generatedAt: string): { blob: Blob; extension: string } {
  const format = report.format.toUpperCase();
  if (format === "CSV") return { blob: buildCsv(report, productName, generatedAt), extension: "csv" };
  if (format === "XLSX") return { blob: buildSpreadsheet(report, productName, generatedAt), extension: "xlsx" };
  return { blob: buildPdf(report, productName, generatedAt), extension: "pdf" };
}

function downloadReport(report: AnalyticsReport, productName: string) {
  const generatedAt = new Date().toLocaleString("pt-BR");
  const { blob, extension } = reportBlob(report, productName, generatedAt);
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${fileSlug(productName)}-${fileSlug(report.name)}.${extension}`;
  a.click();
  URL.revokeObjectURL(url);
}

function ReportCard({ r, productId, productName }: { r: AnalyticsReport; productId: string; productName: string }) {
  const [generating, setGenerating] = useState(false);
  const [generatedAt, setGeneratedAt] = useState(r.lastGenerated);

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      await analyticsService.generateReport(productId, r.name);
      setGeneratedAt(new Date().toLocaleString("pt-BR"));
      toast.success("Relatório gerado!", { description: r.name });
    } finally {
      setGenerating(false);
    }
  };

  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{r.name}</h3><ComparisonBadge value={r.status} /></div>
      <p className="mt-2 text-sm text-muted-foreground">{r.description}</p>
      <div className="mt-4 grid grid-cols-2 gap-2 text-xs"><span>Período: {r.period}</span><span>Formato: {r.format}</span><span>Última geração: {generatedAt}</span></div>
      <div className="mt-4 flex gap-2">
        <Button onClick={handleGenerate} disabled={generating}>{generating && <Loader2 size={15} className="animate-spin" />}{generating ? "Gerando..." : "Gerar"}</Button>
        <Button primary onClick={() => { downloadReport(r, productName); toast.success("Download iniciado!", { description: `${r.format} · ${r.name}` }); }}>Baixar</Button>
      </div>
    </Card>
  );
}

export function ReportGrid() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const productName = product?.name ?? "produto";
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
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{reports.map((r) => <ReportCard key={r.name} r={r} productId={productId} productName={productName} />)}</div>
      )}
    </>
  );
}

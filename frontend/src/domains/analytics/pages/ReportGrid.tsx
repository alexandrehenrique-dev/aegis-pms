import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ComparisonBadge } from "../components/AnalyticsBits";

function ReportCard({ r }: { r: string[] }) {
  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{r[0]}</h3><ComparisonBadge value={r[4]} /></div>
      <p className="mt-2 text-sm text-muted-foreground">{r[1]}</p>
      <div className="mt-4 grid grid-cols-2 gap-2 text-xs"><span>Período: {r[2]}</span><span>Formato: {r[3]}</span><span>Última geração: {r[5]}</span></div>
      <div className="mt-4 flex gap-2"><Button>Gerar</Button><Button primary>Baixar</Button></div>
    </Card>
  );
}

export function ReportGrid() {
  const reports = [
    ["Relatório mensal do produto", "Resumo executivo-operacional do produto.", "Junho", "PDF", "pronto", "hoje"],
    ["Performance de conteúdo", "Páginas, SEO e publicações.", "30 dias", "CSV", "gerando", "agora"],
    ["Conversões de formulários", "Leads, origem e resposta.", "7 dias", "XLSX", "pronto", "ontem"],
    ["Saúde do produto", "Pendências por módulo.", "atual", "PDF", "sem dados suficientes", "—"],
    ["Auditoria editorial", "Fluxo de revisão e publicação.", "mês", "PDF", "erro", "2 dias"],
    ["SEO e acessibilidade", "Alt text, metas e oportunidades.", "30 dias", "PDF", "pronto", "hoje"],
  ];
  return (
    <>
      <PageHeader title="Reports" module="Analytics" desc="Central de relatórios implementáveis e rastreáveis." badge="Relatórios">
        <Button primary>Gerar relatório</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{reports.map((r) => <ReportCard key={r[0]} r={r} />)}</div>
    </>
  );
}

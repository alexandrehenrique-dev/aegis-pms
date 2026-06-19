import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { OperationalTimeline } from "../../../shared/components/OperationalTimeline";

export function ProductDetail() {
  return (
    <>
      <PageHeader title="Maestro Beton" module="Visão Geral" desc="Dados administrativos, módulos, equipe, domínios futuros e auditoria recente.">
        <Button>Editar</Button>
        <Button primary>Salvar visão</Button>
      </PageHeader>
      <div className="mb-4 flex gap-2 overflow-auto pb-1">
        {["Visão Geral", "Módulos", "Conteúdo", "Assets", "Forms", "Analytics", "Graph", "Configurações"].map((t, i) => (
          <button key={t} className={`whitespace-nowrap rounded-lg border px-3 py-2 text-sm ${i === 0 ? "border-primary bg-primary text-white" : "border-border bg-card"}`}>{t}</button>
        ))}
      </div>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <h2 className="mb-4 text-lg font-semibold">Dados gerais</h2>
          <div className="grid gap-3 md:grid-cols-2">
            {[["Status", "Ativo"], ["Tipo", "Site Institucional"], ["Slug", "maestro-beton"], ["Idioma", "pt-BR"], ["Descrição", "Produto institucional governado"], ["Módulos", "Conteúdo, Assets, Forms, Analytics"]].map((r) => (
              <div key={r[0]} className="rounded-xl bg-muted p-3"><p className="text-xs text-muted-foreground">{r[0]}</p><p className="font-medium">{r[1]}</p></div>
            ))}
          </div>
        </Card>
        <Card><h2 className="mb-3 text-lg font-semibold">Auditoria recente</h2><OperationalTimeline /></Card>
      </div>
    </>
  );
}

import { useNavigate } from "react-router";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";

function KnowledgeTimeline() {
  return (
    <div className="space-y-1">
      {["hero-maestro.jpg vinculado à Página Home", "Formulário Orçamento conectado à Página Home", "SEO Home associado ao conteúdo publicado", "Lead Camila criado via Submission #93"].map((t, i) => (
        <div key={t} className="flex gap-3 rounded-xl p-3 hover:bg-muted">
          <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
          <div><p className="text-sm font-medium">{t}</p><p className="text-xs text-muted-foreground">há {i + 1} h · Knowledge Graph · Maestro Beton</p></div>
        </div>
      ))}
    </div>
  );
}

export function KnowledgeOverview() {
  const navigate = useNavigate();
  return (
    <>
      <PageHeader title="Knowledge Graph" desc="Mapa operacional de relações, dependências e impactos do produto." badge="Maestro Beton">
        <Button onClick={() => navigate("/knowledge/search")}>Buscar entidade</Button>
        <Button primary onClick={() => navigate("/knowledge/graph")}>Explorar grafo</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KPIWidget label="Entidades totais" value="184" detail="Produto, conteúdo, assets e forms" />
            <KPIWidget label="Relações totais" value="426" detail="+14 esta semana" />
            <KPIWidget label="Entidades órfãs" value="12" detail="limpeza recomendada" error />
            <KPIWidget label="Conteúdos sem vínculo" value="5" detail="revisar navegação" />
            <KPIWidget label="Assets sem uso" value="18" detail="impacto em biblioteca" />
            <KPIWidget label="Formulários desconectados" value="3" detail="sem conteúdo origem" error />
            <KPIWidget label="Relações recentes" value="27" detail="últimas 48h" />
            <KPIWidget label="Mudanças recentes" value="9" detail="auditáveis futuramente" />
          </div>
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Estados do módulo</h2>
            <div className="grid gap-3 md:grid-cols-3">
              <EmptyState compact title="Grafo vazio" description="Assim que entidades forem vinculadas, o mapa aparece." />
              <SkeletonLines />
              <PermissionHint />
            </div>
          </Card>
        </div>
        <Card><h2 className="mb-3 text-lg font-semibold">Timeline de relações</h2><KnowledgeTimeline /><PartialErrorWidget /></Card>
      </div>
    </>
  );
}

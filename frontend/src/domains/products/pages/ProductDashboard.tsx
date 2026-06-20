import { useNavigate, useSearchParams } from "react-router";
import { ChevronRight, ExternalLink } from "lucide-react";
import { useViewAsRole } from "../../../core/permissions/ViewAsRoleContext";
import { Button, Card, KPIWidget, PageHeader } from "../../../shared/components/Primitives";
import { OperationalTimeline } from "../../../shared/components/OperationalTimeline";
import { QuickActions } from "../components/QuickActions";
import { ModuleCatalog } from "../components/ModuleCatalog";
import { ProductEmpty } from "./ProductEmpty";

export function ProductDashboard() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { viewAsRole } = useViewAsRole();
  if (params.get("empty")) return <ProductEmpty />;

  const pendencias: [string, string][] = [
    ["Revisar conteúdo em aprovação", "/content/workflow"],
    ["Configurar SEO da página Home", "/settings/product"],
    ["Adicionar imagens à galeria", "/assets"],
    ["Ver respostas do formulário de orçamento", "/forms/submissions"],
  ];

  return (
    <>
      <PageHeader title="Maestro Beton" desc="Cockpit operacional do produto digital: saúde, pendências, módulos e próximos passos." badge="Site Institucional">
        <Button onClick={() => navigate("/products/maestro-beton/detail")}>Editar produto</Button>
        <Button onClick={() => navigate("/products/maestro-beton/modules")}>Ver módulos</Button>
        <Button primary onClick={() => window.open("https://maestro-beton.byop.app", "_blank", "noopener,noreferrer")}><ExternalLink size={15} />Preview público</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KPIWidget label="Status do produto" value="Saudável" detail="Sem incidentes críticos" onClick={() => navigate("/analytics/health")} />
            <KPIWidget label="Módulos habilitados" value="6" detail="1 dependência pendente" onClick={() => navigate("/products/maestro-beton/modules")} />
            <KPIWidget label="Conteúdos publicados" value="42" detail="+3 na semana" onClick={() => navigate("/content/list")} />
            <KPIWidget label="Em revisão" value="7" detail="2 acima do SLA" onClick={() => navigate("/content/workflow")} />
            <KPIWidget label="Formulários" value="89" detail="12 não lidos" onClick={() => navigate("/forms/submissions")} />
            <KPIWidget label="Conversão estimada" value="5.2%" detail="+0.8 p.p." onClick={() => navigate("/analytics")} />
            <KPIWidget label="Assets recentes" value="18" detail="Galeria atualizada" onClick={() => navigate("/assets")} />
            <KPIWidget label="Erro parcial" value="—" detail="" error onClick={() => navigate("/analytics/states")} />
          </div>
          <QuickActions viewAsRole={viewAsRole} />
          <ModuleCatalog compact />
        </div>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Atividade recente</h2>
          <OperationalTimeline />
          <h2 className="mb-3 mt-5 text-lg font-semibold">Pendências</h2>
          {pendencias.map(([label, path]) => (
            <button key={label} onClick={() => navigate(path)} className="mb-2 flex w-full items-center justify-between rounded-xl border border-border p-3 text-left text-sm transition hover:border-primary/30 hover:bg-muted">
              <span>{label}</span>
              <ChevronRight size={14} className="shrink-0 text-muted-foreground" />
            </button>
          ))}
        </Card>
      </div>
    </>
  );
}

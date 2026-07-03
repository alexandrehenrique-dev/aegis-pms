import { useNavigate } from "react-router";
import { Plus } from "lucide-react";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { ConversionCard } from "../components/ConversionCard";
import { FormsTimeline } from "../components/FormsTimeline";

export function FormsDashboard() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const canCreate = viewAsRole !== "viewer";
  return (
    <>
      <PageHeader title="Forms" module="Forms" desc="Central operacional de captura de dados, submissões e qualificação de leads." badge="Maestro Beton">
        <Button onClick={() => navigate("/forms/list")}>Ver formulários</Button>
        <PermGate allowed={canCreate}><Button primary onClick={() => navigate("/forms/new")}><Plus size={15} />Criar formulário</Button></PermGate>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <KPIWidget label="Formulários ativos" value="4" detail="2 com baixa atividade" />
            <KPIWidget label="Respostas recebidas" value="439" detail="+18 nas últimas 24h" />
            <KPIWidget label="Conversão média" value="7.4%" detail="+1.2 p.p. na semana" />
            <KPIWidget label="Respostas pendentes" value="16" detail="8 aguardam responsável" />
            <KPIWidget label="Leads qualificados" value="74" detail="23 convertidos" />
            <KPIWidget label="Formulários sem atividade" value="2" detail="revisar publicação" error />
          </div>
          <div className="grid gap-4 lg:grid-cols-2">
            <ConversionCard />
            <Card>
              <h2 className="mb-3 text-lg font-semibold">Estados previstos</h2>
              <div className="grid gap-3">
                <EmptyState compact title="Sem formulários" description="Crie o primeiro fluxo de captura do produto." />
                <SkeletonLines />
                <PermissionHint />
              </div>
            </Card>
          </div>
        </div>
        <Card><h2 className="mb-3 text-lg font-semibold">Timeline Forms</h2><FormsTimeline /><PartialErrorWidget /></Card>
      </div>
    </>
  );
}

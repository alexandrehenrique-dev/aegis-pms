import { useNavigate } from "react-router";
import { Plus } from "lucide-react";
import { useViewAsRole } from "../../../core/permissions/ViewAsRoleContext";
import { PermGate } from "../../../app/guards/PermGate";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PartialErrorWidget, PermissionHint } from "../../../shared/components/Primitives";
import { OperationalTimeline } from "../../../shared/components/OperationalTimeline";

export function DashboardGlobal() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const canCreate = !["editor", "viewer"].includes(viewAsRole);
  const canSeeUsers = ["super_admin", "tenant_admin"].includes(viewAsRole);
  const canSeeFinancial = viewAsRole === "super_admin";

  return (
    <>
      <PageHeader title="Dashboard Global" desc="Visão operacional dos produtos digitais deste tenant." badge="Tenant BYOP">
        <Button>Últimos 30 dias</Button>
        <PermGate allowed={canCreate}><Button primary onClick={() => navigate("/products/new")}><Plus size={15} />Criar produto</Button></PermGate>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KPIWidget label="Produtos ativos" value="5" detail="1 produto arquivado" onClick={() => navigate("/products")} />
            <KPIWidget label="Conteúdos pendentes" value="18" detail="7 exigem revisão" onClick={() => navigate("/content/list")} />
            <KPIWidget label="Aprovações em aberto" value="6" detail="2 críticas" onClick={() => navigate("/content/workflow")} />
            <KPIWidget label="Formulários recebidos" value="143" detail="+12 hoje" onClick={() => navigate("/forms/submissions")} />
            <KPIWidget label="Assets recentes" value="32" detail="Atualizados na semana" onClick={() => navigate("/assets")} />
            <KPIWidget label="Usuários ativos" value="21" detail="9 Product Managers" locked={!canSeeUsers} onClick={canSeeUsers ? () => navigate("/users") : undefined} />
            <KPIWidget label="Conversão" value="4.8%" detail="Estimativa agregada" error onClick={() => navigate("/analytics")} />
            <KPIWidget label="Financeiro" value="—" detail="" locked={!canSeeFinancial} />
          </div>
          <div className="grid gap-4 lg:grid-cols-2">
            <Card><h2 className="mb-3 text-lg font-semibold">Alertas operacionais</h2><PartialErrorWidget /><div className="mt-3"><PermissionHint /></div></Card>
            <Card><h2 className="mb-3 text-lg font-semibold">Estado vazio previsto</h2><EmptyState compact title="Tenant sem produtos" description="Quando não houver produtos, a tela conduz para criação sem parecer vazia." /></Card>
          </div>
        </div>
        <Card><h2 className="mb-1 text-lg font-semibold">Atividade recente</h2><p className="mb-4 text-sm text-muted-foreground">Timeline operacional do tenant.</p><OperationalTimeline /></Card>
      </div>
    </>
  );
}

import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";
import { useAuth } from "../../../core/auth/useAuth";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { canCreateProduct, getPostLoginLandingPath } from "../../../core/permissions/roles";
import { PermGate } from "../../../app/guards/PermGate";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { OperationalTimeline } from "../../../shared/components/OperationalTimeline";
import { CreateProductModal } from "../../products/components/CreateProductModal";
import { dashboardService } from "../services/dashboardService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { ProductOption } from "../../../shared/types";
import type { ProductSummary } from "../../products/contracts/responses";

const PERIODS = ["Últimos 7 dias", "Últimos 30 dias", "Últimos 90 dias"];

export function DashboardGlobal() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const { authUser, effectiveTenant, tenantProducts, selectProduct } = useAuth();
  const canCreate = canCreateProduct(viewAsRole);
  const [showCreateProduct, setShowCreateProduct] = useState(false);
  const canSeeUsers = ["super_admin", "tenant_admin"].includes(viewAsRole);
  const canSeeFinancial = viewAsRole === "super_admin";
  const { data: summary, loading, error } = useAsyncData(() => dashboardService.getSummary(), []);
  const [period, setPeriod] = useState(PERIODS[1]);
  // Contagem real (não mock) — reflete na hora criação/edição/exclusão de produto
  // feitas em ProductSelectScreen/ProductsList, ao contrário do resto do summary.
  const activeProducts = tenantProducts.filter((p) => p.status === "Ativo").length;
  const archivedProducts = tenantProducts.filter((p) => p.status === "Arquivado").length;

  // Tarefa C.2 — `/products/new` não corresponde a nenhuma tela de criação
  // real (só existe o modal); antes o usuário ficava preso na tela anterior.
  // Mesmo padrão de `ProductSelectScreen.tsx`: abre o modal e, no sucesso,
  // navega para o produto recém-criado.
  const handleProductCreated = (created: ProductSummary) => {
    setShowCreateProduct(false);
    if (!authUser) return;
    const product: ProductOption = { id: created.id ?? created.name, name: created.name, type: created.type, status: created.status, modules: created.modules };
    selectProduct(product);
    navigate(getPostLoginLandingPath(authUser.role));
  };

  return (
    <>
      <AnimatePresence>
        {showCreateProduct && effectiveTenant && (
          <CreateProductModal tenantId={effectiveTenant.id} tenantName={effectiveTenant.name} onClose={() => setShowCreateProduct(false)} onCreated={handleProductCreated} />
        )}
      </AnimatePresence>
      <PageHeader data-tour="dashboard-global" title="Dashboard Global" desc="Visão operacional dos produtos digitais deste tenant." badge="Tenant BYOP">
        <Popover>
          <PopoverTrigger asChild><Button>{period}</Button></PopoverTrigger>
          <PopoverContent>
            <div className="flex flex-col gap-1">{PERIODS.map((p) => <Button key={p} onClick={() => setPeriod(p)} primary={period === p}>{p}</Button>)}</div>
          </PopoverContent>
        </Popover>
        <PermGate allowed={canCreate}><Button data-tour="dashboard-criar-produto" primary onClick={() => setShowCreateProduct(true)}><Plus size={15} />Criar produto</Button></PermGate>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          {loading ? (
            <SkeletonLines />
          ) : error || !summary ? (
            <PartialErrorWidget />
          ) : (
            <div data-tour="dashboard-kpis" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <PermGate allowed={canCreate}>
                <KPIWidget label="Produtos ativos" value={String(activeProducts)} detail={`${archivedProducts} produto${archivedProducts === 1 ? "" : "s"} arquivado${archivedProducts === 1 ? "" : "s"}`} onClick={() => navigate("/products")} />
              </PermGate>
              <KPIWidget label="Conteúdos pendentes" value={String(summary.pendingContent)} detail={`${summary.pendingContentNeedingReview} exigem revisão`} onClick={() => navigate("/content/list")} />
              <KPIWidget label="Aprovações em aberto" value={String(summary.openApprovals)} detail={`${summary.criticalApprovals} críticas`} onClick={() => navigate("/content/workflow")} />
              <KPIWidget label="Formulários recebidos" value={String(summary.formsReceived)} detail={`+${summary.formsReceivedToday} hoje`} onClick={() => navigate("/forms/submissions")} />
              <KPIWidget label="Assets recentes" value={String(summary.recentAssets)} detail="Atualizados na semana" onClick={() => navigate("/assets")} />
              <KPIWidget label="Usuários ativos" value={String(summary.activeUsers)} detail={`${summary.productManagers} Product Managers`} locked={!canSeeUsers} onClick={canSeeUsers ? () => navigate("/users") : undefined} />
              <KPIWidget label="Conversão" value={summary.conversionRate} detail="Estimativa agregada" error onClick={() => navigate("/analytics")} />
              <KPIWidget label="Financeiro" value="—" detail="" locked={!canSeeFinancial} />
            </div>
          )}
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

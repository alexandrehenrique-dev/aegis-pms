import { useNavigate, useSearchParams } from "react-router";
import { ChevronRight, ExternalLink } from "lucide-react";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { isRouteBlocked } from "../../../core/permissions/roles";
import { Button, Card, KPIWidget, PageHeader } from "../../../shared/components/Primitives";
import { OperationalTimeline } from "../../../shared/components/OperationalTimeline";
import { QuickActions } from "../components/QuickActions";
import { ModuleCatalog } from "../components/ModuleCatalog";
import { ProductEmpty } from "./ProductEmpty";
import { useAuth } from "../../../core/auth/useAuth";

export function ProductDashboard() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const { viewAsRole } = useViewAsRole();
  const { effectiveProduct } = useAuth();
  if (params.get("empty")) return <ProductEmpty />;

  const allPendencias: [string, string][] = [
    ["Revisar conteúdo em aprovação", "/content/workflow"],
    ["Configurar SEO da página Home", "/settings/product"],
    ["Adicionar imagens à galeria", "/assets"],
    ["Ver respostas do formulário de orçamento", "/forms/submissions"],
  ];
  // Sprint 13, Tarefa N: pendência só aparece se a rota não for bloqueada para o papel atual (ex.: "Configurar SEO" exige /settings, fora do escopo de editor/viewer).
  const pendencias = allPendencias.filter(([, path]) => !isRouteBlocked(viewAsRole, path));

  const productSlug = effectiveProduct ? effectiveProduct.name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "") : "";
  const productName = effectiveProduct?.name ?? "Produto";
  const productType = effectiveProduct?.type ?? "";
  const moduleCount = String(effectiveProduct?.modules ?? "—");

  return (
    <>
      <PageHeader title={productName} desc="Cockpit operacional do produto digital: saúde, pendências, módulos e próximos passos." badge={productType}>
        <Button onClick={() => navigate(`/products/${productSlug}/detail`)}>Editar produto</Button>
        <Button onClick={() => navigate(`/products/${productSlug}/modules`)}>Ver módulos</Button>
        <Button primary onClick={() => window.open(`https://${productSlug}.byop.app`, "_blank", "noopener,noreferrer")}><ExternalLink size={15} />Preview público</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KPIWidget label="Status do produto" value={effectiveProduct?.status ?? "—"} detail="" onClick={() => navigate("/analytics/health")} />
            <KPIWidget label="Módulos habilitados" value={moduleCount} detail="" onClick={() => navigate(`/products/${productSlug}/modules`)} />
            <KPIWidget label="Conteúdos publicados" value="—" detail="" onClick={() => navigate("/content/list")} />
            <KPIWidget label="Em revisão" value="—" detail="" onClick={() => navigate("/content/workflow")} />
            <KPIWidget label="Formulários" value="—" detail="" onClick={() => navigate("/forms/submissions")} />
            <KPIWidget label="Conversão estimada" value="—" detail="" onClick={() => navigate("/analytics")} />
            <KPIWidget label="Assets recentes" value="—" detail="" onClick={() => navigate("/assets")} />
            <KPIWidget label="Tipo" value={productType || "—"} detail="" onClick={() => navigate(`/products/${productSlug}/detail`)} />
          </div>
          <QuickActions viewAsRole={viewAsRole} />
          <ModuleCatalog compact productId={effectiveProduct?.id} />
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

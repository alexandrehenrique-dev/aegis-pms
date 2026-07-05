import { useNavigate } from "react-router";
import { Plus } from "lucide-react";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { useAuth } from "../../../core/auth/useAuth";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { ConversionCard } from "../components/ConversionCard";
import { FormsTimeline } from "../components/FormsTimeline";
import { formsService } from "../services/formsService";
import type { FormSummary } from "../contracts/responses";

function parseMetricNumber(value: string): number {
  const parsed = Number.parseInt(value.replace(/\D/g, ""), 10);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function parseConversion(value: string): number | null {
  const normalized = value.replace("%", "").replace(",", ".").trim();
  const parsed = Number.parseFloat(normalized);
  return Number.isNaN(parsed) ? null : parsed;
}

function isPublished(form: FormSummary): boolean {
  return form.publication.toLowerCase() === "publicado";
}

function isActive(form: FormSummary): boolean {
  const status = form.status.toLowerCase();
  return status === "ativo" || isPublished(form);
}

function isWithoutActivity(form: FormSummary): boolean {
  const status = form.status.toLowerCase();
  const lastActivity = form.lastActivity.toLowerCase();
  return parseMetricNumber(form.responses) === 0 || status.includes("sem respostas") || lastActivity.includes("sem atividade") || lastActivity.includes("sem respostas");
}

export function FormsDashboard() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const { effectiveProduct } = useAuth();
  const productId = effectiveProduct?.id;
  const { data: formsData, loading, error } = useAsyncData(
    () => (productId ? formsService.listForms(productId) : Promise.resolve([])),
    [productId],
  );
  const currentForms = formsData ?? [];
  const activeForms = currentForms.filter(isActive);
  const publishedForms = currentForms.filter(isPublished);
  const draftForms = currentForms.filter((form) => form.status.toLowerCase() === "rascunho");
  const inactiveForms = currentForms.filter(isWithoutActivity);
  const totalResponses = currentForms.reduce((total, form) => total + parseMetricNumber(form.responses), 0);
  const conversionValues = currentForms.map((form) => parseConversion(form.conversion)).filter((value): value is number => value !== null);
  const averageConversion = conversionValues.length === 0
    ? "0%"
    : `${(conversionValues.reduce((total, value) => total + value, 0) / conversionValues.length).toFixed(1)}%`;
  const canCreate = viewAsRole !== "viewer";
  return (
    <>
      <PageHeader title="Forms" module="Forms" desc="Central operacional de captura de dados, submissões e qualificação de leads." badge={effectiveProduct?.name}>
        <Button onClick={() => navigate("/forms/list")}>Ver formulários</Button>
        <PermGate allowed={canCreate}><Button primary onClick={() => navigate("/forms/new")}><Plus size={15} />Criar formulário</Button></PermGate>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {loading ? (
              <SkeletonLines />
            ) : (
              <>
                <KPIWidget label="Formulários ativos" value={String(activeForms.length)} detail={`${publishedForms.length} publicados`} />
                <KPIWidget label="Respostas recebidas" value={String(totalResponses)} detail={`${currentForms.length} formulários no produto`} />
                <KPIWidget label="Conversão média" value={averageConversion} detail={`${conversionValues.length} formulários com métrica`} />
                <KPIWidget label="Rascunhos" value={String(draftForms.length)} detail="pendentes de publicação" />
                <KPIWidget label="Formulários publicados" value={String(publishedForms.length)} detail={`${activeForms.length} ativos ou publicados`} />
                <KPIWidget label="Formulários sem atividade" value={String(inactiveForms.length)} detail="revisar publicação" error={Boolean(error)} />
              </>
            )}
          </div>
          <div className="grid gap-4 lg:grid-cols-2">
            <ConversionCard value={averageConversion} />
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
        <Card><h2 className="mb-3 text-lg font-semibold">Timeline Forms</h2><FormsTimeline /></Card>
      </div>
    </>
  );
}

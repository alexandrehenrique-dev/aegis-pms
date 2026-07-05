import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { AlertTriangle, Plus } from "lucide-react";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { contentService } from "../services/contentService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { NewContentModal } from "../components/NewContentModal";

function EditorialAttentionCard() {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Atenção editorial</h2>
      {["Página Home possui SEO incompleto.", "Sobre o Maestro está aguardando revisão.", "Galeria possui imagens sem texto alternativo.", "Contato foi publicado há 12 dias."].map((x) => (
        <div key={x} className="mb-2 rounded-xl border border-border bg-muted/30 p-3 text-sm"><AlertTriangle size={15} className="mb-1 text-[#8A5A12]" />{x}</div>
      ))}
      <PartialErrorWidget />
    </Card>
  );
}

function EditorialTimeline() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const { data: editEvents } = useAsyncData(() => contentService.listEditEvents(productId), [productId]);
  return (
    <div className="space-y-1">
      {(editEvents ?? []).map((t, i) => (
        <div key={t} className="flex gap-3 rounded-xl p-3 hover:bg-muted">
          <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
          <div><p className="text-sm font-medium">{t}</p><p className="text-xs text-muted-foreground">há {i + 1} h · {product?.name ?? "Produto"} → Conteúdo</p></div>
        </div>
      ))}
    </div>
  );
}

export function EditorialDashboard() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const { data: contentItems, loading, error } = useAsyncData(
    () => (productId ? contentService.listContent(productId) : Promise.resolve([])),
    [productId],
  );
  const canEdit = viewAsRole !== "viewer";
  const [showNewContent, setShowNewContent] = useState(false);
  const rows = contentItems ?? [];
  const drafts = rows.filter((item) => item.status === "Draft").length;
  const inReview = rows.filter((item) => item.status === "In Review").length;
  const published = rows.filter((item) => item.status === "Published").length;
  const archived = rows.filter((item) => item.status === "Archived").length;
  const translations = rows.filter((item) => item.lang !== "PT-BR").length;
  const recentlyUpdated = rows.filter((item) => item.updatedAt && item.updatedAt !== "—").length;
  const approvals = inReview;

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  return (
    <>
      <AnimatePresence>{showNewContent && <NewContentModal onClose={() => setShowNewContent(false)} />}</AnimatePresence>
      <PageHeader title="Conteúdo" module="Conteúdo" desc="Gerencie artigos, traduções, revisões e publicações deste produto." badge={product?.name}>
        <PermGate allowed={canEdit}><Button onClick={() => navigate("/content/workflow")}>Ver workflow</Button></PermGate>
        <PermGate allowed={canEdit}><Button data-tour="content-novo" primary onClick={() => setShowNewContent(true)}><Plus size={15} />Novo conteúdo</Button></PermGate>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KPIWidget label="Rascunhos" value={String(drafts)} detail="conteúdos em draft" />
            <KPIWidget label="Em revisão" value={String(inReview)} detail="aguardando aprovação" />
            <KPIWidget label="Publicados" value={String(published)} detail="visíveis para consumo" />
            <KPIWidget label="Arquivados" value={String(archived)} detail="histórico preservado" />
            <KPIWidget label="Traduções pendentes" value={String(translations)} detail="idiomas não PT-BR" />
            <KPIWidget label="Atualizados recentemente" value={String(recentlyUpdated)} detail="itens com atualização" />
            <KPIWidget label="Aprovações pendentes" value={String(approvals)} detail="Product Manager" />
            <KPIWidget label="Widget restrito" value="—" detail="" locked />
          </div>
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Estados globais do módulo</h2>
            <div className="grid gap-3 md:grid-cols-3">
              <EmptyState compact title="Sem conteúdo" description="Crie o primeiro item editorial do produto." />
              <div><p className="mb-2 text-sm font-medium">Loading</p><SkeletonLines /></div>
              <PermissionHint />
            </div>
          </Card>
        </div>
        <div className="space-y-4">
          <EditorialAttentionCard />
          <Card><h2 className="mb-3 text-lg font-semibold">Atividade editorial recente</h2><EditorialTimeline /></Card>
        </div>
      </div>
    </>
  );
}

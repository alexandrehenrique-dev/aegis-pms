import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { AlertTriangle, Plus } from "lucide-react";
import { Button, Card, EmptyState, KPIWidget, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/ViewAsRoleContext";
import { contentService } from "../services/contentService";
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
  const { data: editEvents } = useAsyncData(() => contentService.listEditEvents(), []);
  return (
    <div className="space-y-1">
      {(editEvents ?? []).map((t, i) => (
        <div key={t} className="flex gap-3 rounded-xl p-3 hover:bg-muted">
          <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
          <div><p className="text-sm font-medium">{t}</p><p className="text-xs text-muted-foreground">há {i + 1} h · BYOP → Maestro Beton → Conteúdo</p></div>
        </div>
      ))}
    </div>
  );
}

export function EditorialDashboard() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const canEdit = viewAsRole !== "viewer";
  const [showNewContent, setShowNewContent] = useState(false);
  return (
    <>
      <AnimatePresence>{showNewContent && <NewContentModal onClose={() => setShowNewContent(false)} />}</AnimatePresence>
      <PageHeader title="Conteúdo" module="Conteúdo" desc="Gerencie artigos, traduções, revisões e publicações deste produto." badge="Maestro Beton">
        <PermGate allowed={canEdit}><Button onClick={() => navigate("/content/workflow")}>Ver workflow</Button></PermGate>
        <PermGate allowed={canEdit}><Button primary onClick={() => setShowNewContent(true)}><Plus size={15} />Novo conteúdo</Button></PermGate>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KPIWidget label="Rascunhos" value="8" detail="3 atualizados hoje" />
            <KPIWidget label="Em revisão" value="5" detail="2 acima do SLA" />
            <KPIWidget label="Publicados" value="42" detail="+4 nesta semana" />
            <KPIWidget label="Arquivados" value="7" detail="Histórico preservado" />
            <KPIWidget label="Traduções pendentes" value="11" detail="EN-US e ES-ES" />
            <KPIWidget label="Atualizados recentemente" value="14" detail="últimas 48h" />
            <KPIWidget label="Aprovações pendentes" value="4" detail="Product Manager" />
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

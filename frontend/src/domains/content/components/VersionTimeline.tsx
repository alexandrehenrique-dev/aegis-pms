import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Lock } from "lucide-react";
import { Badge, Button, Card, EmptyState, PageHeader, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import type { ContentVersionRow } from "../contracts/responses";

export function VersionTimeline({ compact = false }: { compact?: boolean }) {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const contentId = id ?? "home";
  const [revision, setRevision] = useState(0);
  const { data: versions, loading, error } = useAsyncData(() => contentService.listVersions(contentId, productId), [contentId, productId, revision]);
  const [confirmRestore, setConfirmRestore] = useState<ContentVersionRow | null>(null);
  const [restoring, setRestoring] = useState(false);

  const handleRestore = async () => {
    if (!confirmRestore) return;
    setRestoring(true);
    try {
      await contentService.restoreVersion(confirmRestore.id, contentId, productId);
      toast.success("Versão restaurada!", { description: `${confirmRestore.versionLabel} agora é a versão atual.` });
      setConfirmRestore(null);
      setRevision((value) => value + 1);
    } finally {
      setRestoring(false);
    }
  };

  const body = (
    <div className="space-y-2">
      <AnimatePresence>
        {confirmRestore && <ConfirmDialog title="Restaurar esta versão?" desc={`${confirmRestore.versionLabel} se tornará a versão atual. A versão vigente será preservada no histórico.`} danger loading={restoring} onConfirm={handleRestore} onCancel={() => setConfirmRestore(null)} />}
      </AnimatePresence>
      {loading && <SkeletonLines />}
      {error && <PartialErrorWidget />}
      {!loading && !error && versions?.length === 0 && <EmptyState compact title="Sem histórico" description="Este conteúdo ainda não possui versões gravadas." />}
      {!loading && !error && versions?.map((version, i) => (
        <div key={version.id} className="rounded-xl border border-border p-3 text-sm">
          <div className="flex justify-between"><b>{version.versionLabel}</b><Badge tone={i === 0 ? "green" : "neutral"}>{i === 0 ? "Atual" : "Antiga"}</Badge></div>
          <p className="text-muted-foreground">{version.createdByName} · {version.createdAt} · snapshot preservado</p>
          <div className="mt-2 flex gap-2">
            <Button onClick={() => navigate(`/content/${contentId}/versions`)}>Visualizar</Button>
            <Button onClick={() => navigate(`/content/${contentId}/compare`)}>Comparar</Button>
            {i > 0 && <Button onClick={() => setConfirmRestore(version)}>Restaurar</Button>}
          </div>
        </div>
      ))}
    </div>
  );
  if (compact) return body;
  return (
    <>
      <PageHeader title="Histórico de Versões" module="Conteúdo" desc="Histórico estilo Git simplificado com autor, data, comentário e ações." badge="Versões">
        {/* fixo por enquanto: usuário atual não tem papel com permissão de restaurar versões publicadas — ADR/Sprint 09 */}
        <Button disabled><Lock size={14} />Sem permissão para restaurar</Button>
      </PageHeader>
      <Card>{body}</Card>
    </>
  );
}

import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { AnimatePresence } from "motion/react";
import { Lock } from "lucide-react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

export function VersionTimeline({ compact = false }: { compact?: boolean }) {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "p1";
  const [confirmRestore, setConfirmRestore] = useState<string | null>(null);
  const [restoring, setRestoring] = useState(false);

  const handleRestore = async () => {
    if (!confirmRestore) return;
    setRestoring(true);
    try {
      await contentService.restoreVersion(confirmRestore, id, productId);
      toast.success("Versão restaurada!", { description: `${confirmRestore} agora é a versão atual.` });
      setConfirmRestore(null);
    } finally {
      setRestoring(false);
    }
  };

  const versions = ["v18 Home hero ajustado", "v17 SEO description alterada", "v16 CTA secundário atualizado", "v15 Imagem hero alterada"];

  const body = (
    <div className="space-y-2">
      <AnimatePresence>
        {confirmRestore && <ConfirmDialog title="Restaurar esta versão?" desc={`${confirmRestore} se tornará a versão atual. A versão vigente será preservada no histórico.`} danger loading={restoring} onConfirm={handleRestore} onCancel={() => setConfirmRestore(null)} />}
      </AnimatePresence>
      {versions.map((v, i) => (
        <div key={v} className="rounded-xl border border-border p-3 text-sm">
          <div className="flex justify-between"><b>{v}</b><Badge tone={i === 0 ? "green" : "neutral"}>{i === 0 ? "Atual" : "Antiga"}</Badge></div>
          <p className="text-muted-foreground">Marina Costa · há {i + 1} dias · diferença resumida disponível</p>
          <div className="mt-2 flex gap-2">
            <Button onClick={() => navigate("/content/home/versions")}>Visualizar</Button>
            <Button onClick={() => navigate("/content/home/compare")}>Comparar</Button>
            {i > 0 && <Button onClick={() => setConfirmRestore(v)}>Restaurar</Button>}
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

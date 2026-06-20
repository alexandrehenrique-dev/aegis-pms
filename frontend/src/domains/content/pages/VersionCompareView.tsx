import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";

function DiffList({ old = false }: { old?: boolean }) {
  return (
    <div className="space-y-2 text-sm">
      {["título alterado", "CTA alterado", "imagem hero alterada", "SEO description alterada"].map((x) => (
        <div key={x} className={`rounded-lg p-3 ${old ? "bg-[#fee2e2] text-[#dc2626]" : "bg-[#ede9fe] text-[#7c3aed]"}`}>{old ? "-" : "+"} {x}</div>
      ))}
    </div>
  );
}

export function VersionCompareView() {
  const navigate = useNavigate();
  const [restoring, setRestoring] = useState(false);

  const handleRestore = async () => {
    setRestoring(true);
    try {
      await contentService.restoreVersion("v17");
      toast.success("Versão v17 restaurada!");
    } finally {
      setRestoring(false);
    }
  };

  return (
    <>
      <PageHeader title="Comparação de Versões" module="Conteúdo" desc="Diff lado a lado entre versões do conteúdo Home." badge="Diff">
        <Button onClick={() => navigate(-1)}>Voltar</Button>
        <Button primary onClick={handleRestore} disabled={restoring}>{restoring && <Loader2 size={15} className="animate-spin" />}{restoring ? "Restaurando..." : "Restaurar versão"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-2">
        <Card><h2 className="mb-3 text-lg font-semibold">Versão A — v17</h2><DiffList old /></Card>
        <Card><h2 className="mb-3 text-lg font-semibold">Versão B — v18</h2><DiffList /></Card>
      </div>
      <Card className="mt-4">
        <h2 className="mb-3 text-lg font-semibold">Diff JSON simplificado</h2>
        <pre className="overflow-auto rounded-xl bg-muted p-4 font-mono text-xs">{`{"title":"alterado","cta":"alterado","heroImage":"alterada","seoDescription":"alterada"}`}</pre>
      </Card>
    </>
  );
}

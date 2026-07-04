import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { toast } from "../../../core/notifications/toast";
import { settingsService } from "../services/settingsService";
import { useAuth } from "../../../core/auth/useAuth";

const SUBJECTS = ["Editor", "Viewer", "Product Manager", "Tenant Admin"];
const PRODUCTS = ["Maestro Beton", "Conecta Talentos"];
const MODULES = ["Conteúdo", "Assets", "Forms", "Analytics"];

export function AccessPreviewPanel() {
  const { effectiveTenant } = useAuth();
  const [subject, setSubject] = useState(SUBJECTS[0]);
  const [product, setProduct] = useState(PRODUCTS[0]);
  const [module, setModule] = useState(MODULES[0]);
  const [pickOpen, setPickOpen] = useState(false);
  const [generating, setGenerating] = useState(false);

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      await settingsService.generateAccessPreview({ subject, product, module }, effectiveTenant?.id);
      toast.success("Preview gerado!", { description: `${subject} em ${product} · ${module}` });
    } finally {
      setGenerating(false);
    }
  };

  return (
    <>
      <Dialog open={pickOpen} onOpenChange={setPickOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Selecionar usuário ou role</DialogTitle></DialogHeader>
          <SelectLike label="Usuário ou role" value={subject} options={SUBJECTS} onChange={setSubject} />
          <DialogFooter><Button primary onClick={() => setPickOpen(false)}>Confirmar</Button></DialogFooter>
        </DialogContent>
      </Dialog>
      <PageHeader title="Access Preview" module="Permissions" desc="Veja exatamente o que um usuário ou papel consegue visualizar e executar." badge={`${subject} em ${product}`}>
        <Button onClick={() => setPickOpen(true)}>Selecionar usuário</Button>
        <Button primary onClick={handleGenerate} disabled={generating}>{generating && <Loader2 size={15} className="animate-spin" />}{generating ? "Gerando..." : "Gerar preview"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[360px_1fr]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Entrada</h2>
          <SelectLike label="Usuário ou role" value={subject} options={SUBJECTS} onChange={setSubject} />
          <SelectLike label="Produto" value={product} options={PRODUCTS} onChange={setProduct} />
          <SelectLike label="Módulo" value={module} options={MODULES} onChange={setModule} />
        </Card>
        <div className="grid gap-4 md:grid-cols-2">
          <Card><h2 className="mb-3 text-lg font-semibold">Pode</h2>{["criar conteúdo", "editar conteúdo", "enviar revisão", "usar assets"].map((x) => <div key={x} className="mb-2 rounded-lg bg-[#ede9fe] p-3 text-sm text-primary">{x}</div>)}</Card>
          <Card><h2 className="mb-3 text-lg font-semibold">Não pode</h2>{["publicar", "alterar permissões", "acessar auditoria completa", "editar tenant"].map((x) => <div key={x} className="mb-2 rounded-lg bg-muted p-3 text-sm text-muted-foreground">{x}</div>)}</Card>
        </div>
      </div>
    </>
  );
}

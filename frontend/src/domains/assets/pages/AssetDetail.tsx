import { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { AssetUsagePanel } from "../components/AssetUsagePanel";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

function AssetPreviewPanel() {
  return (
    <Card>
      <div className="aspect-video rounded-2xl border border-border bg-[linear-gradient(135deg,#EEF3F0,#FFFFFF)] p-6">
        <div className="flex justify-between"><Badge tone="green">imagem</Badge><Badge>zoom 100%</Badge></div>
        <h2 className="mt-20 max-w-md text-3xl font-semibold">hero-maestro-beton.jpg</h2>
        <p className="mt-2 text-sm text-muted-foreground">Preview grande do arquivo com controles de zoom e informações técnicas.</p>
      </div>
      <div className="mt-4 grid gap-2 md:grid-cols-3"><Badge>1920×1080</Badge><Badge>2.4 MB</Badge><Badge>checksum: ags_42f9</Badge></div>
    </Card>
  );
}

export function AssetDetail() {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const canEdit = viewAsRole !== "viewer";
  const assetName = "hero-maestro-beton.jpg";
  const replaceInputRef = useRef<HTMLInputElement>(null);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [archiving, setArchiving] = useState(false);

  const handleCopyReference = async () => {
    await navigator.clipboard.writeText(`/assets/${assetName}`);
    toast.success("Copiado");
  };

  const handleReplaceFile = () => replaceInputRef.current?.click();

  const handleFileReplaced = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    await assetsService.upload(e.target.files[0], productId);
    toast.success("Arquivo substituído!", { description: e.target.files[0].name });
    e.target.value = "";
  };

  const handleArchive = async () => {
    setArchiving(true);
    try {
      await assetsService.archiveAsset(productId, assetName);
      toast.success("Asset arquivado.", { description: assetName });
      setConfirmArchive(false);
    } finally {
      setArchiving(false);
    }
  };

  return (
    <>
      <input ref={replaceInputRef} type="file" hidden onChange={handleFileReplaced} />
      <AnimatePresence>
        {confirmArchive && <ConfirmDialog title="Arquivar este asset?" desc="O asset deixará de aparecer nas listagens ativas. Referências em uso podem precisar revisão." danger loading={archiving} onConfirm={handleArchive} onCancel={() => setConfirmArchive(false)} />}
      </AnimatePresence>
      <PageHeader title="hero-maestro-beton.jpg" module="Assets" desc="Preview, metadados, tags, uso no sistema e ações do asset." badge="Ativo">
        <PermGate allowed={canEdit}><Button onClick={() => navigate("/assets/hero-maestro-beton/metadata")}>Editar metadados</Button></PermGate>
        <Button onClick={handleCopyReference}>Copiar referência</Button>
        <a href={assetsService.getDownloadUrl(assetName)} target="_blank" rel="noreferrer">
          <Button primary>Baixar</Button>
        </a>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
        <div className="space-y-4"><AssetPreviewPanel /><AssetUsagePanel /></div>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Metadados</h2>
          {[["Tipo", "JPG"], ["Tamanho", "2.4 MB"], ["Dimensões", "1920×1080"], ["Status", "ativo"], ["Upload", "12 jun · Marina Costa"], ["URL futura", "/assets/hero-maestro-beton.jpg"], ["Alt text", "Maestro Beton em apresentação ao vivo"], ["Crédito", "BYOP Studio"]].map((x) => (
            <div key={x[0]} className="mb-2 flex justify-between rounded-lg bg-muted p-2 text-sm"><span>{x[0]}</span><b className="text-right">{x[1]}</b></div>
          ))}
          <div className="mt-3 flex flex-wrap gap-1">{["hero", "seo", "institucional"].map((t) => <Badge key={t} tone="blue">{t}</Badge>)}</div>
          <PermGate allowed={canEdit}>
            <div className="mt-4 space-y-2">
              <Button onClick={handleReplaceFile}>Substituir arquivo</Button>
              <Button onClick={() => setConfirmArchive(true)}>Arquivar</Button>
            </div>
          </PermGate>
        </Card>
      </div>
    </>
  );
}

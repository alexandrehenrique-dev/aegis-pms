import { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { Loader2, Plus } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { AssetMetadataFormCard } from "../components/AssetMetadataFormCard";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";

function UploadProgressItem({ name, p }: { name: string; p: number }) {
  return (
    <div className="rounded-xl border border-border p-3">
      <div className="flex justify-between text-sm"><b>{name}</b><span>{p}%</span></div>
      <div className="mt-2 h-2 rounded-full bg-muted"><div className="h-2 rounded-full bg-primary" style={{ width: `${p}%` }} /></div>
    </div>
  );
}

function FileValidationAlert() {
  return <div className="rounded-xl border border-[#D97706]/25 bg-[#FBF1DF] p-3 text-sm text-[#8A5A12]">Use textos alternativos para melhorar acessibilidade e SEO. Tipos permitidos: JPG, PNG, WEBP, SVG, MP4, MP3 e PDF.</div>;
}

function AssetUploadZone({ onSelect }: { onSelect: () => void }) {
  return (
    <div className="rounded-2xl border border-dashed border-border bg-muted/40 p-8 text-center">
      <Plus className="mx-auto mb-3 text-primary" />
      <h3 className="font-semibold">Arraste arquivos para cá ou selecione do seu dispositivo.</h3>
      <p className="mt-1 text-sm text-muted-foreground">Arquivos enviados ficam vinculados ao produto ativo.</p>
      <Button primary onClick={onSelect}>Selecionar arquivos</Button>
    </div>
  );
}

export function AssetUploadScreen() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState([
    { name: "hero-maestro-beton.jpg", p: 72 },
    { name: "release-institucional.pdf", p: 100 },
    { name: "video-depoimento.mov", p: 38 },
  ]);
  const [completing, setCompleting] = useState(false);

  const handleSelectFiles = () => fileInputRef.current?.click();

  const handleFilesSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files;
    if (!selected || selected.length === 0) return;
    setFiles((prev) => [...prev, ...Array.from(selected).map((f) => ({ name: f.name, p: 0 }))]);
    e.target.value = "";
  };

  const handleCompleteUpload = async () => {
    setCompleting(true);
    try {
      await assetsService.uploadFiles();
      toast.success("Upload concluído!", { description: `${files.length} arquivo(s) vinculado(s) ao produto.` });
      navigate("/assets");
    } finally {
      setCompleting(false);
    }
  };

  return (
    <>
      <input ref={fileInputRef} type="file" multiple hidden onChange={handleFilesSelected} />
      <PageHeader title="Upload de Asset" module="Assets" desc="Envie arquivos com validação, progresso e metadados iniciais." badge="Upload">
        <Button onClick={() => navigate(-1)}>Cancelar</Button>
        <Button primary onClick={handleCompleteUpload} disabled={completing}>{completing && <Loader2 size={15} className="animate-spin" />}{completing ? "Concluindo..." : "Concluir upload"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <AssetUploadZone onSelect={handleSelectFiles} />
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Arquivos selecionados</h2>
            {files.map((f) => <UploadProgressItem key={f.name} name={f.name} p={f.p} />)}
            <div className="mt-3"><FileValidationAlert /></div>
          </Card>
        </div>
        <AssetMetadataFormCard />
      </div>
    </>
  );
}

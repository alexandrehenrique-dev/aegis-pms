import { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { Loader2, Plus } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { AssetMetadataFormCard } from "../components/AssetMetadataFormCard";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

type UploadStatus = "ready" | "uploading" | "processing" | "done" | "error";
type UploadFileState = { id: string; fileKey: string; name: string; p: number; status: UploadStatus };

const UPLOAD_STATUS_LABEL: Record<UploadStatus, string> = {
  ready: "pronto para envio",
  uploading: "enviando",
  processing: "processando",
  done: "concluído",
  error: "erro",
};

function selectedFileKey(file: File) {
  return `${file.name}-${file.size}-${file.lastModified}`;
}

function UploadProgressItem({ active, file, onSelect }: { active: boolean; file: UploadFileState; onSelect: () => void }) {
  const label = file.status === "uploading" || file.status === "processing" || file.status === "done"
    ? `${UPLOAD_STATUS_LABEL[file.status]} · ${file.p}%`
    : UPLOAD_STATUS_LABEL[file.status];

  return (
    <button
      type="button"
      onClick={onSelect}
      className={`w-full rounded-xl border p-3 text-left transition ${active ? "border-primary bg-primary/5" : "border-border hover:bg-muted"}`}
    >
      <div className="flex justify-between gap-3 text-sm"><b className="min-w-0 truncate">{file.name}</b><span className="shrink-0">{label}</span></div>
      {file.status === "ready" ? (
        <p className="mt-2 text-xs text-muted-foreground">{active ? "Editando metadados deste arquivo." : "Clique para editar os metadados deste arquivo."}</p>
      ) : (
        <div className="mt-2 h-2 rounded-full bg-muted">
          <div className={`h-2 rounded-full ${file.status === "error" ? "bg-destructive" : "bg-primary"}`} style={{ width: `${file.p}%` }} />
        </div>
      )}
    </button>
  );
}

function FileValidationAlert() {
  return <div className="rounded-xl border border-[#D97706]/25 bg-[#FBF1DF] p-3 text-sm text-[#8A5A12]">Use textos alternativos para melhorar acessibilidade e SEO. Aceita qualquer tipo de arquivo (imagens, vídeo, áudio, PDF, DOCX, ZIP...).</div>;
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
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState<UploadFileState[]>([]);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [metadataFileKey, setMetadataFileKey] = useState<string | null>(null);
  const [completing, setCompleting] = useState(false);
  const canStartUpload = Boolean(productId) && files.length > 0 && files.every((file) => file.status === "ready" || file.status === "error");
  const activeMetadataFile = selectedFiles.find((file) => selectedFileKey(file) === metadataFileKey) ?? selectedFiles[0];

  const handleSelectFiles = () => fileInputRef.current?.click();

  const handleFilesSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files;
    if (!selected || selected.length === 0) return;
    const nextFiles = Array.from(selected);
    setMetadataFileKey((current) => current ?? selectedFileKey(nextFiles[0]));
    setSelectedFiles((prev) => [...prev, ...nextFiles]);
    setFiles((prev) => [
      ...prev,
      ...nextFiles.map((file) => {
        const fileKey = selectedFileKey(file);
        return { id: `${fileKey}-${crypto.randomUUID()}`, fileKey, name: file.name, p: 0, status: "ready" as const };
      }),
    ]);
    e.target.value = "";
  };

  const handleCompleteUpload = async () => {
    if (!productId) {
      toast.error("Produto ativo não encontrado.", { description: "Selecione um produto antes de enviar assets." });
      return;
    }
    setCompleting(true);
    setFiles((current) => current.map((file) => ({ ...file, p: 0, status: "ready" })));
    try {
      for (const selectedFile of selectedFiles) {
        const fileKey = selectedFileKey(selectedFile);
        let latestProgress = 1;
        setFiles((current) => current.map((file) => file.fileKey === fileKey ? { ...file, p: 1, status: "uploading" } : file));

        const progressTimer = window.setInterval(() => {
          setFiles((current) => current.map((file) => {
            if (file.fileKey !== fileKey || file.status !== "uploading") return file;
            latestProgress = Math.min(95, Math.max(latestProgress + 1, file.p + 1));
            return { ...file, p: latestProgress };
          }));
        }, 900);

        try {
          await assetsService.uploadFile(productId, selectedFile, (percent) => {
            latestProgress = Math.max(latestProgress, percent);
            setFiles((current) => current.map((file) => {
              if (file.fileKey !== fileKey) return file;
              const status: UploadStatus = percent >= 99 ? "processing" : "uploading";
              return { ...file, p: Math.max(file.p, percent), status };
            }));
          });
          setFiles((current) => current.map((file) => file.fileKey === fileKey ? { ...file, p: 100, status: "done" } : file));
        } catch (error) {
          setFiles((current) => current.map((file) => file.fileKey === fileKey ? { ...file, status: "error" } : file));
          throw error;
        } finally {
          window.clearInterval(progressTimer);
        }
      }
      toast.success("Upload concluído!", { description: `${selectedFiles.length} arquivo(s) vinculado(s) ao produto.` });
      navigate("/assets");
    } catch {
      toast.error("Upload não concluído.", { description: "Verifique o tipo/tamanho do arquivo e tente novamente." });
    } finally {
      setCompleting(false);
    }
  };

  return (
    <>
      <input ref={fileInputRef} type="file" multiple hidden onChange={handleFilesSelected} />
      <PageHeader title="Upload de Asset" module="Assets" desc="Envie arquivos com validação, progresso e metadados iniciais." badge="Upload">
        <Button onClick={() => navigate(-1)}>Cancelar</Button>
        <Button primary onClick={handleCompleteUpload} disabled={completing || !canStartUpload}>{completing && <Loader2 size={15} className="animate-spin" />}{completing ? "Enviando..." : "Enviar upload"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <AssetUploadZone onSelect={handleSelectFiles} />
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Arquivos selecionados</h2>
            {files.length === 0 ? <p className="text-sm text-muted-foreground">Nenhum arquivo selecionado.</p> : files.map((file) => {
              return (
                <UploadProgressItem
                  key={file.id}
                  active={file.fileKey === metadataFileKey}
                  file={file}
                  onSelect={() => setMetadataFileKey(file.fileKey)}
                />
              );
            })}
            <div className="mt-3"><FileValidationAlert /></div>
          </Card>
        </div>
        <AssetMetadataFormCard key={activeMetadataFile ? selectedFileKey(activeMetadataFile) : "no-file"} suggestedFileName={activeMetadataFile?.name} />
      </div>
    </>
  );
}

import { Plus } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { AssetMetadataFormCard } from "../components/AssetMetadataFormCard";

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

function AssetUploadZone() {
  return (
    <div className="rounded-2xl border border-dashed border-border bg-muted/40 p-8 text-center">
      <Plus className="mx-auto mb-3 text-primary" />
      <h3 className="font-semibold">Arraste arquivos para cá ou selecione do seu dispositivo.</h3>
      <p className="mt-1 text-sm text-muted-foreground">Arquivos enviados ficam vinculados ao produto ativo.</p>
      <Button primary>Selecionar arquivos</Button>
    </div>
  );
}

export function AssetUploadScreen() {
  return (
    <>
      <PageHeader title="Upload de Asset" module="Assets" desc="Envie arquivos com validação, progresso e metadados iniciais." badge="Upload">
        <Button>Cancelar</Button>
        <Button primary>Concluir upload</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <AssetUploadZone />
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Arquivos selecionados</h2>
            <UploadProgressItem name="hero-maestro-beton.jpg" p={72} />
            <UploadProgressItem name="release-institucional.pdf" p={100} />
            <UploadProgressItem name="video-depoimento.mov" p={38} />
            <div className="mt-3"><FileValidationAlert /></div>
          </Card>
        </div>
        <AssetMetadataFormCard />
      </div>
    </>
  );
}

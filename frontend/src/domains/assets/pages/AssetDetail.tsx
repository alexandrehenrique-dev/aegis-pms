import { useNavigate } from "react-router";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { AssetUsagePanel } from "../components/AssetUsagePanel";

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
  return (
    <>
      <PageHeader title="hero-maestro-beton.jpg" module="Assets" desc="Preview, metadados, tags, uso no sistema e ações do asset." badge="Ativo">
        <Button onClick={() => navigate("/assets/hero-maestro-beton/metadata")}>Editar metadados</Button>
        <Button>Copiar referência</Button>
        <Button primary>Baixar</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
        <div className="space-y-4"><AssetPreviewPanel /><AssetUsagePanel /></div>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Metadados</h2>
          {[["Tipo", "JPG"], ["Tamanho", "2.4 MB"], ["Dimensões", "1920×1080"], ["Status", "ativo"], ["Upload", "12 jun · Marina Costa"], ["URL futura", "/assets/hero-maestro-beton.jpg"], ["Alt text", "Maestro Beton em apresentação ao vivo"], ["Crédito", "BYOP Studio"]].map((x) => (
            <div key={x[0]} className="mb-2 flex justify-between rounded-lg bg-muted p-2 text-sm"><span>{x[0]}</span><b className="text-right">{x[1]}</b></div>
          ))}
          <div className="mt-3 flex flex-wrap gap-1">{["hero", "seo", "institucional"].map((t) => <Badge key={t} tone="blue">{t}</Badge>)}</div>
          <div className="mt-4 space-y-2"><Button>Substituir arquivo</Button><Button>Arquivar</Button></div>
        </Card>
      </div>
    </>
  );
}

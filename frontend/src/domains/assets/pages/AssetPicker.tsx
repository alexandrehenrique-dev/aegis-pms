import { useRef, useState } from "react";
import { Image, Search } from "lucide-react";
import { Badge, Button, Card, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { assetsService } from "../services/assetsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { AssetTypeIcon } from "../components/AssetBits";
import { toast } from "../../../core/notifications/toast";

export function AssetPicker() {
  const [selected, setSelected] = useState("hero-maestro-beton.jpg");
  const { data: assets, loading, error } = useAsyncData(() => assetsService.listAssets(), []);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleQuickUpload = () => fileInputRef.current?.click();

  const handleFilesSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    await assetsService.uploadFiles();
    toast.success(`${files.length} arquivo(s) enviado(s)!`);
    setSelected(files[0].name);
    e.target.value = "";
  };

  const handleConfirmSelection = () => {
    toast.success("Asset selecionado!", { description: selected });
  };

  return (
    <>
      <input ref={fileInputRef} type="file" multiple hidden onChange={handleFilesSelected} />
      <PageHeader title="Asset Picker" module="Assets" desc="Componente reutilizável para Editor, SEO, Forms e configurações futuras." badge="Picker">
        <Button onClick={handleQuickUpload}>Upload rápido</Button>
        <Button primary onClick={handleConfirmSelection}>Confirmar seleção</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <div className="mb-3 flex items-center gap-2 rounded-xl border border-border px-3 py-2"><Search size={16} /><input placeholder="Buscar asset existente..." className="w-full bg-transparent text-sm outline-none" /></div>
          <div className="mb-3 flex flex-wrap gap-2"><Badge>Busca</Badge><Badge>Filtros</Badge><Badge>Seleção única</Badge><Badge>Seleção múltipla</Badge><Badge>Sem resultados</Badge></div>
          {loading ? <SkeletonLines /> : error || !assets ? <PartialErrorWidget /> : (
            <div className="grid gap-3 md:grid-cols-2">
              {assets.slice(0, 6).map((a) => (
                <button key={a.name} onClick={() => setSelected(a.name)} className={`rounded-xl border p-3 text-left ${selected === a.name ? "border-primary bg-muted" : "border-border"}`}>
                  <AssetTypeIcon type={a.type} />
                  <p className="mt-2 font-medium">{a.name}</p>
                  <p className="text-sm text-muted-foreground">{a.type} · {a.size}</p>
                </button>
              ))}
            </div>
          )}
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Preview lateral</h2>
          <div className="aspect-video rounded-xl bg-muted p-4"><Image className="text-primary" /></div>
          <p className="mt-3 font-medium">{selected}</p>
          <p className="text-sm text-muted-foreground">Asset selecionado vinculado ao produto ativo.</p>
          <Button primary onClick={handleConfirmSelection}>Usar asset</Button>
        </Card>
      </div>
    </>
  );
}

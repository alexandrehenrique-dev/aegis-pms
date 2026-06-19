import { useState } from "react";
import { Image, Search } from "lucide-react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { assets } from "../mocks/assets.mocks";
import { AssetTypeIcon } from "../components/AssetBits";

export function AssetPicker() {
  const [selected, setSelected] = useState("hero-maestro-beton.jpg");
  return (
    <>
      <PageHeader title="Asset Picker" module="Assets" desc="Componente reutilizável para Editor, SEO, Forms e configurações futuras." badge="Picker">
        <Button>Upload rápido</Button>
        <Button primary>Confirmar seleção</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <div className="mb-3 flex items-center gap-2 rounded-xl border border-border px-3 py-2"><Search size={16} /><input placeholder="Buscar asset existente..." className="w-full bg-transparent text-sm outline-none" /></div>
          <div className="mb-3 flex flex-wrap gap-2"><Badge>Busca</Badge><Badge>Filtros</Badge><Badge>Seleção única</Badge><Badge>Seleção múltipla</Badge><Badge>Sem resultados</Badge></div>
          <div className="grid gap-3 md:grid-cols-2">
            {assets.slice(0, 6).map((a) => (
              <button key={a[0]} onClick={() => setSelected(a[0])} className={`rounded-xl border p-3 text-left ${selected === a[0] ? "border-primary bg-muted" : "border-border"}`}>
                <AssetTypeIcon type={a[1]} />
                <p className="mt-2 font-medium">{a[0]}</p>
                <p className="text-sm text-muted-foreground">{a[1]} · {a[2]}</p>
              </button>
            ))}
          </div>
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Preview lateral</h2>
          <div className="aspect-video rounded-xl bg-muted p-4"><Image className="text-primary" /></div>
          <p className="mt-3 font-medium">{selected}</p>
          <p className="text-sm text-muted-foreground">Asset selecionado vinculado ao produto ativo.</p>
          <Button primary>Usar asset</Button>
        </Card>
      </div>
    </>
  );
}

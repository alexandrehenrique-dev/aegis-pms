import { AlertTriangle } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { AssetMetadataFormCard } from "../components/AssetMetadataFormCard";

export function AssetMetadataForm() {
  return (
    <>
      <PageHeader title="Editar Metadados" module="Assets" desc="Atualize descrição, tags, visibilidade e uso SEO do asset." badge="Metadados">
        <Button>Cancelar</Button>
        <Button primary>Salvar</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div><AssetMetadataFormCard /></div>
        <Card><AlertTriangle className="mb-2 text-[#8A5A12]" /><p className="text-sm">Imagem usada em SEO deve possuir alt text e descrição verificável.</p></Card>
      </div>
    </>
  );
}

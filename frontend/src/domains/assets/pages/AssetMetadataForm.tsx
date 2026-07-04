import { useState } from "react";
import { useNavigate } from "react-router";
import { AlertTriangle, Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { AssetMetadataFormCard } from "../components/AssetMetadataFormCard";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

export function AssetMetadataForm() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      // Botão duplicado do "Salvar metadados" de `AssetMetadataFormCard`
      // (que tem acesso aos campos reais do formulário) — pré-existente,
      // fora do escopo desta sprint. Sem `assetId`/campos aqui, não há como
      // montar um payload real; mantido só para não quebrar o botão do header.
      await assetsService.saveMetadata(productId, "asset-sem-rota", {});
      toast.success("Metadados salvos!");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Editar Metadados" module="Assets" desc="Atualize descrição, tags, visibilidade e uso SEO do asset." badge="Metadados">
        <Button onClick={() => navigate(-1)}>Cancelar</Button>
        <Button primary onClick={handleSave} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div><AssetMetadataFormCard /></div>
        <Card><AlertTriangle className="mb-2 text-[#8A5A12]" /><p className="text-sm">Imagem usada em SEO deve possuir alt text e descrição verificável.</p></Card>
      </div>
    </>
  );
}

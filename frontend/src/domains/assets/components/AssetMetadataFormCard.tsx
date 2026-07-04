import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, SelectLike } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

const FOLDERS = ["Campanha institucional", "Campanha de evento", "Branding", "Sem grupo"];
const VISIBILITY = ["Público", "Interno", "Restrito"];
const SEO_USAGE = ["OG Image", "Hero", "Thumbnail", "Não aplicável"];

export function AssetMetadataFormCard() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [friendlyName, setFriendlyName] = useState("Hero Maestro Beton");
  const [altText, setAltText] = useState("Maestro Beton em apresentação ao vivo");
  const [caption, setCaption] = useState("Apresentação institucional");
  const [credit, setCredit] = useState("BYOP Studio");
  const [tags, setTags] = useState("hero, seo, institucional");
  const [folder, setFolder] = useState(FOLDERS[0]);
  const [visibility, setVisibility] = useState(VISIBILITY[0]);
  const [seoUsage, setSeoUsage] = useState(SEO_USAGE[0]);
  const [notes, setNotes] = useState("Usado na Home e preview público");
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      // Tela ainda não recebe o asset real por rota (mockup estático desde a
      // criação — Sprint de Integração 05 conecta a um `AssetSummary` real);
      // `friendlyName` é o único identificador disponível aqui hoje.
      await assetsService.saveMetadata(productId, friendlyName, { friendlyName, altText, caption, credit, tags, folder, visibility, seoUsage, notes });
      toast.success("Metadados salvos!");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Metadados do asset</h2>
      <div className="space-y-3">
        <Field label="Nome amigável" value={friendlyName} onChange={setFriendlyName} />
        <Field label="Alt text" value={altText} onChange={setAltText} />
        <p className="text-xs text-muted-foreground">{altText.length}/125 caracteres recomendados</p>
        <Field label="Legenda" value={caption} onChange={setCaption} />
        <Field label="Crédito" value={credit} onChange={setCredit} />
        <Field label="Tags" value={tags} onChange={setTags} />
        <SelectLike label="Pasta lógica / grupo" value={folder} options={FOLDERS} onChange={setFolder} />
        <SelectLike label="Visibilidade" value={visibility} options={VISIBILITY} onChange={setVisibility} />
        <SelectLike label="SEO usage" value={seoUsage} options={SEO_USAGE} onChange={setSeoUsage} />
        <Field label="Observações internas" value={notes} onChange={setNotes} textarea />
        <UnsavedChangesBanner />
        <Button primary onClick={handleSave} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar metadados"}</Button>
        <Button onClick={() => navigate(-1)}>Cancelar</Button>
      </div>
    </Card>
  );
}

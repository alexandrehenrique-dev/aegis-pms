import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, SelectLike } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

const FOLDERS = ["Sem grupo", "Campanha institucional", "Campanha de evento", "Branding"];
const VISIBILITY = ["Público", "Interno", "Restrito"];
const SEO_USAGE = ["Não aplicável", "OG Image", "Hero", "Thumbnail"];

function friendlyNameFromFile(fileName?: string): string {
  return fileName ? fileName.replace(/\.[a-z0-9]+$/i, "") : "";
}

export function AssetMetadataFormCard({ suggestedFileName }: { suggestedFileName?: string }) {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const [friendlyName, setFriendlyName] = useState("");
  const [altText, setAltText] = useState("");
  const [caption, setCaption] = useState("");
  const [credit, setCredit] = useState("");
  const [tags, setTags] = useState("");
  const [folder, setFolder] = useState(FOLDERS[0]);
  const [visibility, setVisibility] = useState(VISIBILITY[0]);
  const [seoUsage, setSeoUsage] = useState(SEO_USAGE[0]);
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setFriendlyName(friendlyNameFromFile(suggestedFileName));
    setAltText("");
    setCaption("");
    setCredit("");
    setTags("");
    setFolder(FOLDERS[0]);
    setVisibility(VISIBILITY[0]);
    setSeoUsage(SEO_USAGE[0]);
    setNotes("");
  }, [suggestedFileName]);

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
      <h2 className="text-lg font-semibold">Metadados do asset</h2>
      <p className="mb-3 mt-1 text-xs text-muted-foreground">
        {suggestedFileName ? `Editando ${suggestedFileName}` : "Selecione um arquivo para editar metadados."}
      </p>
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

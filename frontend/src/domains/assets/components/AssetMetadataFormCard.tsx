import { Button, Card, Field, SelectLike } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";

export function AssetMetadataFormCard() {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Metadados do asset</h2>
      <div className="space-y-3">
        <Field label="Nome amigável" value="Hero Maestro Beton" />
        <Field label="Alt text" value="Maestro Beton em apresentação ao vivo" />
        <p className="text-xs text-muted-foreground">42/125 caracteres recomendados</p>
        <Field label="Legenda" value="Apresentação institucional" />
        <Field label="Crédito" value="BYOP Studio" />
        <Field label="Tags" value="hero, seo, institucional" />
        <SelectLike label="Pasta lógica / grupo" value="Campanha institucional" />
        <SelectLike label="Visibilidade" value="Público" />
        <SelectLike label="SEO usage" value="OG Image" />
        <Field label="Observações internas" value="Usado na Home e preview público" textarea />
        <UnsavedChangesBanner />
        <Button primary>Salvar metadados</Button>
        <Button>Cancelar</Button>
      </div>
    </Card>
  );
}

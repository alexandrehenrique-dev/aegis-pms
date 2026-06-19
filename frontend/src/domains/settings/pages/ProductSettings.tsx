import { Button, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { SettingsSection } from "../components/SettingsBits";

export function ProductSettings() {
  return (
    <>
      <PageHeader title="Product Settings" module="Configurações" desc="Configurações do produto Maestro Beton com rastreabilidade." badge="Produto">
        <Button>Cancelar</Button>
        <Button primary>Salvar alterações</Button>
      </PageHeader>
      <UnsavedChangesBanner />
      <div className="grid gap-4">
        <SettingsSection title="Dados gerais" items={["Nome", "Slug", "Tipo", "Idioma padrão", "Descrição", "Status"]} />
        <SettingsSection title="Branding" items={["Logo", "Cor principal", "Favicon", "Assets de marca"]} />
        <SettingsSection title="SEO" items={["Title padrão", "Description padrão", "OG image", "Locale"]} />
        <SettingsSection title="Publicação e módulos" items={["Ambiente", "Preview público", "Domínio futuro", "Status publicação", "Módulos habilitados", "Dependências"]} />
      </div>
    </>
  );
}

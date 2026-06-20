import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { SettingsSection } from "../components/SettingsBits";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../../products/services/productsService";

export function ProductSettings() {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      await productsService.saveSettings();
      toast.success("Alterações salvas!");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Product Settings" module="Configurações" desc="Configurações do produto Maestro Beton com rastreabilidade." badge="Produto">
        <Button onClick={() => navigate(-1)}>Cancelar</Button>
        <Button primary onClick={handleSave} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar alterações"}</Button>
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

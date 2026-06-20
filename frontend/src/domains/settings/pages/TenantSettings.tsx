import { useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { SettingsSection } from "../components/SettingsBits";
import { toast } from "../../../core/notifications/toast";
import { tenantsService } from "../../../core/tenants/services/tenantsService";

export function TenantSettings() {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      await tenantsService.update("t1", { name: "BYOP", plan: "Pro", status: "ativo" });
      toast.success("Configurações do tenant salvas!");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Tenant Settings" module="Configurações" desc="Configurações do tenant BYOP: identidade, governança e limites futuros." badge="Tenant BYOP">
        <Button onClick={() => navigate(-1)}>Cancelar</Button>
        <Button primary onClick={handleSave} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar tenant"}</Button>
      </PageHeader>
      <UnsavedChangesBanner />
      <div className="grid gap-4">
        <SettingsSection title="Dados gerais" items={["Nome", "Slug", "Descrição", "Status", "Idioma padrão"]} />
        <SettingsSection title="Branding do tenant" items={["Logo", "Cor institucional", "Assinatura visual"]} />
        <SettingsSection title="Governança" items={["Política de convites", "Regras de acesso", "Retenção futura", "Auditoria obrigatória"]} />
        <SettingsSection title="Limites futuros" items={["Produtos", "Usuários", "Storage", "Formulários"]} />
      </div>
    </>
  );
}

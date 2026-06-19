import { Button, PageHeader } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { SettingsSection } from "../components/SettingsBits";

export function TenantSettings() {
  return (
    <>
      <PageHeader title="Tenant Settings" module="Configurações" desc="Configurações do tenant BYOP: identidade, governança e limites futuros." badge="Tenant BYOP">
        <Button>Cancelar</Button>
        <Button primary>Salvar tenant</Button>
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
